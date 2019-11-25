package net.plugins.screenrecorder;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;

import org.apache.cordova.CallbackContext;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class ScreenRecorder {
    private static final String TAG = "ScreenRecorder";
    private Activity activity;

    private int mScreenDensity;
    private MediaRecorder mMediaRecorder;
    private MediaProjectionManager mProjectionManager;
    private MediaProjectionCallback mMediaProjectionCallback;
    private ScreenRecorderService mScreenRecorderSerivce;

    private VirtualDisplay mVirtualDisplay;
    private MediaProjection mMediaProjection;

    // @TODO this values should be parametrized, maybe from the client.
    private static final int DISPLAY_WIDTH = 480;
    private static final int DISPLAY_HEIGHT = 640;

    private String fileName;
    // Callback invoked when the mediaRecorder has stopped.
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            Log.d(TAG, "Stopping MediaProjectionCallback");
        }
    }

    public ScreenRecorder (Activity activity) {
        this.activity = activity;
    }

    public void initializeDisplay(JSONObject opts, ScreenRecorderService mScreenRecorderService) throws IOException, JSONException {
        Log.d(TAG, "Initializing display");
        this.mScreenRecorderSerivce = mScreenRecorderService;
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        Log.v(TAG, "Density display " + mScreenDensity);

        // media recorder provide the native API to capture the screen.
        mMediaRecorder = new MediaRecorder();

        // handles the authorization request asking for permissions to share the screen
        mProjectionManager = (MediaProjectionManager) mScreenRecorderService.getSystemService
                (Context.MEDIA_PROJECTION_SERVICE);
        mMediaProjectionCallback = new MediaProjectionCallback();

        Context context = this.activity.getApplicationContext();
        String basePath = context.getFilesDir().getAbsolutePath();
        fileName = basePath + "/" + opts.get("sessionName") + "-capture.mp4";
        Log.v(TAG, "Filename for video " + fileName);
        // config codec, resolution, size, fps, etc
        configureCodec();

        // initialize the recorder after all the configuration
        mMediaRecorder.prepare();
    }

    /* ---------- Permissions ----------- */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Intent getIntentForScreenCapture() {
        return mProjectionManager.createScreenCaptureIntent();
    }

    // If the mMediaProjection is null, is because is the first time we ask access to the screen.
    public boolean hasPermissionForSharingScreen() {
        return mMediaProjection != null;
    }

    // The user accepted
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void permissionGranted (int resultCode, Intent data, CallbackContext callbackContext) {
        Log.v(TAG, "The user has granted permission to record the screen");
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mMediaProjection.registerCallback(mMediaProjectionCallback, null);
        startRecording();
        callbackContext.success("started");
        minimizeApp();
    }

    public void stopRecording () {
        Log.v(TAG, "Stop recording");
        if (checkWriteSettings()) {
            Settings.System.putInt(this.activity.getApplicationContext().getContentResolver(),"show_touches", 0);
        }        mScreenRecorderSerivce.stopForeground(true);// toBackground();
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        Log.v(TAG, "Recording Stopped");
        releaseResources();
        stopScreenSharing();
        Log.i(TAG, "MediaProjection Stopped");
    }

    public JSONObject getFileInfo () throws FileNotSaved {
        Log.v(TAG, "Getting file info");
        File file = new File(this.fileName);
        if (file.exists()) {
            JSONObject response = new JSONObject();
            try {
                response.put("size", file.length());
                response.put("name", fileName);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "Error creating json response");
            }
            Log.v(TAG, "Fileinfo " + response.toString());
            return response;
        } else {
            throw new FileNotSaved("FILE_NOT_SAVED");
        }
    }

    private boolean checkWriteSettings () {
        boolean permission;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permission = Settings.System.canWrite(this.activity);
        } else {
            permission = ContextCompat.checkSelfPermission(this.activity, Manifest.permission.WRITE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
        }
        return permission;
    }

    public void startRecording () {
        Log.d(TAG, "Start recording");
        if (checkWriteSettings()) {
            Settings.System.putInt(this.activity.getApplicationContext().getContentResolver(),"show_touches", 1);
        }
        // the user already granted permission
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
        minimizeApp();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private VirtualDisplay createVirtualDisplay() {
        Log.v(TAG, "Creating virtual display");
        Surface surface = mMediaRecorder.getSurface();
        Log.v(TAG, "Surface adquired " + surface.toString());

        VirtualDisplay vd = mMediaProjection.createVirtualDisplay("Playmobil Recording",
                DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface, null, null /*Handler*/);
        Log.v(TAG, vd.toString());
        return vd;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void stopScreenSharing() {
        Log.v(TAG, "Stop sharing the screen");
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
    }

    /**
     * Starting the Laucher activity simulates the minimization
     * of the app. (it doesn't minimize, just lunch the home activity)
     * */
    private void minimizeApp () {
        Log.v(TAG, "Minimizing the app");
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        activity.startActivity(startMain);
    }

    /* ----------  Configure the codec ------------ */

    private void configureCodec() {
        // @TODO this values should be parametrized, maybe from the client.
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mMediaRecorder.setVideoEncodingBitRate(512 * 1000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
        mMediaRecorder.setOutputFile(this.fileName);
        Log.v(TAG, "Media recorded configured " + mMediaRecorder.toString());
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void releaseResources() {
        Log.v(TAG, "Releasing media projection");
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    /**
     * Custom exception for error while saving the video file.
     */
    public class FileNotSaved extends Exception {
        public FileNotSaved(String message) {
            super(message);
        }
    }
}
