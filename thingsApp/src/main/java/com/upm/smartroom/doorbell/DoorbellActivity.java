package com.upm.smartroom.doorbell;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;
import com.upm.smartroom.board.BoardDefaults;
import com.upm.smartroom.CloudVisionUtils;
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
import com.upm.smartroom.postData.PictureRESTAPIService;
import com.upm.smartroom.postData.SpringPicture;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Doorbell activity that capture a picture from an Android Things
 * camera on a button press and post it to Firebase and Google Cloud
 * Vision API.
 */
public class DoorbellActivity extends Activity {
    private static final String TAG = DoorbellActivity.class.getSimpleName();

    private FirebaseDatabase mDatabase;
    private FirebaseStorage mStorage;
    private DoorbellCamera mCamera;

    //API
    private static final String SPRING_API_BASE_URL = "http://192.168.1.55:8080";
    private PictureRESTAPIService pictureRESTAPIService;

    /**
     * Driver for the doorbell button;
     */
    private ButtonInputDriver mButtonInputDriver;
    //LED GPIO06_IO14
    private Gpio led;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Doorbell Activity created.");

        // We need permission to access the camera
        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // A problem occurred auto-granting the permission
            Log.e(TAG, "No permission");
            return;
        }
        // Firebase
        mDatabase = FirebaseDatabase.getInstance();
        mStorage = FirebaseStorage.getInstance();

        //Spring API
        Retrofit retrofitSpring = new Retrofit.Builder()
                .baseUrl(SPRING_API_BASE_URL).addConverterFactory(GsonConverterFactory.create())
                .build();
        pictureRESTAPIService = retrofitSpring.create(PictureRESTAPIService.class);

        // Creates new handlers and associated threads for camera and networking operations.
        mCameraThread = new HandlerThread("CameraBackground");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());

        mCloudThread = new HandlerThread("CloudThread");
        mCloudThread.start();
        mCloudHandler = new Handler(mCloudThread.getLooper());

        // Initialize the doorbell button driver
        initPIO();

        // Camera code is complicated, so we've shoved it all in this closet class for you.
        mCamera = DoorbellCamera.getInstance();
        mCamera.initializeCamera(this, mCameraHandler, mOnImageAvailableListener);
    }

    private void initPIO() {
        PeripheralManager pio = PeripheralManager.getInstance();
        try {
            //LED 灯，接在GPIO6_IO14口。
            led = pio.openGpio(BoardDefaults.getGPIOForLED());
            //点亮LED灯
            led.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);

            //初始化按钮
            mButtonInputDriver = new ButtonInputDriver(
                    BoardDefaults.getGPIOForCameraButton(),
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
            //将doorbell存在firebase 实时数据库
            final DatabaseReference doorbell = mDatabase.getReference("doorbell").push();
            //image存入storage
            final StorageReference imageRef = mStorage.getReference("doorbell").child(doorbell.getKey());

            // upload image to firebase storage
            //一，将图片上传到firebase
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
                            doorbell.child("timestamp").setValue(ServerValue.TIMESTAMP);
                            doorbell.child("image").setValue(downloadUrl.toString());
                            // process image annotations
                            annotateImage(doorbell, imageBytes);
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // clean up this entry
                    Log.w(TAG, "Unable to upload image to Firebase");
                    doorbell.removeValue();
                }
            });

            // upload image to my Spring server
            //二，将图片上传到本地Spring后端
            //imageBytes 图片byte[]文件
            //owner
            RequestBody owner = RequestBody.create(null, "owner");
            //remark
            RequestBody remark = RequestBody.create(null, "remark");
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

    public static MultipartBody.Part toMultiPartFile(byte[] byteArray) {
        RequestBody reqFile = RequestBody.create(MediaType.parse("image/jpeg"), byteArray);
        return MultipartBody.Part.createFormData("file",
                "doorbell", // filename, this is optional
                reqFile);
    }

}
