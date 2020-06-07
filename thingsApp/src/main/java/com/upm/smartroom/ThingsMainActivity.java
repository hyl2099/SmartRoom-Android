package com.upm.smartroom;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
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
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.things.contrib.driver.bmx280.Bmx280SensorDriver;
import com.upm.smartroom.board.BoardSpec;
import com.upm.smartroom.doorbell.DoorbellMsgActivity;
import com.upm.smartroom.plant.PlantMainActivity;
import com.upm.smartroom.postData.PictureRESTAPIService;
import com.upm.smartroom.postData.SpringPicture;
import com.upm.smartroom.postData.SpringTemperature;
import com.upm.smartroom.postData.TemperatureRESTAPIService;
import com.upm.smartroom.state.AlarmState;
import com.upm.smartroom.board.BoardDefaults;
import com.upm.smartroom.doorbell.DoorbellCamera;
import com.upm.smartroom.state.RoomTemperature;
import com.upm.smartroom.state.SwitchState;
import com.upm.smartroom.weather.Weather;
import com.upm.smartroom.weather.WeatherRESTAPIService;

import org.json.JSONException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class ThingsMainActivity extends AppCompatActivity {
    private static final String TAG = ThingsMainActivity.class.getSimpleName();
    private static final String LOG_TAG = ThingsMainActivity.class.getSimpleName();

    //data from weather API
    private ImageView weatherImage;
    private TextView weatherTxt;
    private TextView currTemp;
    private TextView minTemp;
    private TextView maxTemp;
    private TextView humidity;
    private TextView wind;
    //data from sensor for view
    private ImageView bmp280Image;
    private TextView temperatureDisplay;
    private TextView barometerDisplay;
    private TextView timeTxt;
    private ViewGroup otherTxt;
    private TextView alarmTxt;
    private TextView switchTxt;
    private Switch alarmSwitcher;
    private Switch switchSwitcher;


    // btb Firebase database variables
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mAlarmDatabaseReference;
    private DatabaseReference mSwitchDatabaseReference;
    private DatabaseReference mRoomTempDatabaseReference;
    private FirebaseStorage mStorage;
    private ChildEventListener mChildEventAlarmListener;
    private ChildEventListener mChildEventSwitchListener;

    // doorbell
    private DoorbellCamera mCamera;

    //weather API
    private static final String WEATHER_API_BASE_URL = "https://api.openweathermap.org";
    //Spring Temperature API
    private static final String SPRING_API_BASE_URL = "http://192.168.1.55:8080";
    private WeatherRESTAPIService apiService;
    private TemperatureRESTAPIService temperatureRESTAPIService;
    private PictureRESTAPIService pictureRESTAPIService;
    private SpringTemperature springTemperature;

    // buttons
    //Driver for the doorbell button GPIO2_IO05;
    private ButtonInputDriver mButtonCameraInputDriver;
    //Driver for Alarm button GPIO2_IO07
    private ButtonInputDriver mButtonAlarmInputDriver;
    //alarm状态
    private int alarmState;
    private static final int ALARMON = 1;
    private static final int ALARMOFF = 0;

    //LED GPIO06_IO14
    private Gpio led;
    //LED开关状态
    private int switchState;
    private static final int SWITCHON = 1;
    private static final int SWITCHOFF = 0;

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
    private static final int POST_DATA_TO_SPRING =7;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.0");

    //speaker: active buzzer arduino GPI06_IO15
    private Gpio buzzerSpeaker;

    //sendor movement GPI06_IO13
    private Gpio movementSensor;
    //detect something moving
    private int somethingIsMoving;
    private  static final int SOMETHING_MOVING = 1;
    private  static final int NOTHING_MOVING = 0;

    //设置一个常量，根据相机按钮是否按下拍照来判断拍照是camera还是movement
    //1 -- camera; 2 -- movement
    private  static int DOORBELL_MOVEMENT;

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
    public Handler mHandler;
    class Mhandler extends Handler {
        private int mBarometerImage = -1;
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    //get weather data from API and update
                    obtenerInfo(findViewById(R.id.weatherTxt));
                    //update room temperature to mobile
                    break;
                case 2:
                    //the Clock on screen.
                    myTimer(findViewById(R.id.timeTxt));
                    break;
                case 3:
//                    if(somethingIsMoving == SOMETHING_MOVING){
//                        try {
//                            startBuzzerAlarm();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
                    break;
                case POST_DATA_TO_SPRING:
                    // 7
                    //post temperature data to spring every 10 mins.
//                    try {
//                        postDataToSpring();
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                    break;
                case MSG_UPDATE_BAROMETER_UI:
                    //4
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
                    //5
                    temperatureDisplay.setText(DECIMAL_FORMAT.format(mLastTemperature));
                    Map<String, Object> childTemperatureUpdates = new HashMap<>();
                    childTemperatureUpdates.put("mLastTemperature", mLastTemperature);
                    mRoomTempDatabaseReference.updateChildren(childTemperatureUpdates);
                    break;
                case MSG_UPDATE_BAROMETER:
                    //6
                    barometerDisplay.setText(DECIMAL_FORMAT.format(mLastPressure*0.1));
                    Map<String, Object> childPressureUpdates = new HashMap<>();
                    childPressureUpdates.put("mLastPressure", mLastPressure);
                    mRoomTempDatabaseReference.updateChildren(childPressureUpdates);
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
                    //per min update the API weather and room temperature to mobile
                    Thread.sleep(1000*60);
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
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //thread for POST temperature to Sping server.
    class MyPostDataThread implements Runnable {
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                Message message = new Message();
                message.what = 7;
                mHandler.sendMessage(message);
                try {
                    Thread.sleep(100*60*10);
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
            } else if (sensor.getType() == Sensor.TYPE_PRESSURE) {
                // Our sensor is connected. Start receiving pressure data.
                mSensorManager.registerListener(mPressureListener, sensor,
                        SensorManager.SENSOR_DELAY_NORMAL);
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

    //for movment sensor callback
    private final GpioCallback mMoveSensorCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            try {
                if (gpio.getValue()) {
                    somethingIsMoving = SOMETHING_MOVING;
                    Log.e("有人来了", gpio.getValue() + ":111111111111111111111111111111111111111111111111111111111111111111111111111111");
                    startMovementAlarm();
                } else {
                    somethingIsMoving = NOTHING_MOVING;
                    Log.e("没有人", gpio.getValue() + ":2222222222222222222222222222222222222222222222222222222222222222222222222222222222");
                    //stopBuzzer();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
    };



    ////////////////////////////////////////////////////////////////////////
    //////////////////////oncreate///////////////////////////////////////
    ///////////////////////////////////////////////////////////
    //////////////////////////////////////////
    //////////////////////////////////////////
    //////////////////////////////////////////
    //////////////////////////////////////////
    @SuppressLint("CutPasteId")
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
        otherTxt = findViewById(R.id.otherTxt);
        alarmTxt = (TextView) findViewById(R.id.alarmTxt);
        switchTxt = (TextView) findViewById(R.id.switchTxt);
        alarmSwitcher = (Switch)findViewById(R.id.alarmSwitch);
        switchSwitcher = (Switch)findViewById(R.id.switchSwitcher);

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
        //判断是camera还是movement
        DOORBELL_MOVEMENT =0;

        // Get instance of Firebase database
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mStorage = FirebaseStorage.getInstance();
        //reference of alarm , switch state
        //初始化时alarm，switch状态根据数据库中数据来完成
        mAlarmDatabaseReference = mFirebaseDatabase.getReference().child("alarmState");
        //Alarm ON
        AlarmState nowAlarmState = new AlarmState("1");
        mAlarmDatabaseReference.setValue(nowAlarmState);
        alarmSwitcher.setChecked(true);
        somethingIsMoving = NOTHING_MOVING;
        alarmState = ALARMON;
        alarmTxt.setText("ALARM ON");
        mSwitchDatabaseReference = mFirebaseDatabase.getReference().child("switchState");
        //Switch OFF
        SwitchState nowSwitchState = new SwitchState("0");
        mSwitchDatabaseReference.setValue(nowSwitchState);
        switchSwitcher.setChecked(false);
        switchState = SWITCHOFF;
        switchTxt.setText("SWITCH OFF");

        //Temperature and Humidity
        mRoomTempDatabaseReference = mFirebaseDatabase.getReference().child("roomTemperature");
        RoomTemperature nowRoomTemperature = new RoomTemperature(mLastTemperature, mLastPressure);
        mRoomTempDatabaseReference.setValue(nowRoomTemperature);

        // Initialize Camera
        mCamera = DoorbellCamera.getInstance();
        mCamera.initializeCamera(this, mCameraHandler, mOnImageAvailableListener);

        // Initialize the doorbell button driver, and other sensors
        initPIO();

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
        new Thread(new MyBuzzerThread()).start();
        new Thread(new MyPostDataThread()).start();

        //initialize API
        // added for retrofit for weather API
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(WEATHER_API_BASE_URL).addConverterFactory(GsonConverterFactory.create())
                .build();
        //API
        apiService = retrofit.create(WeatherRESTAPIService.class);
        Retrofit retrofitSpring = new Retrofit.Builder()
                .baseUrl(SPRING_API_BASE_URL).addConverterFactory(GsonConverterFactory.create())
                .build();
        temperatureRESTAPIService = retrofitSpring.create(TemperatureRESTAPIService.class);
        pictureRESTAPIService = retrofitSpring.create(PictureRESTAPIService.class);


        // Alarm and switch states
        alarmSwitcher.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    Map<String, Object> childUpdates = new HashMap<>();
                    childUpdates.put(mAlarmDatabaseReference.getKey(), "1");
                    mAlarmDatabaseReference.updateChildren(childUpdates);
                    Log.d(TAG, "switch check Alarm is on!!!");

                }else {
                    Map<String, Object> childUpdates = new HashMap<>();
                    childUpdates.put(mAlarmDatabaseReference.getKey(), "0");
                    mAlarmDatabaseReference.updateChildren(childUpdates);
                    Log.d(TAG, "switch check  Alarm is off!!!");
                }
            }
        });
        //是否点亮LED
        switchSwitcher.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    Map<String, Object> childUpdates = new HashMap<>();
                    childUpdates.put(mSwitchDatabaseReference.getKey(), "1");
                    mSwitchDatabaseReference.updateChildren(childUpdates);
                    Log.d(TAG, "switch check Switch is on!!!");
                }else {
                    Map<String, Object> childUpdates = new HashMap<>();
                    childUpdates.put(mSwitchDatabaseReference.getKey(), "0");
                    mSwitchDatabaseReference.updateChildren(childUpdates);
                    Log.d(TAG, "switch check Switch is off!!!");
                }
            }
        });


        // Listener will be called when changes were performed in DB
        //监听实时数据库中alarm开关变化
        mChildEventAlarmListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                // Deserialize data from DB into our AlarmState object
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                //if alarm state in real time database changed, change alarmState in iMX7.
                String rtAlarmState = (String) dataSnapshot.getValue().toString();
                if(rtAlarmState.equals("1")){
                    alarmState = ALARMON;
                    if(!alarmSwitcher.isChecked()){
                        alarmSwitcher.setChecked(true);
                    }
                    try {
                        startBuzzerAlarm();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "Database alarm is on!!!");
                }else if(rtAlarmState.equals("0")){
                    alarmState = ALARMOFF;
                    if(alarmSwitcher.isChecked()) {
                        alarmSwitcher.setChecked(false);
                    }
                    try {
                        stopBuzzer();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "Database alarm is off!!!");
                }
            }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}
            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        };
        mAlarmDatabaseReference.addChildEventListener(mChildEventAlarmListener);
        mChildEventSwitchListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                //if switch state in real time database changed, change alarmState in iMX7.
                String rtSwitchState = (String) dataSnapshot.getValue();
                if(rtSwitchState.equals("1")){
                    switchState = SWITCHON;
                    if(!switchSwitcher.isChecked()){
                        switchSwitcher.setChecked(true);
                    }
                    try {
                        switchOn();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "Database switch is on!!!");
                }else if(rtSwitchState.equals("0")){
                    switchState = SWITCHOFF;
                    if(switchSwitcher.isChecked()){
                        switchSwitcher.setChecked(false);
                    }
                    try {
                        switchOff();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "Database switch is off!!!");
                }
            }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}
            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        };
        mSwitchDatabaseReference.addChildEventListener(mChildEventSwitchListener);
    }
    ///////////////end of onCreate()///////////////////
    //////////////////////////////////////////////////
    /////////////////////////////////////////////////
    ///////////////////////////////////////////////

    private void initPIO() {
        try {
            //1,LED;2,CameraButton;3,AlarmButton;4,Buzzer
            //5,sensor movement; 6,BPM280;7,switch;
            //1................
            //LED 灯，接在GPIO6_IO14口。
            PeripheralManager pio = PeripheralManager.getInstance();
            led = pio.openGpio(BoardDefaults.getGPIOForLED());
            //开机不点亮LED灯
            led.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            //2................
            //初始化camera按钮
            mButtonCameraInputDriver = new ButtonInputDriver(
                    BoardDefaults.getGPIOForCameraButton(),
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_ENTER);
            mButtonCameraInputDriver.register();
            //3............
            //初始化alarm按钮。
            mButtonAlarmInputDriver = new ButtonInputDriver(
                    BoardDefaults.getGPIOForAlarmButton(),
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_0);
            mButtonAlarmInputDriver.register();
            //4.................
            //初始化buzzerSpeaker(Zumbado)
            PeripheralManager buzzerPio = PeripheralManager.getInstance();
            buzzerSpeaker = buzzerPio.openGpio(BoardDefaults.getGPIOForBuzzer());
            buzzerSpeaker.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            //5.............
            //初始化HC_SR501 motion sensor
            try {
                movementSensor = buzzerPio.openGpio(BoardDefaults.getGPIOForMovementSensor());//Echo针脚
                movementSensor.setDirection(Gpio.DIRECTION_IN);//将引脚初始化为输入
                movementSensor.setActiveType(Gpio.ACTIVE_HIGH);//设置收到高电压是有效的结果
                //注册状态更改监听类型 EDGE_NONE（无更改，默认）EDGE_RISING（从低到高）EDGE_FALLING（从高到低）
                movementSensor.setEdgeTriggerType(Gpio.EDGE_BOTH);
                movementSensor.registerGpioCallback(mMoveSensorCallback);//注册回调
            } catch (IOException e) {
                e.printStackTrace();
            }
            //6....................
//            初始化 BMP280Sensor
            mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));
