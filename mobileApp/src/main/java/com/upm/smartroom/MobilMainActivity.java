package com.upm.smartroom;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.upm.smartroom.doorbell.DoorbellMsgActivity;
import com.upm.smartroom.plant.PlantMainActivity;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class MobilMainActivity extends AppCompatActivity {

    private static final String TAG = MobilMainActivity.class.getSimpleName();
    private Switch alarmSwitcher;
    private Switch lockSwitcher;
    private Switch switchSwitcher;

    private TextView temperatureDisplay;
    private TextView barometerDisplay;

    // btb Firebase database variables
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mAlarmDatabaseReference;
    private DatabaseReference mLockDatabaseReference;
    private DatabaseReference mSwitchDatabaseReference;
    private DatabaseReference mRoomTempDatabaseReference;
    private FirebaseStorage mStorage;
    private ChildEventListener mChildEventAlarmListener;
    private ChildEventListener mChildEventLockListener;
    private ChildEventListener mChildEventSwitchListener;
    private ChildEventListener mChildEventRoomTempListener;

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.0");

    private int alarmState;
    private static final int ALARMON = 1;
    private static final int ALARMOFF = 0;
    private int switchState;
    private static final int SWITCHON = 1;
    private static final int SWITCHOFF = 0;
    private int lockState;
    private static final int LOCKON = 1;
    private static final int LOCKOFF = 0;


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
        mLockDatabaseReference = mFirebaseDatabase.getReference().child("lockState");
        mSwitchDatabaseReference = mFirebaseDatabase.getReference().child("switchState");
        mRoomTempDatabaseReference = mFirebaseDatabase.getReference().child("roomTemperature");
        //
        temperatureDisplay = (TextView) findViewById(R.id.temperatureDisplay);
        temperatureDisplay.setText("-");
        barometerDisplay = (TextView) findViewById(R.id.barometerDisplay);
        barometerDisplay.setText("-");

        alarmSwitcher = (Switch)findViewById(R.id.alarmSwitch);
//        lockSwitcher = (Switch)findViewById(R.id.lockSwitch);
        switchSwitcher = (Switch)findViewById(R.id.switchSwitcher);

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
        lockSwitcher.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    Map<String, Object> childUpdates = new HashMap<>();
                    childUpdates.put(mLockDatabaseReference.getKey(), "1");
                    mLockDatabaseReference.updateChildren(childUpdates);
                    Log.d(TAG, "switch check Lock is on!!!");
                }else {
                    Map<String, Object> childUpdates = new HashMap<>();
                    childUpdates.put(mLockDatabaseReference.getKey(), "0");
                    mLockDatabaseReference.updateChildren(childUpdates);
                    Log.d(TAG, "switch check Lock is off!!!");
                }
            }
        });
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



        // btb Listener will be called when changes were performed in DB
        //监听实时数据库中alarm开关变化
        mChildEventAlarmListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                // Deserialize data from DB into our AlarmState object
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                //if alarm state in real time database changed, change alarmState in iMX7.
                String rtAlarmState = (String) Objects.requireNonNull(dataSnapshot.getValue()).toString();
                if(rtAlarmState.equals("1")){
                    alarmState = ALARMON;
                    if(!alarmSwitcher.isChecked()){
                        alarmSwitcher.setChecked(true);
                    }
                    Log.d(TAG, "Database alarm is on!!!");
                }else if(rtAlarmState.equals("0")){
                    alarmState = ALARMOFF;
                    if(alarmSwitcher.isChecked()) {
                        alarmSwitcher.setChecked(false);
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

        mChildEventLockListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                //if alarm state in real time database changed, change alarmState in iMX7.
                String rtLockState = (String) dataSnapshot.getValue();
                assert rtLockState != null;
                if(rtLockState.equals("1")){
                    lockState = LOCKON;
                    if(!lockSwitcher.isChecked()){
                        lockSwitcher.setChecked(true);
                    }
                    Log.d(TAG, "Database lock is on!!!");
                }else if(rtLockState.equals("0")){
                    lockState = LOCKOFF;
                    if(lockSwitcher.isChecked()){
                        lockSwitcher.setChecked(true);
                    }
                    Log.d(TAG, "Database lock is off!!!");
                }
            }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}
            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        };
        mLockDatabaseReference.addChildEventListener(mChildEventLockListener);
        mChildEventSwitchListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                //if alarm state in real time database changed, change alarmState in iMX7.
                String rtSwitchState = (String) dataSnapshot.getValue();
                assert rtSwitchState != null;
                if(rtSwitchState.equals("1")){
                    switchState = SWITCHON;
                    if(!switchSwitcher.isChecked()){
                        switchSwitcher.setChecked(true);
                    }
                    Log.d(TAG, "Database switch is on!!!");
                }else if(rtSwitchState.equals("0")){
                    switchState = SWITCHOFF;
                    if(switchSwitcher.isChecked()){
                        switchSwitcher.setChecked(false);
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

        mChildEventRoomTempListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                //if alarm state in real time database changed, change alarmState in iMX7.
                if(dataSnapshot.getKey().equals("mLastPressure")){
                    barometerDisplay.setText(DECIMAL_FORMAT.format((double)dataSnapshot.getValue()/10));
                }else{
                    temperatureDisplay.setText(DECIMAL_FORMAT.format(dataSnapshot.getValue()));
                }
            }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}
            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        };
        mRoomTempDatabaseReference.addChildEventListener(mChildEventRoomTempListener);

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
            case R.id.opcPlants:
                startActivity(new Intent(this, PlantMainActivity.class));
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