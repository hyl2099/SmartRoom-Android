package com.upm.smartroom.plant;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.upm.smartroom.R;
import com.upm.smartroom.ThingsMainActivity;
import com.upm.smartroom.doorbell.DoorbellEntryAdapter;
import com.upm.smartroom.doorbell.DoorbellMsgActivity;

public class PlantMainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private RecyclerView mRecyclerView;
    private PlantEntryAdapter mAdapter;

    private DatabaseReference ref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plant_main);
        //在子Activity的onCreate中，将返回键显示出来；
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Reference for doorbell events from embedded device
        //FirebaseDatabase
        ref = FirebaseDatabase.getInstance().getReference().child("plants");

        //定义antivity_main 中的doorbellView
        mRecyclerView = (RecyclerView) findViewById(R.id.plantView);
        // Show most recent items at the top
        LinearLayoutManager layoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true);
        mRecyclerView.setLayoutManager(layoutManager);

        mAdapter = new PlantEntryAdapter(this, ref);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Initialize Firebase listeners in adapter
        mAdapter.startListening();
        // Make sure new events are visible
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        // Tear down Firebase listeners in adapter
        mAdapter.stopListening();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static void returnHome(Context context) {
        Intent intent = new Intent(context, ThingsMainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }
}