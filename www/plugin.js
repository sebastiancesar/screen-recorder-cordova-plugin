'use strict';

var exec = require('cordova/exec');

var PLUGIN_NAME = 'ScreenRecorderPlugin';

var MyCordovaPlugin = {
  start: function(opts) {
    return new Promise (function (success, reject) {
        exec(success, reject, PLUGIN_NAME, 'start', [opts]);
    });
  },
  stop: function(opts) {
    return new Promise(function (success, reject) {
        exec(success, reject, PLUGIN_NAME, 'stop', [opts]);
    });
  }
};

module.exports = MyCordovaPlugin;
