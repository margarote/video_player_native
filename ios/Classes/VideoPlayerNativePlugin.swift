import Flutter
import UIKit
import AVKit

@available(iOS 13.0, *)
public class VideoPlayerNativePlugin: NSObject, FlutterPlugin, AVPlayerViewControllerDelegate {
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "video_player_native", binaryMessenger: registrar.messenger())
        let instance = VideoPlayerNativePlugin()
        requestPIPBackgroundMode()
        registrar.addMethodCallDelegate(instance, channel: channel)
        
        let factory = VideoPlayerViewFactory(registrar: registrar, channel: channel)
        registrar.register(factory, withId: "video_player_native_view")
    }
    
    private var apiBaseURL: String {
        return Bundle.main.object(forInfoDictionaryKey: "APIBaseURL") as? String ?? "https://revivamomentos.com"
    }
    
    private var momentaryId: String = ""
    private var userId: String = ""
    private var fileId: String = ""
    private var videoURL: String = ""
    private var intervalTimer: Timer?
    private var url: URL?
    
    // Variável para acumular bytes baixados
    private var totalBytesDownloaded: Int64 = 0
    private var totalBytesConsumed: Int64 = 0
    
    override init() {
        super.init()
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        if call.method == "openVideoPlayer" {
            guard let args = call.arguments as? [String: Any],
                  let urlString = args["url"] as? String,
                  let url = URL(string: urlString),
                  let momentaryId = args["momentaryId"] as? String,
                  let userId = args["userId"] as? String,
                  let fileId = args["fileId"] as? String else {
                result(FlutterError(code: "INVALID_ARGUMENTS", message: "Missing or invalid arguments", details: nil))
                return
            }
            
            self.momentaryId = momentaryId
            self.userId = userId
            self.fileId = fileId
            self.videoURL = urlString
            self.url = url
            
            playHLSVideo(url: url)
            result("Video launched successfully")
        } else {
            result(FlutterMethodNotImplemented)
        }
    }
    
    private func playHLSVideo(url: URL) {
        //        let playerItem = AVPlayerItem(url: url)
        let playerItem = VideoCacheManager.shared.configureCaching(for: url)
        let player = AVPlayer(playerItem: playerItem)
        
        self.observeHLSProgress(player: player, videoURL: url)
        
        let playerViewController = AVPlayerViewController()
        playerViewController.player = player
        playerViewController.delegate = self // Para monitorar fechamento do player
        
        // Adicionar observador para as novas entradas de log de acesso
        NotificationCenter.default.addObserver(self, selector: #selector(handleAccessLogEntry(_:)), name: .AVPlayerItemNewAccessLogEntry, object: playerItem)
        
        // Iniciar o envio periódico de dados
        startPeriodicDataSend()
        
        DispatchQueue.main.async {
            if let rootVC = UIApplication.shared.delegate?.window??.rootViewController {
                rootVC.present(playerViewController, animated: true) {
                    player.play()
                }
            }
        }
    }
    
    @objc private func handleAccessLogEntry(_ notification: Notification) {
        guard let playerItem = notification.object as? AVPlayerItem,
              let log = playerItem.accessLog(),
              let lastEvent = log.events.last else {
            return
        }
        
        //        let downloadedBytes = lastEvent.numberOfBytesTransferred
        //        self.totalBytesDownloaded += Int64(downloadedBytes)
        //        print("Bytes downloaded: \(downloadedBytes)")
    }
    
    private func startPeriodicDataSend() {
        //        DispatchQueue.main.async {
        //            self.intervalTimer = Timer.scheduledTimer(withTimeInterval: 5.0, repeats: true) { [weak self] _ in
        //                self?.sendDataUsage()
        //            }
        //        }
    }
    
    private var isVideoMarkedAsSent = false
    
    // Monitorar o progresso do vídeo
    private func observeHLSProgress(player: AVPlayer, videoURL: URL) {
        guard let playerItem = player.currentItem else { return }
        
        player.addPeriodicTimeObserver(forInterval: CMTime(seconds: 1, preferredTimescale: 1), queue: DispatchQueue.main) { [weak self] currentTime in
            guard let self = self else { return }
            
            self.checkHLSProgress(playerItem: playerItem)
        }
    }
    
    // Calcular progresso e bytes consumidos
    private func checkHLSProgress(playerItem: AVPlayerItem) {
        guard let log = playerItem.accessLog(), let lastEvent = log.events.last else { return }
        
        let bytesConsumed = lastEvent.numberOfBytesTransferred
        
        self.sendDataUsage(newValue: bytesConsumed)
        
        let totalDuration = CMTimeGetSeconds(playerItem.duration)
        let currentPlaybackTime = CMTimeGetSeconds(playerItem.currentTime())
        
        guard totalDuration > 0 else { return }
        
        // Calcular porcentagem de progresso
        let progressPercentage = (currentPlaybackTime / totalDuration) * 100
        print("Progress: \(progressPercentage)% - Bytes Consumed: \(totalBytesConsumed)")
        
        if progressPercentage >= 70, !isVideoMarkedAsSent {
            print("Playback has reached 70% of the video.")
            
            guard let videoURL = self.url else { return }
            
            // Marca o vídeo como enviado
            VideoCacheManager.shared.markURLAsSent(videoURL)
            isVideoMarkedAsSent = true
        }
    }
    
    
    private func stopPeriodicDataSend() {
        DispatchQueue.main.async {
            self.intervalTimer?.invalidate()
            self.intervalTimer = nil
        }
    }
    
    private func sendDataUsage(newValue: Int64) {
        guard let url = url else { return }
        
        // Se não houve incremento, não faz nada
        if newValue <= totalBytesConsumed {
            return
        }
        
        // Calcula somente o delta (bytes recém-baixados)
        let newlyDownloadedBytes = newValue - totalBytesConsumed
        
        // Atualiza o total de bytes consumidos
        totalBytesConsumed = newValue
        
        // Se a URL já foi marcada como “enviada”, não fazer nada
        if VideoCacheManager.shared.isURLMarkedAsSent(url) {
            return
        }
        
        // Se não houve bytes a enviar, sai
        if newlyDownloadedBytes <= 0 {
            return
        }
        
        // Monta e faz a request com 'newlyDownloadedBytes' ao invés do total
        guard let apiURL = URL(string: "\(apiBaseURL)/revivamomentos/bandwidth/create/usage") else {
            print("URL da API inválida.")
            return
        }
        
        var request = URLRequest(url: apiURL)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let body: [String: Any] = [
            "momentary_id": momentaryId,
            "file_id": fileId,
            "user_id": userId,
            "url": videoURL,
            "type_file": "video",
            // Aqui vai somente o delta recém-baixado
            "bytes_downloaded": newlyDownloadedBytes
        ]
        
        do {
            request.httpBody = try JSONSerialization.data(withJSONObject: body, options: [])
        } catch {
            print("Falha ao serializar JSON: \(error.localizedDescription)")
            return
        }
        
        let task = URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error {
                print("Erro ao enviar dados: \(error.localizedDescription)")
                return
            }
            if let httpResponse = response as? HTTPURLResponse {
                print("Resposta da API: \(httpResponse.statusCode)")
            }
        }
        task.resume()
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
