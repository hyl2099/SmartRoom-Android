package com.upm.smartroom.doorbell;

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

//本类是写的显示所有门铃图片的页面（）jin仅从firebase中获取并显示
public class DoorbellMsgActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private RecyclerView mRecyclerView;
    private DoorbellEntryAdapter mAdapter;

    private DatabaseReference ref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.doorbell_main);
        //在子Activity的onCreate中，将返回键显示出来；
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Reference for doorbell events from embedded device
        //FirebaseDatabase
        ref = FirebaseDatabase.getInstance().getReference().child("doorbellRecords");

        //定义antivity_main 中的doorbellView
        mRecyclerView = (RecyclerView) findViewById(R.id.doorbellView);
        // Show most recent items at the top
        LinearLayoutManager layoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true);
        mRecyclerView.setLayoutManager(layoutManager);

        // Initialize RecyclerView adapter
        //在使用RecyclerView时候，必须指定一个适配器Adapter和一个布局管理器LayoutManager。
        // 适配器继承RecyclerView.Adapter类，具体实现类似ListView的适配器，取决于数据信息以及展示的UI。
        // 布局管理器用于确定RecyclerView中Item的展示方式以及决定何时复用已经不可见的Item，
        // 避免重复创建以及执行高成本的findViewById()方法。
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

    AlertDialog deleteDialog;
    public void onDeleteClick(View view) {
        deleteDialog = new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setView(R.layout.item_delete)
                .setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) new OnOkClick())
                .create();
        deleteDialog.show();
    }

    class OnOkClick implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            ref.removeValue();
        }
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
