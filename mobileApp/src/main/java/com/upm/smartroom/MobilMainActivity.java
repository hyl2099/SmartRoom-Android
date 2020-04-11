package com.upm.smartroom;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.upm.smartroom.doorbell.DoorbellMsgActivity;

import java.util.HashMap;
import java.util.Map;


public class MobilMainActivity extends AppCompatActivity {

    private static final String TAG = MobilMainActivity.class.getSimpleName();
    private Switch alarmSwitcher;
    private Switch lockSwitcher;

    // btb Firebase database variables
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mAlarmDatabaseReference;
    private FirebaseStorage mStorage;
    private ChildEventListener mChildEventAlarmListener;

    public void onCreate(Bundle savedInstanceState) {
        //必须调用一次父类的该方法，因为父类中做了大量的工作
        super.onCreate(savedInstanceState);
        //给当前的Activity绑定一个布局
        setContentView(R.layout.activity_main);
        // btb Get instance of Firebase database
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mStorage = FirebaseStorage.getInstance();
        //reference of alarm state
        mAlarmDatabaseReference = mFirebaseDatabase.getReference().child("alarmState");
        alarmSwitcher = (Switch)findViewById(R.id.alarmSwitch);
        lockSwitcher = (Switch)findViewById(R.id.lockSwitch);
        alarmSwitcher.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    Map<String, Object> childUpdates = new HashMap<>();
                    childUpdates.put(mFirebaseDatabase.getReference().child("alarmState").getKey(), "1");
                    mAlarmDatabaseReference.updateChildren(childUpdates);
                    Log.d(TAG, "Alarm is on!!!");
                }else {
                    Map<String, Object> childUpdates = new HashMap<>();
                    childUpdates.put(mFirebaseDatabase.getReference().child("alarmState").getKey(), "0");
                    mAlarmDatabaseReference.updateChildren(childUpdates);
                    Log.d(TAG, "Alarm is off!!!");
                }
            }
        });

    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.opciones_menu, menu);
        return true;
    }



    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.opcMessage:
                startActivity(new Intent(this, DoorbellMsgActivity.class));
                return true;
            case R.id.opcAbout:
                startActivity(new Intent(this, About.class));
                return true;
            default:
                Snackbar.make(
                        findViewById(android.R.id.content),
                        getString(R.string.opcMessage),
                        Snackbar.LENGTH_LONG
                ).show();
        }
        return true;
    }


}