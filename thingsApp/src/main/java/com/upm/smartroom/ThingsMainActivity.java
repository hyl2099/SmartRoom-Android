package com.upm.smartroom;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.things.contrib.driver.bmx280.Bmx280SensorDriver;
import com.upm.smartroom.doorbell.DoorbellActivity;
import com.upm.smartroom.doorbell.DoorbellCamera;
import com.upm.smartroom.weather.Weather;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class ThingsMainActivity extends AppCompatActivity {
    private static final String TAG = DoorbellActivity.class.getSimpleName();
    private static final String LOG_TAG = DoorbellActivity.class.getSimpleName();



    private TextView weatherTxt;
    private ImageView weatherImage;


    // btb Firebase database variables
    private FirebaseDatabase mFirebaseDatabase;
    private FirebaseStorage mStorage;


    private DoorbellCamera mCamera;

    //weather API
    private static final String WEATHER_API_BASE_URL = "https://api.openweathermap.org";
    private WeatherRESTAPIService apiService;

    /**
     * Driver for the doorbell button;
     */
    private ButtonInputDriver mButtonInputDriver;
    //LED GPIO06_IO14
    private Gpio led;
    //BMP280 for temperature and humidity
    private Bmx280SensorDriver mEnvironmentalSensorDriver;
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


    //定义我的handler
    //handler
    public Handler mHandler;
    class Mhandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    obtenerInfo(findViewById(R.id.weatherTxt));
                    break;
                case 2:
                    //obtenerInfo(findViewById(R.id.tvRespuesta));
                    break;
            }
        }
    }
    class MyThread implements Runnable {
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                Message message = new Message();
                message.what = 1;
                mHandler.sendMessage(message);
                try {
                    Thread.sleep(10000);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }




    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.things_main_activity);
        Log.d(TAG, "iMX7 Android Things Main Activity created.");

        //定义Homepage视图
        weatherTxt = (TextView) findViewById(R.id.weatherTxt);
        weatherTxt.setText("");
        weatherImage = findViewById(R.id.weatherImage);


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

    }
    ///////////////end of onCreate()///////////////////

    private void initPIO() {
        PeripheralManager pio = PeripheralManager.getInstance();
        try {
            //LED 灯，接在GPIO6_IO14口。
            led = pio.openGpio(BoardDefaults.getGPIOForLED());
            //点亮LED灯
            led.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
            //初始化 BMP280Sensor

            //初始化speaker(Zumbado)

            //初始化electronic lock

            //初始化electric switch



            //初始化按钮
            mButtonInputDriver = new ButtonInputDriver(
                    BoardDefaults.getGPIOForButton(),
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_ENTER);
            mButtonInputDriver.register();
        } catch (IOException e) {
            mButtonInputDriver = null;
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
            mButtonInputDriver.close();
        } catch (IOException e) {
            Log.e(TAG, "button driver error", e);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            // Doorbell rang!
            Log.d(TAG, "button pressed");
            mCamera.takePicture();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

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
            //将log存在firebase 实时数据库
            final DatabaseReference log = mFirebaseDatabase.getReference("logs").push();
            //image存入storage
            final StorageReference imageRef = mStorage.getReference().child(log.getKey());

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
                            log.child("timestamp").setValue(ServerValue.TIMESTAMP);
                            log.child("image").setValue(downloadUrl.toString());
                            // process image annotations
                            annotateImage(log, imageBytes);
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // clean up this entry
                    Log.w(TAG, "Unable to upload image to Firebase");
                    log.removeValue();
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

    //initial menu
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


    //click button to get info
    public void obtenerInfo(final View v) {
        weatherTxt.setText("");
        Call<Weather> call_async = apiService.getTempByCityName();
        Log.i(LOG_TAG, "getTempByCityName:" + apiService.getTempByCityName());
        // Asíncrona
        call_async.enqueue(new Callback<Weather>() {
            @Override
            public void onResponse(Call<Weather> call, Response<Weather> response) {
                Weather weather = response.body();
                Log.i(LOG_TAG, "weather:" + weather);
                int dataListNum = 0;
                if (null != weather) {
                    Log.i(LOG_TAG, "getWeatherInfo:" + weather);

                    SimpleDateFormat sdf = new SimpleDateFormat();// 格式化时间
                    weatherTxt.append("Weather Outside:  "  + "\n\n");
                    sdf.applyPattern("yyyy-MM-dd HH:mm:ss a");// a为am/pm的标记
                    Date date = new Date();// 获取当前时间
                    weatherTxt.append(sdf.format(date)+"\n\n");

                    weatherTxt.append(weather.getWeather().get(0).getMain()+"\n\n");
                    weatherTxt.append(weather.getWeather().get(0).getDescription()+"\n\n");
                    DecimalFormat df = new DecimalFormat("#.0");
                    weatherTxt.append("Current Temperature: "+df.format(weather.getMain().getTemp()-273.15)+"\n\n");
                    weatherTxt.append("Minimum Temperature: "+df.format(weather.getMain().getTemp_min()-273.15)+"\n\n");
                    weatherTxt.append("Maximum Temperature: "+df.format(weather.getMain().getTemp_max()-273.15)+"\n\n");
                    weatherTxt.append("Humidity: "+weather.getMain().getHumidity().toString()+"\n\n");
                    weatherTxt.append("Wind Speed: "+weather.getWind().getSpeed().toString()+"\n\n");

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
                    } else {
                        weatherImage.setImageResource(R.drawable.w01d);
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

}