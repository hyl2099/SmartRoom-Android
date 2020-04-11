package com.upm.smartroom;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;


import com.google.android.gms.tasks.Task;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.things.contrib.driver.bmx280.Bmx280SensorDriver;
import com.upm.smartroom.alarm.AlarmState;
import com.upm.smartroom.board.BoardDefaults;
import com.upm.smartroom.board.BoardSpec;
import com.upm.smartroom.doorbell.DoorbellActivity;
import com.upm.smartroom.doorbell.DoorbellCamera;
import com.upm.smartroom.weather.Weather;
import com.upm.smartroom.weather.WeatherRESTAPIService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class ThingsMainActivity extends AppCompatActivity {
    private static final String TAG = DoorbellActivity.class.getSimpleName();
    private static final String LOG_TAG = DoorbellActivity.class.getSimpleName();


    //data from API
    private ImageView weatherImage;
    private TextView weatherTxt;
    private TextView currTemp;
    private TextView minTemp;
    private TextView maxTemp;
    private TextView humidity;
    private TextView wind;
    //data from sensor
//    private TextView bmp280Txt;
    private ImageView bmp280Image;
    private TextView temperatureDisplay;
    private TextView barometerDisplay;
    private TextView timeTxt;
    private TextView otherTxt;




    // btb Firebase database variables
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mAlarmDatabaseReference;
    private FirebaseStorage mStorage;
    private ChildEventListener mChildEventAlarmListener;


    private DoorbellCamera mCamera;

    //weather API
    private static final String WEATHER_API_BASE_URL = "https://api.openweathermap.org";
    private WeatherRESTAPIService apiService;

    //Driver for the doorbell button GPIO2_IO05;
    private ButtonInputDriver mButtonInputDriver;
    //Alarm button GPIO2_IO07
    private ButtonInputDriver mButtonAlarmInputDriver;
    private int alarmState;
    private static final int ALARMON = 1;
    private static final int ALARMOFF = 0;
    //LED GPIO06_IO14
    private Gpio led;
    //Red GPIO2_IO01//Green GPIO2_IO02//Blue GPIO2_IO00
    private List<Gpio> rgbLed;
    //BMP280 for temperature and humidity
    private SensorManager mSensorManager;
    private Bmx280SensorDriver mEnvironmentalSensorDriver;
    private float mLastTemperature;
    private float mLastPressure;
    private static final float BAROMETER_RANGE_LOW = 965.f;
    private static final float BAROMETER_RANGE_HIGH = 1035.f;
    private static final float BAROMETER_RANGE_SUNNY = 1010.f;
    private static final float BAROMETER_RANGE_RAINY = 990.f;
    private static final int MSG_UPDATE_BAROMETER_UI = 4;
    private static final int MSG_UPDATE_TEMPERATURE = 5;
    private static final int MSG_UPDATE_BAROMETER = 6;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.0");
    //speaker: active buzzer arduino
    private Speaker speaker;
    private Gpio buzzerSpeaker;
    Blink blinkBuzzerSpeaker;
    //detect something moving
    private int somethingIsMoving;
    private  static final int SOMETHING_MOVING = 1;
    private  static final int NOTHING_MOVING = 0;
    //electric switch

    //electronic lock


    /**
     * A {@link Handler} for running Camera tasks in the background.
     */
    private Handler mCameraHandler;

    /**
     * An additional thread for running Camera tasks that shouldn't block the UI.
     */
    private HandlerThread mCameraThread;

    /**
     * A {@link Handler} for running Cloud tasks in the background.
     */
    private Handler mCloudHandler;

    /**
     * An additional thread for running Cloud tasks that shouldn't block the UI.
     */
    private HandlerThread mCloudThread;


    //////定义我的handler
    //my handler
    public Handler mHandler;
    class Mhandler extends Handler {
        private int mBarometerImage = -1;
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    obtenerInfo(findViewById(R.id.weatherTxt));
                    break;
                case 2:
                    myTimer(findViewById(R.id.timeTxt));
                    break;
                case 3:
                    try {
                        startBuzzerAlarm();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case MSG_UPDATE_BAROMETER_UI:
                    int img;
                    if (mLastPressure > BAROMETER_RANGE_SUNNY) {
                        img = R.drawable.ic_sunny;
                    } else if (mLastPressure < BAROMETER_RANGE_RAINY) {
                        img = R.drawable.ic_rainy;
                    } else {
                        img = R.drawable.ic_cloudy;
                    }
                    if (img != mBarometerImage) {
                        bmp280Image.setImageResource(img);
                        mBarometerImage = img;
                    }
                    break;
                case MSG_UPDATE_TEMPERATURE:
                    temperatureDisplay.setText(DECIMAL_FORMAT.format(mLastTemperature));
                    break;
                case MSG_UPDATE_BAROMETER:
                    barometerDisplay.setText(DECIMAL_FORMAT.format(mLastPressure*0.1));
                    break;
            }
        }
    }
    //thread for get API data from internet
    class MyThread implements Runnable {
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                Message message = new Message();
                message.what = 1;
                mHandler.sendMessage(message);
                try {
                    Thread.sleep(1000*60*5);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    //thread for Show Current Time
    class MyTimerThread implements Runnable {
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                Message message = new Message();
                message.what = 2;
                mHandler.sendMessage(message);
                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    //thread for check if need alarm on, buzzer on.
    class MyBuzzerThread implements Runnable {
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                Message message = new Message();
                message.what = 3;
                mHandler.sendMessage(message);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Callback used when we register the BMP280 sensor driver with the system's SensorManager.
    private SensorManager.DynamicSensorCallback mDynamicSensorCallback
            = new SensorManager.DynamicSensorCallback() {
        @Override
        public void onDynamicSensorConnected(Sensor sensor) {
            if (sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
                // Our sensor is connected. Start receiving temperature data.
                mSensorManager.registerListener(mTemperatureListener, sensor,
                        SensorManager.SENSOR_DELAY_NORMAL);
                //if (mPubsubPublisher != null) {
                //    mSensorManager.registerListener(mPubsubPublisher.getTemperatureListener(), sensor,
                //            SensorManager.SENSOR_DELAY_NORMAL);
                //}
            } else if (sensor.getType() == Sensor.TYPE_PRESSURE) {
                // Our sensor is connected. Start receiving pressure data.
                mSensorManager.registerListener(mPressureListener, sensor,
                        SensorManager.SENSOR_DELAY_NORMAL);
                //if (mPubsubPublisher != null) {
                //    mSensorManager.registerListener(mPubsubPublisher.getPressureListener(), sensor,
                //            SensorManager.SENSOR_DELAY_NORMAL);
                //}
            }
        }
        @Override
        public void onDynamicSensorDisconnected(Sensor sensor) {
            super.onDynamicSensorDisconnected(sensor);
        }
    };

    // Callback when SensorManager delivers temperature data.
    private SensorEventListener mTemperatureListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            mLastTemperature = event.values[0];
            //Log.d(TAG, "温度反馈: " + mLastTemperature+"℃");
            updateTemperatureDisplay(mLastTemperature);
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d(TAG, "accuracy changed: " + accuracy);
        }
    };

    // Callback when SensorManager delivers pressure data.
    private SensorEventListener mPressureListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            mLastPressure = event.values[0];
            //Log.d(TAG, "气压反馈: " + mLastPressure*0.1 +"kPa");
            updateBarometerDisplay(mLastPressure);
            updateBarometer(mLastPressure);
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d(TAG, "accuracy changed: " + accuracy);
        }
    };

    ////////////////////////////////////////////////////////////////////////
    //////////////////////oncreate///////////////////////////////////////
    ///////////////////////////////////////////////////////////
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.things_main_activity);
        Log.d(TAG, "iMX7 Android Things Main Activity created.");

        //定义Homepage视图
        weatherTxt = (TextView) findViewById(R.id.weatherTxt);
        weatherTxt.setText("");
        weatherImage = findViewById(R.id.weatherImage);
        bmp280Image = (ImageView) findViewById(R.id.imageView);
        temperatureDisplay = (TextView) findViewById(R.id.temperatureDisplay);
        barometerDisplay = (TextView) findViewById(R.id.barometerDisplay);
        timeTxt = (TextView) findViewById(R.id.timeTxt);
        otherTxt = (TextView) findViewById(R.id.otherTxt);

        currTemp = (TextView) findViewById(R.id.currTemp);
        minTemp = (TextView) findViewById(R.id.minTemp);
        maxTemp = (TextView) findViewById(R.id.maxTemp);
        humidity = (TextView) findViewById(R.id.humidity);
        wind = (TextView) findViewById(R.id.wind);

        temperatureDisplay = (TextView) findViewById(R.id.temperatureDisplay);
        barometerDisplay = (TextView) findViewById(R.id.barometerDisplay);


        // We need permission to access the camera
        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // A problem occurred auto-granting the permission
            Log.e(TAG, "No permission");
            return;
        }


        // btb Get instance of Firebase database
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mStorage = FirebaseStorage.getInstance();
        //reference of alarm state
        mAlarmDatabaseReference = mFirebaseDatabase.getReference().child("alarmState");
        AlarmState nowAlarmState = new AlarmState("0");
        mAlarmDatabaseReference.setValue(nowAlarmState);

        // Creates new handlers and associated threads for camera and networking operations.
        //线程1，camera
        mCameraThread = new HandlerThread("CameraBackground");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());

        //线程2，实时数据库
        mCloudThread = new HandlerThread("CloudThread");
        mCloudThread.start();
        mCloudHandler = new Handler(mCloudThread.getLooper());

        //线程3，显示实时温度，湿度数据
        mHandler = new Mhandler();
        new Thread(new MyThread()).start();
        new Thread(new MyTimerThread()).start();
