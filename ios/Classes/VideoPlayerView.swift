import Flutter
import UIKit
import AVFoundation

class VideoPlayerView: NSObject, FlutterPlatformView {
    private var _view: VideoPlayerUIView
    
    init(frame: CGRect, viewIdentifier viewId: Int64, arguments args: Any?, registrar: FlutterPluginRegistrar) {
        // Obter a URL do vídeo dos argumentos
        var videoURL: URL?
        if let argsDict = args as? [String: Any],
           let urlString = argsDict["url"] as? String,
           let url = URL(string: urlString) {
            videoURL = url
        }
        
        if let url = videoURL {
            _view = VideoPlayerUIView(frame: frame, url: url)
        } else {
            _view = VideoPlayerUIView(frame: frame, url: URL(string: "about:blank")!)
        }
        
        super.init()
    }
    
    func view() -> UIView {
        return _view
    }
}
