package com.upm.smartroom.plant;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.upm.smartroom.MobilMainActivity;
import com.upm.smartroom.R;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Map;


public class AddItemActivity extends Activity implements View.OnClickListener {
    private static final String TAG = MobilMainActivity.class.getSimpleName();

    private static final int LOCAL_IMAGE_CODE = 1;
    private static final int CAMERA_IMAGE_CODE = 2;
    private static final String IMAGE_TYPE = "image/*";
    private String rootUrl = null;
    private String curFormatDateStr = null;

    private Button localImgBtn, cameraImgBtn;
    private TextView showUrlTv;
    private ImageView showImageIv;

    private FirebaseDatabase mDatabase;
    private FirebaseStorage mStorage;

    /**
     * A {@link Handler} for running Cloud tasks in the background.
     */
    private Handler mCloudHandler;

    /**
     * An additional thread for running Cloud tasks that shouldn't block the UI.
     */
    private HandlerThread mCloudThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.item_add);

        mDatabase = FirebaseDatabase.getInstance();
        mStorage = FirebaseStorage.getInstance();

        final DatabaseReference description = mDatabase.getReference("logs").push();
        final StorageReference imageRef = mStorage.getReference().child(description.getKey());

        findById();
        initData();
    }

    /**
     * 初始化view
     */
    private void findById() {
        localImgBtn = (Button) this.findViewById(R.id.id_local_img_btn);
        cameraImgBtn = (Button) this.findViewById(R.id.id_camera_img_btn);
        showUrlTv = (TextView) this.findViewById(R.id.id_show_url_tv);
        showImageIv = (ImageView) this.findViewById(R.id.id_image_iv);

        mCloudThread = new HandlerThread("CloudThread");
        mCloudThread.start();
        mCloudHandler = new Handler(mCloudThread.getLooper());

        localImgBtn.setOnClickListener(this);
        cameraImgBtn.setOnClickListener(this);
    }

    /**
     * 初始化相关data
     */
    private void initData() {
        //获取路径
        rootUrl = Environment.getExternalStorageDirectory().getPath();
//        rootUrl = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
//        rootUrl = getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString();
        //getExternalFilesDir(null);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_local_img_btn:
                processLocal();
                break;
            case R.id.id_camera_img_btn:
                processCamera();
                break;
        }
    }

    /**
     * 处理本地图片btn事件
     */
    private void processLocal() {
        Intent i = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, LOCAL_IMAGE_CODE);
    }

    /**
     * 处理camera图片btn事件
     */
    private void processCamera() {
        curFormatDateStr = HelpUtil.getDateFormatString(Calendar.getInstance()
                .getTime());
        String fileName = "IMG_" + curFormatDateStr + ".png";
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                Uri.fromFile(new File(rootUrl, fileName)));
        intent.putExtra("fileName", fileName);
        startActivityForResult(intent, CAMERA_IMAGE_CODE);
    }

    /**
     * 处理Activity跳转后返回事件
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            String url = "";
            Bitmap bitmap = null;
            if (requestCode == LOCAL_IMAGE_CODE) {
            Uri selectedImage = data.getData();
            try {
                bitmap = getBitmapFromUri(this, selectedImage);
            } catch (IOException e) {
                e.printStackTrace();
            }
            showImageIv.setImageBitmap(bitmap);
            Log.d(TAG,"Selected foto");
            } else if (requestCode == CAMERA_IMAGE_CODE) {
                url = rootUrl + "/" + "IMG_" + curFormatDateStr + ".png";
                bitmap = HelpUtil.getBitmapByUrl(url);
                showImageIv.setImageBitmap(HelpUtil.createRotateBitmap(bitmap));
            }
            showUrlTv.setText(url);
        } else {
            Toast.makeText(this, "No photo added!", Toast.LENGTH_SHORT).show();
        }
    }


    public Bitmap getBitmapFromUri(Context context, Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor = null;
        FileDescriptor fileDescriptor = null;
        Bitmap bitmap = null;
        try {
            parcelFileDescriptor = context.getContentResolver().openFileDescriptor(uri, "r");
            if (parcelFileDescriptor != null && parcelFileDescriptor.getFileDescriptor() != null) {
                fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                //转换uri为bitmap类型
                bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {

                if (parcelFileDescriptor != null) {
                    parcelFileDescriptor.close();
                }
            }
            return bitmap;
        }



//
//    class OnAddClick implements DialogInterface.OnClickListener {
//        @Override
//        public void onClick(DialogInterface dialog, int which) {
//            final DatabaseReference log = mDatabase.getReference("plants").push();
//            //image存入storage
//            final StorageReference imageRef = mStorage.getReference().child(log.getKey());
//            Log.i(TAG, "Image add successful");
//
//
//
////            imageBytes = bitmap;
////
////            // upload image to storage
////            UploadTask task = imageRef.putBytes(imageBytes);
////            task.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
////                @Override
////                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
////                    //获取下载的url，然后存入实时数据库，供客户端访问
////                    Task<Uri> firebaseUri = taskSnapshot.getStorage().getDownloadUrl();
////                    firebaseUri.addOnSuccessListener(new OnSuccessListener<Uri>() {
////                        @Override
////                        public void onSuccess(Uri uri) {
////                            Uri downloadUrl;
////                            downloadUrl = uri;
////                            // mark image in the database
////                            Log.i(TAG, "Image upload successful");
////                            log.child("timestamp").setValue(ServerValue.TIMESTAMP);
////                            log.child("image").setValue(downloadUrl.toString());
////                            // process image annotations
////                            annotateImage(log, imageBytes);
////                        }
////                    });
////                }
////            }).addOnFailureListener(new OnFailureListener() {
////                @Override
////                public void onFailure(@NonNull Exception e) {
////                    // clean up this entry
////                    Log.w(TAG, "Unable to upload image to Firebase");
////                    log.removeValue();
////                }
////            });
//        }
//    }


    /**
     * Process image contents with Cloud Vision.
     */
//    private void annotateImage(final DatabaseReference ref, final byte[] imageBytes) {
//        mCloudHandler.post(new Runnable() {
//            @Override
//            public void run() {
//                Log.d(TAG, "sending image to cloud vision");
//                // annotate image by uploading to Cloud Vision API
//                try {
//                    Map<String, Float> annotations = CloudVisionUtils.annotateImage(imageBytes);
//                    Log.d(TAG, "cloud vision annotations:" + annotations);
//                    if (annotations != null) {
//                        ref.child("annotations").setValue(annotations);
//                    }
//                } catch (IOException e) {
//                    Log.e(TAG, "Cloud Vison API error: ", e);
//                }
//            }
//        });
//    }


}