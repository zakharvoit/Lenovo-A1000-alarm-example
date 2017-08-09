package com.zakharvoit.myalarm;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 111;
    private Button mStartTrackingButton;
    private ListView mListOfLocations;
    private List<String> mLocations = new ArrayList<>();
    private ArrayAdapter<String> mLocationsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkLocationAllowed();

        mStartTrackingButton = (Button) findViewById(R.id.start_tracking_button);
        mListOfLocations = (ListView) findViewById(R.id.list_of_locations);

        mStartTrackingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestAlarm(getApplicationContext());
            }
        });

        mLocationsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mLocations);
        mListOfLocations.setAdapter(mLocationsAdapter);
    }

    static void requestAlarm(Context context) {
        Toast.makeText(context, "Tracking started", Toast.LENGTH_SHORT).show();
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        filter.setPriority(1000);
        context.registerReceiver(new AlarmReceiver(context), filter);
    }

    void checkLocationAllowed() {
        // Check for permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this, // Activity
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_FINE_LOCATION);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Location location = intent.getParcelableExtra("Location");
        mLocationsAdapter.add(
                String.format("lat: %.6f lon: %.6f time: %s",
                        location.getLatitude(),
                        location.getLongitude(),
                        SimpleDateFormat.getTimeInstance().format(new Date(location.getTime()))));
    }
}
