import UIKit
import AVFoundation
import AVKit

class VideoPlayerUIView: UIView {
    private var playerViewController: AVPlayerViewController
    private var player: AVPlayer?
    
    init(frame: CGRect, url: URL) {
        self.playerViewController = AVPlayerViewController()
        super.init(frame: frame)
        
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
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) não foi implementado")
    }

    private func setupPlayer(url: URL) {
        // Criar o AVPlayer
        let playerItem = VideoCacheManager.shared.configureCaching(for: url)
        player = AVPlayer(playerItem: playerItem)
        
        // Configurar o playerViewController
        playerViewController.player = player
        playerViewController.showsPlaybackControls = true
        playerViewController.view.backgroundColor = UIColor.black
        
        // Iniciar a reprodução
        player?.play()
    }
    
    deinit {
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
