package com.zakharvoit.myalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import java.util.Date;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone r = RingtoneManager.getRingtone(context, notification);
        r.play();

        Toast.makeText(context, "Alarm received!", Toast.LENGTH_SHORT).show();
        Log.d("!!!ALARM!!!", " date:" + new Date());
    }
}
