package com.upm.smartroom.doorbell;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.upm.smartroom.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DoorbellMsgActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private RecyclerView mRecyclerView;
    private DoorbellEntryAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.doorbell_main);

        // Reference for doorbell events from embedded device
        //FirebaseDatabase
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("logs");

        //定义antivity_main 中的doorbellView
        mRecyclerView = (RecyclerView) findViewById(R.id.doorbellView);
        // Show most recent items at the top
        LinearLayoutManager layoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true);
        mRecyclerView.setLayoutManager(layoutManager);

        // Initialize RecyclerView adapter
        mAdapter = new DoorbellEntryAdapter(this, ref);
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

}