//        somethingIsMoving = NOTHING_MOVING;
        somethingIsMoving = SOMETHING_MOVING;
        alarmState = ALARMOFF;
        otherTxt.setText("Alarm OFF");
        new Thread(new MyBuzzerThread()).start();

        // Initialize the doorbell button driver
        initPIO();
        //initialize API
        // btb added for retrofit for weather API
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(WEATHER_API_BASE_URL).addConverterFactory(GsonConverterFactory.create())
                .build();
        //API
        apiService = retrofit.create(WeatherRESTAPIService.class);

        // Initialize Camera
        mCamera = DoorbellCamera.getInstance();
        mCamera.initializeCamera(this, mCameraHandler, mOnImageAvailableListener);

        // btb Listener will be called when changes were performed in DB
        //监听实时数据库中alarm开关变化
        mChildEventAlarmListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                // Deserialize data from DB into our AlarmState object
//                AlarmState rtAlarmState = dataSnapshot.getValue(AlarmState.class);
//                if(rtAlarmState.getAlarmState().equals("1")){
//                    alarmState = ALARMON;
//                }else if(rtAlarmState.getAlarmState().equals("0")){
//                    alarmState = ALARMOFF;
//                }

            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                //if alarm state in real time database changed, change alarmState in iMX7.
//                AlarmState rtAlarmState = dataSnapshot.getValue(AlarmState.class);
                //alarmState = rtAlarmState.getAlarmState();
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        };
        mAlarmDatabaseReference.addChildEventListener(mChildEventAlarmListener);



    }
    ///////////////end of onCreate()///////////////////

    private void initPIO() {
        try {
            //LED 灯，接在GPIO6_IO14口。
            PeripheralManager pio = PeripheralManager.getInstance();
            led = pio.openGpio(BoardDefaults.getGPIOForLED());
            PeripheralManager rgbLedPioR = PeripheralManager.getInstance();
            PeripheralManager rgbLedPioG = PeripheralManager.getInstance();
            PeripheralManager rgbLedPioB = PeripheralManager.getInstance();
//            rgbLed.set(0, rgbLedPioR.openGpio(BoardDefaults.getGPIOForRGBLED().get(0)));
//            rgbLed.set(1, rgbLedPioG.openGpio(BoardDefaults.getGPIOForRGBLED().get(1)));
//            rgbLed.set(2, rgbLedPioB.openGpio(BoardDefaults.getGPIOForRGBLED().get(2)));
            //开机不点亮LED灯
            led.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
//            rgbLed.get(0).setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
//            rgbLed.get(1).setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
//            rgbLed.get(2).setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
            //初始化 BMP280Sensor
            mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));
            try {
                mEnvironmentalSensorDriver = new Bmx280SensorDriver(BoardSpec.getI2cBus());
                mSensorManager.registerDynamicSensorCallback(mDynamicSensorCallback);
                mEnvironmentalSensorDriver.registerTemperatureSensor();
                mEnvironmentalSensorDriver.registerPressureSensor();
                Log.d(TAG, "Initialized I2C BMP280");
            } catch (IOException e) {
                throw new RuntimeException("Error initializing BMP280", e);
            }
            //初始化buzzerSpeaker(Zumbado)
            PeripheralManager buzzerPio = PeripheralManager.getInstance();
            buzzerSpeaker = buzzerPio.openGpio(BoardDefaults.getGPIOForBuzzer());
            buzzerSpeaker.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            //speaker.play(5);
            //初始化electronic lock

            //初始化electric switch



            //初始化按钮
            mButtonInputDriver = new ButtonInputDriver(
                    BoardDefaults.getGPIOForButton(),
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_ENTER);
            mButtonInputDriver.register();
            //初始化alarm按钮。
            mButtonAlarmInputDriver = new ButtonInputDriver(
                    BoardDefaults.getGPIOForAlarmButton(),
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_0);
            mButtonAlarmInputDriver.register();
        } catch (IOException e) {
            mButtonInputDriver = null;
            mButtonAlarmInputDriver = null;
            Log.w(TAG, "Could not open GPIO pins", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCamera.shutDown();
        mCameraThread.quitSafely();
        mCloudThread.quitSafely();

        try {
            led.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            led.close();
            buzzerSpeaker.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            buzzerSpeaker.close();
            mButtonInputDriver.close();
            mButtonAlarmInputDriver.close();
            if (speaker!=null) speaker.close();
        } catch (IOException e) {
            Log.e(TAG, "button driver error", e);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            //Log.d(TAG, "button pressed now");
            try {
                led.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
                buzzerSpeaker.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else if (keyCode == KeyEvent.KEYCODE_0) {
            //Log.d(TAG, "Alarm pressed now!!!");
            try {
                led.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
                buzzerSpeaker.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    ////按按钮后的响应
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            // Doorbell rang!
            Log.d(TAG, "button pressed");
            mCamera.takePicture();
            //speaker.play(5);
            try {
                led.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
                buzzerSpeaker.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }else if (keyCode == KeyEvent.KEYCODE_0) {
            Log.d(TAG, "Alarm button pressed!!!");
            try {
                led.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
                buzzerSpeaker.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(alarmState == ALARMOFF){
                alarmState = ALARMON;
                //change real time database alarm state
                Map<String, Object> childUpdates = new HashMap<>();
                childUpdates.put(mFirebaseDatabase.getReference().child("alarmState").getKey(), "1");
                mAlarmDatabaseReference.updateChildren(childUpdates);
                otherTxt.setText("Alarm ON");
                Log.d(TAG, "Alarm is on!!!");
            }else {
                alarmState = ALARMOFF;
                //change in real time database alarm state
                Map<String, Object> childUpdates = new HashMap<>();
                childUpdates.put(mFirebaseDatabase.getReference().child("alarmState").getKey(), "0");
                mAlarmDatabaseReference.updateChildren(childUpdates);
                otherTxt.setText("Alarm OFF");
                Log.d(TAG, "Alarm is off!!!");
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    /////////////////////////摄像头代码////////////////
    /**
     * Listener for new camera images.
     */
    private ImageReader.OnImageAvailableListener mOnImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = reader.acquireLatestImage();
                    // get image bytes
                    ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
                    final byte[] imageBytes = new byte[imageBuf.remaining()];
                    imageBuf.get(imageBytes);
                    image.close();

                    onPictureTaken(imageBytes);
                }
            };

    /**
     * Upload image data to Firebase as a doorbell event.
     */
    private void onPictureTaken(final byte[] imageBytes) {
        if (imageBytes != null) {
            //将doorbell record存在firebase 实时数据库
            final DatabaseReference doorbellRecord = mFirebaseDatabase.getReference("doorbellRecords").push();
            //image存入storage
            final StorageReference imageRef = mStorage.getReference().child(doorbellRecord.getKey());

            // upload image to storage
            UploadTask task = imageRef.putBytes(imageBytes);
            task.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    //获取下载的url，然后存入实时数据库，供客户端访问
                    Task<Uri> firebaseUri = taskSnapshot.getStorage().getDownloadUrl();
                    firebaseUri.addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Uri downloadUrl;
                            downloadUrl = uri;
                            // mark image in the database
                            Log.i(TAG, "Image upload successful");
                            doorbellRecord.child("timestamp").setValue(ServerValue.TIMESTAMP);
                            doorbellRecord.child("image").setValue(downloadUrl.toString());
                            // process image annotations
                            annotateImage(doorbellRecord, imageBytes);
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // clean up this entry
                    Log.w(TAG, "Unable to upload image to Firebase");
                    doorbellRecord.removeValue();
                }
            });
        }
    }

    /**
     * Process image contents with Cloud Vision.
     */
    private void annotateImage(final DatabaseReference ref, final byte[] imageBytes) {
        mCloudHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "sending image to cloud vision");
                // annotate image by uploading to Cloud Vision API
                try {
                    Map<String, Float> annotations = CloudVisionUtils.annotateImage(imageBytes);
                    Log.d(TAG, "cloud vision annotations:" + annotations);
                    if (annotations != null) {
                        ref.child("annotations").setValue(annotations);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Cloud Vison API error: ", e);
                }
            }
        });
    }
    /////////////////////摄像头代码////////////////

    ////initial menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.sign_out_menu:
                //sign out

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //fir timeView change per 1 second
    public void myTimer(final View v) {
        timeTxt.setText("");
        SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
        sDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+2"));
        String date = sDateFormat.format(new java.util.Date());
        timeTxt.append(date.toString());
    }

    ////////////get info from weather API
    public void obtenerInfo(final View v) {
        weatherTxt.setText("");
        currTemp.setText("");
        minTemp.setText("");
        maxTemp.setText("");
        humidity.setText("");
        wind.setText("");
        Call<Weather> call_async = apiService.getTempByCityName();
        Log.i(LOG_TAG, "getTempByCityName:" + apiService.getTempByCityName());
        // Asíncrona
        call_async.enqueue(new Callback<Weather>() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onResponse(Call<Weather> call, Response<Weather> response) {
                Weather weather = response.body();
                Log.i(LOG_TAG, "weather:" + weather);
                int dataListNum = 0;
                if (null != weather) {
                    Log.i(LOG_TAG, "getWeatherInfo:" + weather);

                    weatherTxt.append(weather.getWeather().get(0).getMain()+"   (");
                    weatherTxt.append(weather.getWeather().get(0).getDescription()+ ")");
                    DecimalFormat df = new DecimalFormat("#.0");
                    currTemp.append(df.format(weather.getMain().getTemp()-273.15));
                    minTemp.append(df.format(weather.getMain().getTemp_min()-273.15));
                    maxTemp.append(df.format(weather.getMain().getTemp_max()-273.15));
                    humidity.append(weather.getMain().getHumidity().toString());
                    wind.append(weather.getWind().getSpeed().toString());

                    /////////////////show icon of weather:
                    String icon = weather.getWeather().get(0).getIcon();
                    if (icon.contains("01d")){
                        weatherImage.setImageResource(R.drawable.w01d);
                    }else if (icon.contains("02d")){
                        weatherImage.setImageResource(R.drawable.w02d);
                    }else if (icon.contains("03d")){
                        weatherImage.setImageResource(R.drawable.w03d);
                    }else if (icon.contains("04d")) {
                        weatherImage.setImageResource(R.drawable.w04d);
                    }else if (icon.contains("09d")){
                        weatherImage.setImageResource(R.drawable.w09d);
                    }else if (icon.contains("10d")){
                        weatherImage.setImageResource(R.drawable.w10d);
                    }else if (icon.contains("11d")){
                        weatherImage.setImageResource(R.drawable.w11d);
                    } else if (icon.contains("13d")){
                        weatherImage.setImageResource(R.drawable.w13d);
                    } else if (icon.contains("01n")){
                        timeTxt.setBackgroundColor(R.color.colorSkyN);
                        otherTxt.setBackgroundColor(R.color.colorSkyN);
                        weatherImage.setBackgroundColor(R.color.colorSkyN);
                        weatherImage.setImageResource(R.drawable.w01n);
                    }else if (icon.contains("02n")){
                        timeTxt.setBackgroundColor(R.color.colorSkyN);
                        otherTxt.setBackgroundColor(R.color.colorSkyN);
                        weatherImage.setBackgroundColor(R.color.colorSkyN);
                        weatherImage.setImageResource(R.drawable.w02n);
                    }else if (icon.contains("03n")){
                        timeTxt.setBackgroundColor(R.color.colorSkyN);
                        otherTxt.setBackgroundColor(R.color.colorSkyN);
                        weatherImage.setBackgroundColor(R.color.colorSkyN);
                        weatherImage.setImageResource(R.drawable.w03n);
                    }else if (icon.contains("04n")) {
                        timeTxt.setBackgroundColor(R.color.colorSkyN);
                        otherTxt.setBackgroundColor(R.color.colorSkyN);
                        weatherImage.setBackgroundColor(R.color.colorSkyN);
                        weatherImage.setImageResource(R.drawable.w04n);
                    }else if (icon.contains("09n")){
                        timeTxt.setBackgroundColor(R.color.colorSkyN);
                        otherTxt.setBackgroundColor(R.color.colorSkyN);
                        weatherImage.setBackgroundColor(R.color.colorSkyN);
                        weatherImage.setImageResource(R.drawable.w09n);
                    }else if (icon.contains("10n")){
                        timeTxt.setBackgroundColor(R.color.colorSkyN);
                        otherTxt.setBackgroundColor(R.color.colorSkyN);
                        weatherImage.setBackgroundColor(R.color.colorSkyN);
                        weatherImage.setImageResource(R.drawable.w10n);
                    }else if (icon.contains("11n")){
                        timeTxt.setBackgroundColor(R.color.colorSkyN);
                        otherTxt.setBackgroundColor(R.color.colorSkyN);
                        weatherImage.setBackgroundColor(R.color.colorSkyN);
                        weatherImage.setImageResource(R.drawable.w11n);
                    } else if (icon.contains("13n")){
                        timeTxt.setBackgroundColor(R.color.colorSkyN);
                        otherTxt.setBackgroundColor(R.color.colorSkyN);
                        weatherImage.setBackgroundColor(R.color.colorSkyN);
                        weatherImage.setImageResource(R.drawable.w13n);
                    } else {
                        weatherImage.setImageResource(R.drawable.w01n);
                    }
                } else {
                    Log.i(LOG_TAG, "no data from external API.");
                }
            }

            @Override
            public void onFailure(Call<Weather> call, Throwable t) {
                Toast.makeText(
                        getApplicationContext(),
                        "ERROR: " + t.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
                Log.e(LOG_TAG, t.getMessage());

            }
        });
    }
    ///////get info from weather API

    ////////////BPM280 sensor
    private void updateBarometerDisplay(float pressure) {
        // Update UI.
        if (!mHandler.hasMessages(MSG_UPDATE_BAROMETER)) {
            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_BAROMETER, 5000);
        }
    }
    private void updateTemperatureDisplay(float pressure) {
        // Update UI.
        if (!mHandler.hasMessages(MSG_UPDATE_TEMPERATURE)) {
            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_TEMPERATURE, 5000);
        }
    }

    private void updateBarometer(float pressure) {
        // Update UI.
        if (!mHandler.hasMessages(MSG_UPDATE_BAROMETER_UI)) {
            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_BAROMETER_UI, 5000);
        }
    }
    /////////////BPM280 sensor

    private void startBuzzerAlarm() throws IOException {
        stopBuzzer();
        if(alarmState == ALARMON){
            otherTxt.setText("Alarm ON");
            if(somethingIsMoving == SOMETHING_MOVING){
                Log.d(TAG, "Alarm State ON!!!");
                try {
                    startBuzzer();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }else {
            otherTxt.setText("Alarm OFF");
        }
    }
    private void startBuzzer() throws IOException {
        buzzerSpeaker.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
    }
    private void stopBuzzer() throws IOException {
        buzzerSpeaker.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
    }
}