package com.zakharvoit.myalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

public class AlarmReceiver extends BroadcastReceiver implements LocationListener, GoogleApiClient.ConnectionCallbacks {
    public static final long DEBOUNCE_TIMEOUT = 1000 * 60;

    private GoogleApiClient mGoogleApiClient;
    private PowerManager.WakeLock mLock;
    private Context mContext;
    private AtomicBoolean mInProgress;
    private AtomicBoolean mIsConnected;
    private long mLastReceived = 0;

    public AlarmReceiver(Context context) {
        mInProgress = new AtomicBoolean(false);
        mIsConnected = new AtomicBoolean(false);
        mContext = context;
        initGoogleApi();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!acquireLock(context)) {
            return;
        }
        long currentTime = SystemClock.elapsedRealtime();
        if (!mIsConnected.get() || (currentTime - mLastReceived) < DEBOUNCE_TIMEOUT) {
            releaseLock();
            return;
        }
        mLastReceived = currentTime;
        requestLocationUpdates();
    }

    private boolean acquireLock(Context context) {
        if (!mInProgress.compareAndSet(false, true)) {
            return false;
        }
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
        mLock.acquire();
        return true;
    }

    void initGoogleApi() {
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .build();
        mGoogleApiClient.connect();
    }

    void requestLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5 * 1000);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        Toast.makeText(mContext, "Location callback received!", Toast.LENGTH_SHORT).show();
        Log.d("!!!Location!!!", "" + location);
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone r = RingtoneManager.getRingtone(mContext, notification);
        r.play();

        Intent intent = new Intent(mContext, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |  Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("Location", location);
        dumpToLogFile(mContext, location);
        mContext.startActivity(intent);

        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        releaseLock();
    }

    private void dumpToLogFile(Context context, Location location) {
        try {
            String path = context.getApplicationInfo().dataDir;
            File log = new File(path, "tracking_log.txt");
            PrintWriter writer = new PrintWriter(new FileOutputStream(log, true));
            writer.write(String.format("lat: %.6f lon: %.6f acc: %.1f time: %s\n",
                    location.getLatitude(),
                    location.getLongitude(),
                    location.getAccuracy(),
                    SimpleDateFormat.getTimeInstance().format(new Date(location.getTime()))));
            writer.close();
        } catch (FileNotFoundException ignored) {
        }
    }

    private void releaseLock() {
        mLock.release();
        mInProgress.set(false);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Toast.makeText(mContext, "Google api connected!", Toast.LENGTH_SHORT).show();
        mIsConnected.set(true);
    }

    @Override
    public void onConnectionSuspended(int i) {
        // should not get here
    }
}
