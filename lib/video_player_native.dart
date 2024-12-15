import 'dart:developer';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'video_player_custom_widget.dart';

class VideoPlayerNative {
  final MethodChannel _channel = const MethodChannel('video_player_native_view');
  final MethodChannel _channelNative = const MethodChannel('video_player_native');
  bool isFullscreen = false;
  double currentPosition = 0;

  /// Método para abrir a tela nativa do player de vídeo
  Future<void> openVideoPlayer(String url, {required Map<String, String?> params}) async {
    try {
      await _channelNative.invokeMethod('openVideoPlayer', {'url': url, ...params});
    } on PlatformException catch (e) {
      debugPrint("Erro ao abrir o player nativo: ${e.message}");
    }
  }

  Future<void> seekToSeconds(double seconds) async {
    try {
      log(seconds.toString());
      await _channelNative.invokeMethod('setPlaybackPosition', {'position': seconds});
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print("Erro ao buscar para $seconds segundos: ${e.message}");
      }
    } catch (e) {
      log(e.toString());
    }
  }

  void onPlatformViewCreated({
    void Function(double duration, double currentTime)? onDuration,
    void Function(bool isFullScreen, double currentPosition)? onChangeFullScreen,
    void Function(bool isInPIP)? onChangeIsInPIP,
  }) {
    _channelNative.setMethodCallHandler((call) async {
      _handleNativeMethodCall(
        call,
        (isFullscreen, currentPosition) {
          this.isFullscreen = isFullscreen;

          if (currentPosition > 0) {
            this.currentPosition = currentPosition;
          }

          onChangeFullScreen?.call(isFullscreen, currentPosition);
        },
        onDuration,
      );
    });
  }

  Future<void> _handleNativeMethodCall(
    MethodCall call,
    void Function(bool isFullScreen, double currentPosition)? onChangeFullScreen,
    void Function(double duration, double currentPosition)? onChangePosition,
  ) async {
    switch (call.method) {
      case 'onFullscreenChange':
        Map data = call.arguments;
        bool isFullscreen = data['isFullscreen'];
        double currentPosition = double.tryParse(data['currentPosition'].toString()) ?? 0;

        onChangeFullScreen?.call(isFullscreen, currentPosition);

        if (kDebugMode) {
          print("Tela cheia: $isFullscreen");
        }
        break;
      case 'onTimeUpdate':
        // Recebe os argumentos enviados do lado nativo
        Map<String, dynamic> args = Map<String, dynamic>.from(call.arguments);

// Extrai os valores do mapa
        double currentTime = args['currentTime'] ?? 0.0;
        double duration = args['duration'] ?? 0.0;

// Chama o callback com o tempo atual
        onChangePosition?.call(
          duration,
          currentTime,
        );
      default:
        if (kDebugMode) {
          print("Método não implementado: ${call.method}");
        }
    }
  }

  void setFullScreen(bool value) {
    _channelNative.invokeMethod('set_fullscreen', {'value': value});
  }

  void setOnPipModeChanged(bool value) {
    _channelNative.invokeMethod('onPipModeChanged', {'isInPip': value});
  }

  /// Widget para embutir o player de vídeo nativo
  Widget embeddedVideoPlayer(String url, int durationInitial) {
    if (defaultTargetPlatform == TargetPlatform.android) {
      return VideoPlayerWidgetAndroid(
        videoUrl: url,
        isFullscreen: isFullscreen,
        durationInitial: durationInitial,
      );
    } else if (defaultTargetPlatform == TargetPlatform.iOS) {
      // iOS
      return SizedBox(
        height: 308,
        width: double.infinity,
        child: UiKitView(
          viewType: 'video_player_native_view',
          layoutDirection: TextDirection.ltr,
          creationParams: {
            'url': url,
          },
          creationParamsCodec: const StandardMessageCodec(),
        ),
      );
    } else {
      return const SizedBox();
    }
  }
}
