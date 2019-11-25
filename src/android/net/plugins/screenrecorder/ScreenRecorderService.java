package net.plugins.screenrecorder;


import android.app.Notification;

import android.app.PendingIntent;
import android.app.Service;

import android.content.Intent;

import android.os.Binder;
import android.os.IBinder;

import android.util.Log;

import net.plugins.MainActivity;
import net.plugins.R;


public class ScreenRecorderService extends Service {
    private String TAG = "ScreenRecorderService";
    private final IBinder mBinder = new LocalBinder();
    private ScreenRecorder recorder;

    public void setRecorder(ScreenRecorder recorder) {
        this.recorder = recorder;
    }


    public boolean isInitialized() {
        return recorder != null;
    }

    public ScreenRecorder getScreenRecorder() {
        return recorder;
    }

    public class LocalBinder extends Binder {
        ScreenRecorderService getService() {
            // Return this instance of LocalService so clients can call public methods
            return ScreenRecorderService .this;
        }
    }

    public void toForeground() {
        Log.v(TAG, "Starting a service in foreground");
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.icon)
                .setContentTitle("Hello there!")
                .setContentText("Recording...")
                .setContentIntent(pendingIntent).build();

        startForeground(1337, notification);
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Bound");
        return mBinder;
    }
}
