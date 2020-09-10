package com.projecteugene.flutter_android_integration

import android.content.*
import android.os.BatteryManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.util.Log
import androidx.annotation.NonNull
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugins.GeneratedPluginRegistrant


class MainActivity : FlutterActivity() {
    private val CHANNEL = "samples.flutter.dev/battery"
    private var eventChannel: EventChannel? = null
    private var clipboardManager: ClipboardManager? = null
    private var clipboardListener: ClipboardManager.OnPrimaryClipChangedListener? = null
    private val TAG = "FlutterIntegration"

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        Log.d(TAG, "configureFlutterEngine")
        GeneratedPluginRegistrant.registerWith(flutterEngine);
        Log.d(TAG, "configureFlutterEngine registerWith")

        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        Log.d(TAG, "configureFlutterEngine getClipboard")

        // Prepare channel
        eventChannel = EventChannel(flutterEngine.dartExecutor.binaryMessenger, "clipboardEvent")
        eventChannel?.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, eventSink: EventSink) {
                Log.d(TAG, "onListen")
                startListening(eventSink)
            }

            override fun onCancel(arguments: Any?) {
                Log.d(TAG, "onCancel")
                cancelListening()
            }
        })

        Log.d(TAG, "configureFlutterEngine eventChannelPrepped")


        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            if (call.method == "getBatteryLevel") {
                val batteryLevel = getBatteryLevel()

                if (batteryLevel != -1) {
                    result.success(batteryLevel)
                } else {
                    result.error("UNAVAILABLE", "Battery level not available.", null)
                }
            } else {
                result.notImplemented()
            }
        }

        Log.d(TAG, "configureFlutterEngine methodChannelPrepped")

    }

    // Listeners
    private fun startListening(emitter: EventSink) {
        Log.d(TAG, "startListening");
        if (clipboardListener == null) {
            clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
                val text = clipboardManager?.primaryClip?.getItemAt(0)?.text
                emitter.success(text)
            }
        }
        clipboardManager?.addPrimaryClipChangedListener(clipboardListener);
    }

    private fun cancelListening() {
        Log.d(TAG, "cancelListening");
        clipboardManager?.removePrimaryClipChangedListener(clipboardListener)
    }

    private fun getBatteryLevel(): Int {
        val batteryLevel: Int
        batteryLevel = if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } else {
            val intent = ContextWrapper(applicationContext).registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            intent!!.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) * 100 / intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        }

        return batteryLevel
    }
}
