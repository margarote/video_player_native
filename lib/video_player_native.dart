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
  double _savedPositionBeforeOrientationChange = 0;
  double currentPlaybackSpeed = 1.0;

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
        double playbackSpeed = double.tryParse(data['playbackSpeed'].toString()) ?? 1.0;

        // Salvar a posição e velocidade atuais
        _savedPositionBeforeOrientationChange = currentPosition;
        this.currentPosition = currentPosition;
        this.currentPlaybackSpeed = playbackSpeed;

        if (kDebugMode) {
          print("Tela cheia: $isFullscreen, posição: $currentPosition, velocidade: $playbackSpeed");
        }

        // Mudar orientação do sistema quando entra/sai de fullscreen
        if (isFullscreen) {
          // Entrou em fullscreen - permitir landscape
          SystemChrome.setPreferredOrientations([
            DeviceOrientation.landscapeLeft,
            DeviceOrientation.landscapeRight,
          ]).then((_) {
            // Após mudar orientação, restaurar posição
            Future.delayed(const Duration(milliseconds: 500), () {
              seekToSeconds(currentPosition);
            });
          });
        } else {
          // Saiu de fullscreen - voltar para portrait
          SystemChrome.setPreferredOrientations([
            DeviceOrientation.portraitUp,
          ]).then((_) {
            // Após mudar orientação, restaurar posição
            Future.delayed(const Duration(milliseconds: 500), () {
              seekToSeconds(currentPosition);
            });
          });
        }

        onChangeFullScreen?.call(isFullscreen, currentPosition);
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
    // Se temos uma posição salva (após mudança de orientação), usar ela
    int positionToUse = _savedPositionBeforeOrientationChange > 0
        ? _savedPositionBeforeOrientationChange.toInt()
        : (currentPosition > 0 ? currentPosition.toInt() : durationInitial);

    if (defaultTargetPlatform == TargetPlatform.android) {
      return VideoPlayerWidgetAndroid(
        videoUrl: url,
        isFullscreen: isFullscreen,
        durationInitial: positionToUse,
        playbackSpeed: currentPlaybackSpeed,
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
