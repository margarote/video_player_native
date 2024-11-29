// lib/video_player_native.dart
import 'dart:async';
import 'package:flutter/services.dart';

class VideoPlayerNative {
  static const MethodChannel _channel = MethodChannel('video_player_native');

  /// Abre a tela nativa de reprodução de vídeo com a URL fornecida.
  static Future<void> openVideoPlayer(String url) async {
    try {
      await _channel.invokeMethod('openVideoPlayer', {'url': url});
    } on PlatformException catch (e) {
      print("Erro ao abrir o player de vídeo: ${e.message}");
      // Opcional: relançar o erro ou tratar conforme necessário
      throw e;
    }
  }
}
