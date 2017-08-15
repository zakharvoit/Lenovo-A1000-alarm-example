package com.zakharvoit.myalarm;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
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
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class LocationTrackingService extends Service implements LocationListener, GoogleApiClient.ConnectionCallbacks {
    public static final long DEBOUNCE_TIMEOUT = 60 * 1000;
    public static final long WAIT_FOR_LOCATION_TIME = 1000 * 20;
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd-HH:mm:ss", Locale.US);
    public static final int START_TYPE = START_REDELIVER_INTENT;
    private static final int NOTIFICATION_ID = 123123;

    private GoogleApiClient mGoogleApiClient;
    private PowerManager.WakeLock mLock;
    private Context mContext;
    private AtomicBoolean mInProgress;
    private AtomicBoolean mIsConnected;
    private long mLastReceived = 0;
    private long mLocationRequestStarted = 0;
    private Location mLastLocationReceived;

    private String mLogFileName;

    @Override
    public void onCreate() {
        mLogFileName = "log" + DATE_FORMAT.format(new Date()) + ".txt";
        mInProgress = new AtomicBoolean(false);
        mIsConnected = new AtomicBoolean(false);
        mContext = getApplicationContext();
        initGoogleApi();
        Notification notification = new Notification.Builder(this)
                .setContentTitle("Location tracking")
                .setContentText("Tracking tracking tracking")
                .setOngoing(true)
                .build();
        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogDebug(mContext, "alarm received");
        if (!acquireLock(mContext)) {
            LogDebug(mContext, "cannot acquire lock");
            return START_TYPE;
        }
        long currentTime = SystemClock.elapsedRealtime();
        if (!mIsConnected.get() || (currentTime - mLastReceived) < DEBOUNCE_TIMEOUT) {
            LogDebug(mContext, "skipping because of debounce");
            releaseLock();
            return START_TYPE;
        }
        mLastReceived = currentTime;
        requestLocationUpdates();
        return START_TYPE;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    void initGoogleApi() {
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .build();
        mGoogleApiClient.connect();
    }

    private void LogDebug(Context context, String text) {
        text = DATE_FORMAT.format(new Date()) + "    " + text;
        try {
            Log.d("GpsTracking", text);
            String path = context.getApplicationInfo().dataDir;
            File log = new File(path, mLogFileName);
            PrintWriter writer = new PrintWriter(new FileOutputStream(log, true));
            writer.write(text + "\n");
            writer.close();
        } catch (FileNotFoundException ignored) {
        }
    }

    public static String locationToString(Location location) {
        return String.format(Locale.US, "created_at: %s lat: %.6f lon: %.6f acc: %.1f provider: %s generated_at: %s",
                DATE_FORMAT.format(new Date()),
                location.getLatitude(),
                location.getLongitude(),
                location.getAccuracy(),
                location.getProvider(),
                DATE_FORMAT.format(new Date(location.getTime())));
    }

    private void releaseLock() {
        LogDebug(mContext, "release lock");
        mLock.release();
        mInProgress.set(false);
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
