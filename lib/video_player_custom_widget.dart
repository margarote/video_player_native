import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class VideoPlayerWidgetAndroid extends StatefulWidget {
  final String videoUrl;
  final int durationInitial;
  final bool isFullscreen;

  const VideoPlayerWidgetAndroid(
      {super.key, required this.videoUrl, required this.isFullscreen, required this.durationInitial});

  @override
  State<VideoPlayerWidgetAndroid> createState() => _VideoPlayerWidgetAndroidState();
}

class _VideoPlayerWidgetAndroidState extends State<VideoPlayerWidgetAndroid> {
  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: widget.isFullscreen ? MediaQuery.sizeOf(context).height : 248,
      width: double.infinity,
      child: AndroidView(
        viewType: 'video_player_native_view',
        creationParams: {'url': widget.videoUrl, 'duration_initial': widget.durationInitial},
        creationParamsCodec: const StandardMessageCodec(),
        onPlatformViewCreated: (id) {},
      ),
    );
  }
}
