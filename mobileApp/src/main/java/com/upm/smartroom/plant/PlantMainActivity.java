package com.upm.smartroom.plant;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.upm.smartroom.MobilMainActivity;
import com.upm.smartroom.R;

public class PlantMainActivity extends AppCompatActivity {

    private static final String TAG = MobilMainActivity.class.getSimpleName();
    private RecyclerView mRecyclerView;
    private PlantEntryAdapter mAdapter;
    private DatabaseReference ref;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plant_main);
        //在子Activity的onCreate中，将返回键显示出来；
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ref = FirebaseDatabase.getInstance().getReference().child("plants");

        //定义antivity_main 中的plantView
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

    public void onAddClick(View view) {
        startActivity(new Intent(this, AddItemActivity.class));
    }

    public void onDeleteClick(View view){
        //点击删除，获得ref position的key，然后删除
    }

    public void onEditClick(View view){
        //点击编辑，获得ref position的key，初始item——add页面
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
        Intent intent = new Intent(context, MobilMainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }



}