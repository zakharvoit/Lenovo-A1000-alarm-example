package com.zakharvoit.myalarm;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    Button mStartTrackingButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStartTrackingButton = (Button) findViewById(R.id.start_tracking_button);

        mStartTrackingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = getApplicationContext();
                Toast.makeText(context, "Tracking started", Toast.LENGTH_SHORT).show();
                IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                filter.setPriority(1000);
                context.registerReceiver(new AlarmReceiver(), filter);
            }
        });
    }
}
