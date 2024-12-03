// example/lib/main.dart (ou onde você deseja usar o plugin)
import 'package:flutter/material.dart';
import 'package:video_player_native/video_player_native.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  final String videoUrl =
      'https://revivamomentos.s3.us-east-005.backblazeb2.com/revivamomentos/1732831568973303772_hls/output.m3u8';

  const MyApp({super.key}); // Substitua pela URL do seu vídeo

  @override
  Widget build(BuildContext context) {
    final videoPlayerNative = VideoPlayerNative();
    return MaterialApp(
      title: 'Video Player Native Demo',
      home: Column(
        children: [
          videoPlayerNative.embeddedVideoPlayer(
            videoUrl,
            0,
          ),
        ],
      ),
    );
  }
}
