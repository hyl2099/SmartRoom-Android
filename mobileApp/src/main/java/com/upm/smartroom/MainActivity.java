package com.upm.smartroom;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;

import com.upm.smartroom.doorbell.DoorbellMsgActivity;
import com.upm.smartroom.About;

public class MainActivity extends AppCompatActivity {

    public final String LOG_KEY = "SmartRoom Client";

    private TextView note;
    SharedPreferences sharedPrefs;

    public void onCreate(Bundle savedInstanceState) {
        //必须调用一次父类的该方法，因为父类中做了大量的工作
        super.onCreate(savedInstanceState);
        //给当前的Activity绑定一个布局
        setContentView(R.layout.activity_main);


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