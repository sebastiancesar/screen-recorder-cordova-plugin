## Screen Recorder Cordova plugin

This plugin lets you record what's on the device screen, through the native Android API. 

### Cordova plugin

The code is encapsulated into a [Cordova plugin](https://cordova.apache.org/docs/es/latest/guide/hybrid/plugins/), and is ready to be used in a Ionic project (tested on v1)

In order to get access to the screen recording api, you need to ask for permissions to the user. That probably will change, a lot as usual with Android, so, that part of the code need to be updated constantly.

Once the user grants access, the plugin, launch a background service and start to record what's on the surface until the plugin receives a "stop" command.
Then, the video file is placed in the internal memory of the device

```java
Context context = this.activity.getApplicationContext();
        String basePath = context.getFilesDir().getAbsolutePath();
        fileName = basePath + "/" + opts.get("sessionName") + "-capture.mp4";
```

### How to use it

The plugin has two actions:

```javascript
ScreenRecorderPlugin.start(opts)
    .then(function (response) {
        logger.debug('start > response ', response)
    });
```

The @response should contain a string "success" in case of everything went ok or an error description in case of problems.


```javascript
ScreenRecorderPlugin.stop()
    .then(function (response) {
        logger.debug('stop > response ', response);       
        sessionRecordedFilename = response.name.substr
    });
```

The response now contain the filename of the saved file.
