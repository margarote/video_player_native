import Flutter
import UIKit
import AVKit

public class VideoPlayerNativePlugin: NSObject, FlutterPlugin {
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "video_player_native", binaryMessenger: registrar.messenger())
        let instance = VideoPlayerNativePlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
        
        let factory = VideoPlayerViewFactory(registrar: registrar)
        registrar.register(factory, withId: "video_player_native_view")
    }

    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        if call.method == "openVideoPlayer" {
            guard let args = call.arguments as? [String: Any],
                  let urlString = args["url"] as? String,
                  let url = URL(string: urlString) else {
                result(FlutterError(code: "INVALID_URL", message: "URL is invalid or missing", details: nil))
                return
            }
            playVideo(url: url)
            result("Video launched successfully")
        } else {
            result(FlutterMethodNotImplemented)
        }
    }

    private func playVideo(url: URL) {
        let playerItem = VideoCacheManager.shared.configureCaching(for: url)
        let player = AVPlayer(playerItem: playerItem)

        let playerViewController = AVPlayerViewController()
        playerViewController.player = player

        if let rootVC = UIApplication.shared.delegate?.window??.rootViewController {
            rootVC.present(playerViewController, animated: true) {
                player.play()
            }
        }
    }

}
