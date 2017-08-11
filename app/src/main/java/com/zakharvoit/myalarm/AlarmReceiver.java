package com.zakharvoit.myalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

public class AlarmReceiver extends BroadcastReceiver implements LocationListener, GoogleApiClient.ConnectionCallbacks {
    public static final long DEBOUNCE_TIMEOUT = 1000 * 60;
    public static final long WAIT_FOR_LOCATION_TIME = 1000 * 20;

    private GoogleApiClient mGoogleApiClient;
    private PowerManager.WakeLock mLock;
    private Context mContext;
    private AtomicBoolean mInProgress;
    private AtomicBoolean mIsConnected;
    private long mLastReceived = 0;
    private long mLocationRequestStarted = 0;
    private Location mLastLocationReceived;

    public AlarmReceiver(Context context) {
        mInProgress = new AtomicBoolean(false);
        mIsConnected = new AtomicBoolean(false);
        mContext = context;
        initGoogleApi();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        LogDebug(context, "alarm received");
        if (!acquireLock(context)) {
            LogDebug(context, "cannot acquire lock");
            return;
        }
        long currentTime = SystemClock.elapsedRealtime();
        if (!mIsConnected.get() || (currentTime - mLastReceived) < DEBOUNCE_TIMEOUT) {
            LogDebug(context, "skipping because of debounce");
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
        LogDebug(mContext, "requesting updates");
        LocationRequest locationRequest = new LocationRequest()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5 * 1000);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
        mLocationRequestStarted = SystemClock.elapsedRealtime();
    }

    @Override
    public void onLocationChanged(Location location) {
        LogDebug(mContext, "on location changed");
        if (mLastLocationReceived == null
                || location.getLatitude() != mLastLocationReceived.getLatitude()
                || location.getLongitude() != mLastLocationReceived.getLongitude()
                || SystemClock.elapsedRealtime() - mLocationRequestStarted > WAIT_FOR_LOCATION_TIME ) {
            Toast.makeText(mContext, "Location callback received!", Toast.LENGTH_SHORT).show();
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(mContext, notification);
            r.play();

            Intent intent = new Intent(mContext, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("Location", location);
            LogDebug(mContext, "save location: " + locationToString(location));
            mContext.startActivity(intent);

            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            releaseLock();
            mLastLocationReceived = location;
        } else {
            LogDebug(mContext, "Discard location because it does not differ: " + locationToString(location));
        }
    }

    private void LogDebug(Context context, String text) {
        text = new Date() + "    " + text;
        try {
            Log.d("GpsTracking", text);
            String path = context.getApplicationInfo().dataDir;
            File log = new File(path, "tracking_log.txt");
            PrintWriter writer = new PrintWriter(new FileOutputStream(log, true));
            writer.write(text + "\n");
            writer.close();
        } catch (FileNotFoundException ignored) {
        }
    }

    public static String locationToString(Location location) {
        DateFormat format = SimpleDateFormat.getTimeInstance();
        return String.format("created_at: %s lat: %.6f lon: %.6f acc: %.1f provider: %s generated_at: %s",
                format.format(new Date()),
                location.getLatitude(),
                location.getLongitude(),
                location.getAccuracy(),
                location.getProvider(),
                format.format(new Date(location.getTime())));
    }

    private void releaseLock() {
        LogDebug(mContext, "release lock");
        mLock.release();
        mInProgress.set(false);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        LogDebug(mContext, "google api connected");
        Toast.makeText(mContext, "Google api connected!", Toast.LENGTH_SHORT).show();
        mIsConnected.set(true);
    }

    @Override
    public void onConnectionSuspended(int i) {
        // should not get here
        LogDebug(mContext, "google api suspended");
    }
}
