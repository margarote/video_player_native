// example/lib/main.dart (ou onde você deseja usar o plugin)
import 'package:flutter/material.dart';
import 'package:video_player_native/video_player_native.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  final String videoUrl =
      'https://revivamomentos.s3.us-east-005.backblazeb2.com/revivamomentos/1732831568973303772_hls/output.m3u8'; // Substitua pela URL do seu vídeo

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Video Player Native Demo',
      home: Scaffold(
        appBar: AppBar(
          title: Text('Video Player Native'),
        ),
        body: Center(
          child: ElevatedButton(
            onPressed: () {
              VideoPlayerNative.openVideoPlayer(videoUrl);
            },
            child: Text('Reproduzir Vídeo'),
          ),
        ),
      ),
    );
  }
}
