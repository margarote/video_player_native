// video_player_native/android/src/main/kotlin/com/example/video_player_native/VideoPlayerNativePlugin.kt
package com.example.video_player_native

import android.app.Activity
import android.content.Context
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

/** VideoPlayerNativePlugin */
class VideoPlayerNativePlugin : FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {
    private lateinit var channel: MethodChannel
    private lateinit var methodChannelVideo: MethodChannel
    private var activity: Activity? = null
    private lateinit var context: Context

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "video_player_native")
        channel.setMethodCallHandler(this)
        context = flutterPluginBinding.applicationContext
        methodChannelVideo = MethodChannel(flutterPluginBinding.binaryMessenger, "video_player_native_view")

        // Registrar o PlatformView
        flutterPluginBinding
            .platformViewRegistry
            .registerViewFactory("video_player_native_view", VideoPlayerViewFactory(methodChannelVideo))
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: MethodChannel.Result) {
        if (call.method == "openVideoPlayer") {
            val url = call.argument<String>("url")
            if (url != null && activity != null) {
                VideoPlayerActivity.start(activity!!, url)
                result.success(null)
            } else {
                result.error("INVALID_ARGUMENTS", "URL inválida ou atividade não disponível.", null)
            }
        } else {
            result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    // ActivityAware methods
    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        activity = null
    }
}
