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
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
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
