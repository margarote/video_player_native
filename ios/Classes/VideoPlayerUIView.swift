import UIKit
import AVFoundation
import AVKit
import Flutter

class VideoPlayerUIView: UIView {
    private var playerViewController: AVPlayerViewController
    private var player: AVPlayer?
    private var timeObserverToken: Any?
    private var flutterChannel: FlutterMethodChannel?
    private var isPlaybackStarted = false
    
    init(frame: CGRect, url: URL, channel: FlutterMethodChannel) {
        self.playerViewController = AVPlayerViewController()
        self.flutterChannel = channel
        super.init(frame: frame)
        
        // Configurar o manipulador para chamadas do Flutter
        setupMethodCallHandler()
        
        self.backgroundColor = UIColor.black
        
        // Configurar o player
        setupPlayer(url: url)
        
        // Adicionar o playerViewController como filho
        addSubview(playerViewController.view)
        playerViewController.view.frame = self.bounds
        playerViewController.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        
        // Habilitar PiP nativo
        playerViewController.allowsPictureInPicturePlayback = true
        playerViewController.delegate = self
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        
        // Verificar o tamanho do vídeo
        if let videoAspectRatio = player?.currentItem?.presentationSize, videoAspectRatio.width > 0 {
            let viewWidth = self.bounds.width
            let videoHeight = viewWidth / (videoAspectRatio.width / videoAspectRatio.height)
            
            // Ajustar a altura e largura mantendo o centro vertical
            playerViewController.view.frame = CGRect(
                x: 0,
                y: (self.bounds.height - videoHeight) / 2,
                width: viewWidth,
                height: videoHeight
            )
        } else {
            // Caso o tamanho do vídeo ainda não esteja disponível, usar o tamanho da view
            playerViewController.view.frame = self.bounds
        }
    }
    
    
    
    private func setupMethodCallHandler() {
        flutterChannel?.setMethodCallHandler { [weak self] call, result in
            guard let self = self else { return }
            
            switch call.method {
            case "setPlaybackPosition":
                if let args = call.arguments as? [String: Any],
                   let position = args["position"] as? Double {
                    self.seekToPosition(seconds: position)
                    self.startPlayback()
                    result(nil) // Retorna sucesso
                } else {
                    result(FlutterError(
                        code: "INVALID_ARGUMENTS",
                        message: "Posição inválida ou ausente.",
                        details: nil
                    ))
                }
            default:
                result(FlutterMethodNotImplemented)
            }
        }
    }
    
    private func seekToPosition(seconds: Double) {
        guard let player = player else { return }
        let time = CMTime(seconds: seconds, preferredTimescale: CMTimeScale(NSEC_PER_SEC))
        player.seek(to: time) { completed in
            print("Mudou para a posição \(seconds) segundos")
        }
    }
    
    private func startPlayback() {
        guard let player = player, !isPlaybackStarted else { return }
        player.play()
        isPlaybackStarted = true
        print("Reprodução iniciada")
    }
    
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) não foi implementado")
    }
    
    private func setupPlayer(url: URL) {
        
        // Alterar o esquema da URL para um esquema personalizado
       
        // Criar o AVPlayer
        let playerItem = VideoCacheManager.shared.configureCaching(for: url)
        player = AVPlayer(playerItem: playerItem)
        
        // Configurar o playerViewController
        playerViewController.player = player
        playerViewController.showsPlaybackControls = true
        playerViewController.view.backgroundColor = UIColor.black
        
        // Adicionar observador de tempo
        addPeriodicTimeObserver()
        
        // Iniciar a reprodução
        player?.pause()
    }
    
    private func addPeriodicTimeObserver() {
        guard let player = player else { return }
        
        // Define o intervalo para o observador (exemplo: a cada 1 segundo)
        let time = CMTime(seconds: 1.0, preferredTimescale: CMTimeScale(NSEC_PER_SEC))
        
        timeObserverToken = player.addPeriodicTimeObserver(forInterval: time, queue: .main) { [weak self] time in
            guard let self = self else { return }
            
            let currentTime = CMTimeGetSeconds(time)
            let duration = CMTimeGetSeconds(player.currentItem?.duration ?? CMTime.zero)
            
            // Evita valores indefinidos ou inválidos para a duração
            guard duration.isFinite else { return }
            
            // Envia o tempo atual e a duração total ao Flutter
            let arguments: [String: Any] = [
                "currentTime": currentTime,
                "duration": duration
            ]
            
            self.flutterChannel?.invokeMethod("onTimeUpdate", arguments: arguments)
        }
    }

    
    deinit {
        if let timeObserverToken = timeObserverToken {
            player?.removeTimeObserver(timeObserverToken)
            self.timeObserverToken = nil
        }
        player?.pause()
        player = nil
    }
}

extension VideoPlayerUIView: AVPlayerViewControllerDelegate {
    // Implementar métodos delegados se necessário
    func playerViewControllerWillStartPictureInPicture(_ playerViewController: AVPlayerViewController) {
        print("PiP vai iniciar")
    }
    
    func playerViewControllerDidStartPictureInPicture(_ playerViewController: AVPlayerViewController) {
        print("PiP iniciou")
    }
    
    func playerViewController(_ playerViewController: AVPlayerViewController, failedToStartPictureInPictureWithError error: Error) {
        print("Falha ao iniciar PiP: \(error.localizedDescription)")
    }
    
    func playerViewControllerWillStopPictureInPicture(_ playerViewController: AVPlayerViewController) {
        print("PiP vai parar")
    }
    
    func playerViewControllerDidStopPictureInPicture(_ playerViewController: AVPlayerViewController) {
        print("PiP parou")
    }
    
    func playerViewControllerShouldAutomaticallyDismissAtPictureInPictureStart(_ playerViewController: AVPlayerViewController) -> Bool {
        return true
    }
}