//            try {
//                mEnvironmentalSensorDriver = new Bmx280SensorDriver(BoardSpec.getI2cBus());
//                mSensorManager.registerDynamicSensorCallback(mDynamicSensorCallback);
//                mEnvironmentalSensorDriver.registerTemperatureSensor();
//                mEnvironmentalSensorDriver.registerPressureSensor();
//                Log.d(TAG, "Initialized I2C BMP280");
//            } catch (IOException e) {
//                throw new RuntimeException("Error initializing BMP280", e);
//            }
            //7..................
            //初始化electric switch to water plants
//            waterPlant = buzzerPio.openGpio(BoardDefaults.getGPIOForSwitcher());
//            waterplant.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        } catch (IOException e) {
            mButtonCameraInputDriver = null;
            mButtonAlarmInputDriver = null;
            Log.w(TAG, "Could not open GPIO pins", e);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        dettachDatabseReadListener();
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

            mButtonCameraInputDriver.close();
            mButtonAlarmInputDriver.close();
        } catch (IOException e) {
            Log.e(TAG, "button driver error", e);
        }
        if (mMoveSensorCallback != null) {
            try {
                movementSensor.close();
            } catch (IOException e) {
                Log.w(TAG, "Unable to close mEchoGpio", e);
            } finally {
                movementSensor = null;
            }
        }
        movementSensor.unregisterGpioCallback(mMoveSensorCallback);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) { //for camera button
            //Log.d(TAG, "button pressed now");
            try {
//                led.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
                buzzerSpeaker.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else if (keyCode == KeyEvent.KEYCODE_0) { //for alarm button
            //Log.d(TAG, "Alarm pressed now!!!");
            try {
//                led.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
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
        if (keyCode == KeyEvent.KEYCODE_ENTER) { //for camera button
            // Doorbell rang!
            Log.d(TAG, "Door bell button pressed");
            mCamera.takePicture();
            DOORBELL_MOVEMENT = 1;
            try {
//                led.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
                buzzerSpeaker.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }else if (keyCode == KeyEvent.KEYCODE_0) { //for alarm button
            Log.d(TAG, "Alarm button pressed!!!");
            try {
//                led.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
                buzzerSpeaker.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //turn on od off alarm
            if(alarmState == ALARMOFF){
                alarmState = ALARMON;
                //change real time database alarm state
                Map<String, Object> childUpdates = new HashMap<>();
                childUpdates.put(mFirebaseDatabase.getReference().child("alarmState").getKey(), "1");
                mAlarmDatabaseReference.updateChildren(childUpdates);
                alarmTxt.setText("Alarm ON");
                Log.d(TAG, "Alarm is on!!!");
            }else {
                alarmState = ALARMOFF;
                //change in real time database alarm state
                Map<String, Object> childUpdates = new HashMap<>();
                childUpdates.put(mFirebaseDatabase.getReference().child("alarmState").getKey(), "0");
                mAlarmDatabaseReference.updateChildren(childUpdates);
                alarmTxt.setText("Alarm OFF");
                Log.d(TAG, "Alarm is off!!!");
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    /////////////////////////摄像头代码////////////////
    //Listener for new camera images.
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
    //Upload image data to Firebase as a doorbell event.
    private void onPictureTaken(final byte[] imageBytes) {
        if (imageBytes != null) {
            final StorageReference imageRef;
            final DatabaseReference movementRecord = mFirebaseDatabase.getReference("movementRecords").push();
            final DatabaseReference doorbellRecord = mFirebaseDatabase.getReference("doorbellRecords").push();
            final DatabaseReference dbRef;
            if(DOORBELL_MOVEMENT ==1){
                dbRef = doorbellRecord;
            }else{
                dbRef = movementRecord;
            }
            //将doorbell record存在firebase 实时数据库
            //image存入storage
            imageRef = mStorage.getReference().child(dbRef.getKey());

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
                            dbRef.child("timestamp").setValue(ServerValue.TIMESTAMP);
                            dbRef.child("image").setValue(downloadUrl.toString());
                            // process image annotations
                            annotateImage(dbRef, imageBytes);
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // clean up this entry
                    Log.w(TAG, "Unable to upload image to Firebase");
                    dbRef.removeValue();
                }
            });
            // upload image to my Spring server
            //二，将图片上传到本地Spring后端
            //imageBytes 图片byte[]文件
            //owner
            RequestBody owner = RequestBody.create(null, "owner");
            //remark
            RequestBody remark;
            if(DOORBELL_MOVEMENT ==1){
                remark = RequestBody.create(null, "doorbell");
            }else{
                remark = RequestBody.create(null, "movement");
            }
            //file
            MultipartBody.Part file = toMultiPartFile(imageBytes);
            pictureRESTAPIService.addPhoto(file,owner,remark).enqueue(new Callback<SpringPicture>() {
                @Override
                public void onResponse(Call<SpringPicture> call, Response<SpringPicture> response) {
                    if(response.isSuccessful()) {
                        Log.i(TAG, "post picture  OK.");
                    }
                }
                @Override
                public void onFailure(Call<SpringPicture> call, Throwable t) {
                    Log.e(TAG, "Unable to submit post picture to API.");
                }
            });
            DOORBELL_MOVEMENT = 0;
        }
    }
    public static MultipartBody.Part toMultiPartFile(byte[] byteArray) {
        RequestBody reqFile = RequestBody.create(MediaType.parse("image/jpeg"), byteArray);
        return MultipartBody.Part.createFormData("file",
                "doorbell", // filename, this is optional
                reqFile);
    }
    //Process image contents with Cloud Vision.
    private void annotateImage(final DatabaseReference ref, final byte[] imageBytes) {
        mCloudHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "sending image to cloud vision");
                // annotate image by uploading to Cloud Vision API
                try {
                    Map<String, Float> annotations = CloudVisionUtils.annotateImage(imageBytes);
//                    Log.d(TAG, "cloud vision annotations:" + annotations);
                    if (annotations != null) {
                        ref.child("annotations").setValue(annotations);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Cloud Vison API error: ", e);
                }
            }
        });
    }
    /////////////////////摄像头代码结束////////////////

    //////////initial menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.opcMessage:
                startActivity(new Intent(this, DoorbellMsgActivity.class));
                return true;
            case R.id.opcPlants:
                startActivity(new Intent(this, PlantMainActivity.class));
                return true;
            case R.id.opcAbout:
                startActivity(new Intent(this, About.class));
                return true;
//            case R.id.sign_out_menu:
                //sign out
//                return true;
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
//        Log.i(LOG_TAG, "getTempByCityName:" + apiService.getTempByCityName());
        // Asíncrona
        call_async.enqueue(new Callback<Weather>() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onResponse(Call<Weather> call, Response<Weather> response) {
                Weather weather = response.body();
//                Log.i(LOG_TAG, "weather:" + weather);
                int dataListNum = 0;
                if (null != weather) {
//                    Log.i(LOG_TAG, "getWeatherInfo:" + weather);
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
    ///////get info from weather API finish/////////


    ////////////BPM280 sensor///////
    private void updateBarometerDisplay(float pressure) {
        // Update UI.
        if (!mHandler.hasMessages(MSG_UPDATE_BAROMETER)) {
            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_BAROMETER, 1000);
        }
    }
    private void updateTemperatureDisplay(float pressure) {
        // Update UI.
        if (!mHandler.hasMessages(MSG_UPDATE_TEMPERATURE)) {
            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_TEMPERATURE, 1000);
        }
    }
    private void updateBarometer(float pressure) {
        // Update UI.
        if (!mHandler.hasMessages(MSG_UPDATE_BAROMETER_UI)) {
            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_BAROMETER_UI, 1000);
        }
    }
    //update room temperature to mobile
    private void updateRoomTemperature(){

    }
    /////////////BPM280 sensor finish/////////


    private void startBuzzerAlarm() throws IOException {
        stopBuzzer();
        if(alarmState == ALARMON){
            alarmTxt.setText("Alarm ON");
            if(somethingIsMoving == SOMETHING_MOVING){
                Log.d(TAG, "SOMETHING_MOVING Alarm State ON!!!");
                try {
                    startBuzzer();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }else {
            alarmTxt.setText("unable to start buzzer...");
        }
    }
    private void startMovementAlarm() throws IOException {
        //stopBuzzer();
        if(alarmState == ALARMON){
            Log.d(TAG, "startMovementAlarm Alarm ON11111111111111111111111111111111111111111!!!");
                Log.d(TAG, "Someoen is coming111111111111111111111111111-----ook picture11111111111111!!!");
                try {
                    startBuzzer();
                    mCamera.takePicture();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }
    private void startBuzzer() throws IOException {
        buzzerSpeaker.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
    }
    private void stopBuzzer() throws IOException {
        alarmTxt.setText("Alarm OFF");
        Log.d(TAG, "Alarm State OFF!!!");
        buzzerSpeaker.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
    }

    private void startSwitchOn() throws IOException {
        if(switchState ==SWITCHON){
            switchOn();
            switchTxt.setText("Switcher ON");
            Log.d(TAG, "Switch is ON!!!");
        }else if(switchState ==SWITCHOFF){
            switchOff();
            switchTxt.setText("Switcher OFF");
            //Log.d(TAG, "Switch is OFF!!!");
        }
    }
    //开关开，LED亮
    private void switchOn() throws IOException {
        led.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
    }
    //开关关，LED灭
    private void switchOff() throws IOException {
        led.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
    }

    // btb method implemented
    private void dettachDatabseReadListener(){
        if(mChildEventAlarmListener!=null){
            mAlarmDatabaseReference.removeEventListener(mChildEventAlarmListener);
            mChildEventAlarmListener = null;
        }
    }

    private void postDataToSpring() throws JSONException {
        Call<Weather> call_async = apiService.getTempByCityName();
        call_async.enqueue(new Callback<Weather>() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onResponse(Call<Weather> call, Response<Weather> response) {
                Weather weather = response.body();
                DecimalFormat df = new DecimalFormat("#.0");
                Float outdoorTemperature = (float)(weather.getMain().getTemp()-273.15);
                Float outdoorHumidity = weather.getMain().getHumidity().floatValue();
                Float indoorTemperature = mLastTemperature;
                Float indoorHumidity = (float)(mLastPressure*0.1);
                String t1 = getNowTimeFormat();
                springTemperature  = new SpringTemperature(indoorTemperature,indoorHumidity,outdoorTemperature,outdoorHumidity,t1);
                sendTemperaturePost(springTemperature);
                Log.i(LOG_TAG, "Add temperature");
            }
            @Override
            public void onFailure(Call<Weather> call, Throwable throwable) {

            }
        });
    }

    public void getTempFromSpring(){
        Call<List<SpringTemperature>> call_async = temperatureRESTAPIService.readTemperatureAll();
        call_async.enqueue(new Callback<List<SpringTemperature>>() {
            @Override
            public void onResponse(Call<List<SpringTemperature>> call, Response<List<SpringTemperature>> response) {
                List<SpringTemperature> t = response.body();
                Log.e(TAG, "get  temperature from API." + t.get(0).getTemperatureOutdoor() + t.get(0).getHumidityOutdoor());
            }
            @Override
            public void onFailure(Call<List<SpringTemperature>> call, Throwable t) {
                Log.e(TAG, "Unable to get  temperature from API.");
            }
        });
    }

    public void sendTemperaturePost(SpringTemperature t) {
        //笔记：
        //temperatureRESTAPIService (APIService 接口的实例) saveTemperature方法会返回一个Call 对象, 这个对象有个enqueue(Callback callback) 方法.
        //enqueue() 异步地发送请求, 然后当响应回来的时候, 使用回调的方式通知你的APP. 因为这个请求是异步的, Retrofit 使用一个另外的线程去执行它, 这样UI 线程就不会被阻塞了.
        //要使用enqueue() 方法, 你需要实现两个回调方法: onResponse() 和onFailure(). 对于一个请求, 当响应回来的时候, 只有一个方法会被执行.
        //onResponse(): 在收到HTTP 响应的时候被调用. 这个方法在服务器可以处理请求的情况下调用, 即使服务器返回的是一个错误的信息. 例如你获取到的响应状态是404 或500. 你可以使用response.code() 来获取状态码, 以便进行不同的处理. 当然你也可以直接用isSuccessful() 方法来判断响应的状态码是不是在200-300 之间(在这个范围内标识是成功的).
        //onFailure(): 当和服务器通信出现网络异常时, 或者在处理请求出现不可预测的错误时, 会调用这个方法.
        temperatureRESTAPIService.saveTemperature(t).enqueue(new Callback<SpringTemperature>() {
            @Override
            public void onResponse(Call<SpringTemperature> call, Response<SpringTemperature> response) {
                if(response.isSuccessful()) {
                    //showResponse(response.body().toString());
                    Log.i(TAG, "post temperature OK." + response.body().toString());
                }
            }
            @Override
            public void onFailure(Call<SpringTemperature> call, Throwable t) {
                Log.e(TAG, "Unable to submit post temperature to API.");
            }
        });
    }

    public void showResponse(String response) {
       //TODO
    }

    private String getNowTimeFormat(){
        Long time=System.currentTimeMillis();//long now = android.os.SystemClock.uptimeMillis();
        SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date d1=new Date(time);
        String t1=format.format(d1);
        return t1;
    }

}