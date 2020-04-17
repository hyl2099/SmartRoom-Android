package com.upm.smartroom.plant;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.upm.smartroom.CloudVisionUtils;
import com.upm.smartroom.MobilMainActivity;
import com.upm.smartroom.R;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Map;


public class AddItemActivity extends Activity implements View.OnClickListener {
    private static final String TAG = MobilMainActivity.class.getSimpleName();

    private static final int LOCAL_IMAGE_CODE = 1;
    private String curFormatDateStr = null;

    private Bitmap bitmap;


    private EditText add_text_name;
    private EditText add_text_humidityNeed;
    private Button localImgBtn;
    private TextView showUrlTv;
    private ImageView showImageIv;
    private Button addBtn;

    private String plantName;
    private String humidityNeed;

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


        bitmap = null;
        findById();
    }

    /**
     * 初始化view
     */
    private void findById() {
        localImgBtn = (Button) this.findViewById(R.id.id_local_img_btn);
        showImageIv = (ImageView) this.findViewById(R.id.id_image_iv);
        addBtn = (Button) this.findViewById(R.id.addButton);

        add_text_name = (EditText) findViewById(R.id.add_text_name);
        add_text_humidityNeed = (EditText) findViewById(R.id.add_text_humidityNeed);

        mCloudThread = new HandlerThread("CloudThread");
        mCloudThread.start();
        mCloudHandler = new Handler(mCloudThread.getLooper());

        localImgBtn.setOnClickListener(this);
        addBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_local_img_btn:
                processLocal();
                break;
            case R.id.addButton:
                addPlantInfo();
                plantName = add_text_name.getText().toString();
                humidityNeed = add_text_humidityNeed.getText().toString();
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
     * 处理Activity跳转后返回事件
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            String url = "";
            if (requestCode == LOCAL_IMAGE_CODE) {
                Uri selectedImage = data.getData();
                try {
                    bitmap = getBitmapFromUri(this, selectedImage);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Please check if there is permission for storage!", Toast.LENGTH_SHORT).show();
                }
                showImageIv.setImageBitmap(bitmap);
                Log.d(TAG, "Selected foto");
            }
//            showUrlTv.setText(url);
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

    public byte[] bitmap2Bytes(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    public void addPlantInfo() {
        plantName = add_text_name.getText().toString();
        humidityNeed = add_text_humidityNeed.getText().toString();
        final DatabaseReference plants = mDatabase.getReference("plants").push();
        //image存入storage
        final StorageReference imageRef = mStorage.getReference().child(plants.getKey());
        Log.i(TAG, "Image add successful");

        final byte[] imageBytes = bitmap2Bytes(bitmap);

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
                        plants.child("timestamp").setValue(ServerValue.TIMESTAMP);
                        plants.child("name").setValue(plantName);
                        plants.child("humidityNeed").setValue(humidityNeed);
                        plants.child("image").setValue(downloadUrl.toString());
                        // process image annotations
//                        annotateImage(log, imageBytes);
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // clean up this entry
                Log.w(TAG, "Unable to upload image to Firebase");
                plants.removeValue();
            }
        });
        returnHome(this);
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

    public static void returnHome(Context context) {
        Intent intent = new Intent(context, PlantMainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

}