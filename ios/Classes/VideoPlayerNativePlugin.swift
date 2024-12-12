import Flutter
import UIKit
import AVKit

public class VideoPlayerNativePlugin: NSObject, FlutterPlugin {
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "video_player_native", binaryMessenger: registrar.messenger())
        let instance = VideoPlayerNativePlugin()
        requestPIPBackgroundMode()
        registrar.addMethodCallDelegate(instance, channel: channel)
        
        let factory = VideoPlayerViewFactory(registrar: registrar, channel: channel)
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
            
            // Verifica se o vídeo está disponível antes de iniciar o player.
            verifyVideoAvailability(url: url) { available in
                DispatchQueue.main.async {
                    if available {
                        self.playVideo(url: url)
                        result("Video launched successfully")
                    } else {
                        result(FlutterError(code: "VIDEO_UNAVAILABLE", message: "Video not available (404)", details: nil))
                    }
                }
            }
            
        } else {
            result(FlutterMethodNotImplemented)
        }
    }
    
    private func playVideo(url: URL) {
        // Aqui assumimos que o VideoCacheManager só cacheia se o arquivo estiver válido.
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
    
    private func verifyVideoAvailability(url: URL, completion: @escaping (Bool) -> Void) {
        var request = URLRequest(url: url)
        request.httpMethod = "HEAD"
        
        URLSession.shared.dataTask(with: request) { _, response, error in
            if let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 {
                completion(true)
            } else {
                completion(false)
            }
        }.resume()
    }
    
    static public func requestPIPBackgroundMode() {
        let session = AVAudioSession.sharedInstance()
        do {
            try session.setCategory(.playback, mode: .moviePlayback)
        } catch let error {
            print(error.localizedDescription)
        }
    }
}
