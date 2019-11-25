package net.plugins.screenrecorder;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Build;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.security.spec.ECField;

import static android.app.Activity.RESULT_OK;

public class ScreenRecorderPlugin extends CordovaPlugin implements ServiceConnection {

    private static final String TAG = "ScreenRecorderPlugin";
    private static final int PERMISSION_CODE = 1;

    private Activity activity;
    private ScreenRecorder screenRecorder;
    private CallbackContext askPermissionCb;
    private CordovaInterface cordova;
    private ScreenRecorderService mScreenRecorderService;
    private JSONObject opts;
    private CallbackContext callbackContext;

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        Log.d(TAG, "Initializing Screen Recorder Plugin");
        this.cordova = cordova;
        activity = cordova.getActivity();
    }

    public void lauchService () {
        Intent intento = new Intent(cordova.getActivity(), ScreenRecorderService.class);
        activity.getApplicationContext().bindService(intento, this, 0);
        // TODO the service should be started when the user actually start a session.
        activity.getApplicationContext().startService(intento);
    }

    public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        Log.v(TAG, "Executing action " + action);
        if (action.equals("start")) {
            try {
                startRecording(callbackContext, args.getJSONObject(0));
                return true;
            } catch (Exception e) {
                LOG.d(TAG, "Error while starting to record");
                e.printStackTrace();
                callbackContext.error("Error while starting");
                return false;
            }

        } else if (action.equals("stop")) {
            stopRecording(callbackContext);
            return true;
        }
        return false;
    }

    private void stopRecording(CallbackContext callbackContext) {
        Log.v(TAG, "Stop recording");
        screenRecorder.stopRecording();
        try {
            JSONObject response = screenRecorder.getFileInfo();
            callbackContext.success(response);
            Log.v(TAG, "Successfully stopped");
        } catch (ScreenRecorder.FileNotSaved fileNotSaved) {
            Log.e(TAG, "Error while stopping the recording");
            fileNotSaved.printStackTrace();
            callbackContext.error(fileNotSaved.getMessage());
        }

    }

    private void startRecording(CallbackContext callbackContext, JSONObject opts) {
        Log.d(TAG, "Strat to record");
        this.opts = opts;
        this.callbackContext = callbackContext;
        if (mScreenRecorderService != null) {
            startRecordingWithService();
        } else {
            lauchService();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void askForSharingScreenAndWait() {
        Log.v(TAG, "Asking permissions for share the screen");
        // ask to the user for permission to share the screen
        cordova.setActivityResultCallback(this);
        activity.startActivityForResult(screenRecorder.getIntentForScreenCapture(), PERMISSION_CODE);
        // exit until the user grant permission.
        return;
    }

    // A confirmation dialog was displayed to the user asking for permission to share the screen.
    // The response of the user is send it to this method.
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != PERMISSION_CODE) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(activity,
                    "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
            return;
        }
        // the permission was successfully granted
        screenRecorder.permissionGranted(resultCode, data, askPermissionCb);
    }

    public void startRecordingWithService () {
        screenRecorder = new ScreenRecorder(activity);
        mScreenRecorderService.setRecorder(screenRecorder);

        try {
            screenRecorder.initializeDisplay(opts, mScreenRecorderService);
            // For sharing the screen first we need to ask permission to the user, but only once.
            if (!screenRecorder.hasPermissionForSharingScreen()) {
                Log.v(TAG, "Asking permission to the user");
                askPermissionCb = callbackContext;
                askForSharingScreenAndWait();
                return;
            } else {
                Log.v(TAG, "The user has already granted permisssions");
                screenRecorder.startRecording();
                callbackContext.success("started");
            }
        } catch (IOException e) {
            e.printStackTrace();
            callbackContext.error("error");
        } catch (JSONException e) {
            e.printStackTrace();
            callbackContext.error("error");
        } catch (Exception e) {
            e.printStackTrace();
            callbackContext.error("error");
        }

    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service){
        Log.i(TAG, "Service Connected");
        ScreenRecorderService.LocalBinder binder = (ScreenRecorderService.LocalBinder) service;
        mScreenRecorderService = binder.getService();
        startRecordingWithService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.i(TAG, "Service disconnected");
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}