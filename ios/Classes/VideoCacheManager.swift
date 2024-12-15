import AVFoundation
import Foundation

class VideoCacheManager: NSObject, AVAssetResourceLoaderDelegate {
    static let shared = VideoCacheManager()

    private let cacheConfig: URLCache
    private let cacheExpiryInSeconds: TimeInterval = 3600 * 24 * 10  // 10 dias
    private let maxCacheSize: Int = 5 * 1024 * 1024 * 1024  // 5GB
    private let cacheMetadataKey = "VideoCacheMetadata"
    private let metadataQueue = DispatchQueue(
        label: "com.yourapp.VideoCacheMetadataQueue", attributes: .concurrent)

    override init() {
        // Configure cache com capacidade de 5GB
        cacheConfig = URLCache(
            memoryCapacity: 0, diskCapacity: maxCacheSize,
            diskPath: "VideoCache")
        super.init()

        // Limpeza automática ao inicializar
        cleanExpiredOrExcessCache()
    }

    func configureCaching(
        for url: URL
    ) -> AVPlayerItem {
        let asset = AVURLAsset(url: url)
        asset.resourceLoader.setDelegate(
            self, queue: DispatchQueue.global(qos: .background))

        // Atualizar metadados de acesso
        saveCacheMetadata(for: url)

        return AVPlayerItem(asset: asset)
    }

    private var sentURLs: Set<String> {
        get {
            // Recupera o conjunto de URLs já processadas
            return Set(
                UserDefaults.standard.array(forKey: "SentURLs") as? [String]
                    ?? [])
        }
        set {
            // Salva o conjunto atualizado no UserDefaults
            UserDefaults.standard.setValue(Array(newValue), forKey: "SentURLs")
        }
    }

    public func markURLAsSent(_ url: URL) {
        sentURLs.insert(url.absoluteString)
        print("URL marcada como enviada: \(url.absoluteString)")
    }

    public func removeSentURL(_ urlString: String) {
        sentURLs.remove(urlString)
        print("URL removida de sentURLs: \(urlString)")
    }
    
    public func isURLMarkedAsSent(_ url: URL) -> Bool {
        return sentURLs.contains(url.absoluteString)
    }

    private func saveCacheMetadata(for url: URL) {
        metadataQueue.async(flags: .barrier) {
            var metadata =
                UserDefaults.standard.dictionary(forKey: self.cacheMetadataKey)
                as? [String: TimeInterval] ?? [:]
            let currentTime = Date().timeIntervalSince1970

            metadata[url.absoluteString] = currentTime  // Armazena o timestamp de último uso
            UserDefaults.standard.setValue(
                metadata, forKey: self.cacheMetadataKey)

            // Enforce limite de cache
            self.enforceCacheLimit(metadata: metadata)
        }
    }

    private func isCacheExpired(for url: URL, metadata: [String: TimeInterval])
        -> Bool
    {
        guard let timestamp = metadata[url.absoluteString] else {
            return false
        }
        let currentTime = Date().timeIntervalSince1970
        return currentTime - timestamp > cacheExpiryInSeconds
    }

    func cleanExpiredOrExcessCache() {
        metadataQueue.async(flags: .barrier) {
            guard
                var metadata = UserDefaults.standard.dictionary(
                    forKey: self.cacheMetadataKey) as? [String: TimeInterval]
            else { return }

            // Remova itens expirados
            let expiredKeys = metadata.filter {
                self.isCacheExpired(
                    for: URL(string: $0.key)!, metadata: metadata)
            }.map { $0.key }
            for key in expiredKeys {
                if let url = URL(string: key) {
                    self.cacheConfig.removeCachedResponse(
                        for: URLRequest(url: url))
                }
                metadata.removeValue(forKey: key)
                self.removeSentURL(key)  // Remove também de sentURLs
            }

            // Atualiza metadados após limpeza
            UserDefaults.standard.setValue(
                metadata, forKey: self.cacheMetadataKey)

            // Enforce limite de cache
            self.enforceCacheLimit(metadata: metadata)
        }
    }

    private func enforceCacheLimit(metadata: [String: TimeInterval]? = nil) {
        metadataQueue.async(flags: .barrier) {
            var currentMetadata =
                metadata
                ?? (UserDefaults.standard.dictionary(
                    forKey: self.cacheMetadataKey) as? [String: TimeInterval]
                    ?? [:])

            while self.cacheConfig.currentDiskUsage > self.maxCacheSize,
                let oldest = currentMetadata.min(by: { $0.value < $1.value })
            {
                if let url = URL(string: oldest.key) {
                    self.cacheConfig.removeCachedResponse(
                        for: URLRequest(url: url))
                }
                currentMetadata.removeValue(forKey: oldest.key)
                self.removeSentURL(oldest.key) // Remove do sentURLs
            }

            UserDefaults.standard.setValue(
                currentMetadata, forKey: self.cacheMetadataKey)
        }
    }

    private func removeOldestCacheItem() {
        // Esta função foi substituída pela lógica em enforceCacheLimit
    }

    func clearCache() {
        cacheConfig.removeAllCachedResponses()
        UserDefaults.standard.removeObject(forKey: cacheMetadataKey)

        // Remover todas as URLs de sentURLs
        sentURLs.removeAll()
        print("Cache e sentURLs foram limpos.")
    }

    private var sentFragments: Set<String> {
        get {
            return Set(
                UserDefaults.standard.array(forKey: "SentFragments")
                    as? [String] ?? [])
        }
        set {
            UserDefaults.standard.setValue(
                Array(newValue), forKey: "SentFragments")
        }
    }

    private func registerFragmentSent(url: URL) {
        let key = url.absoluteString
        sentFragments.insert(key)
    }

    // MARK: - AVAssetResourceLoaderDelegate

    func resourceLoader(
        _ resourceLoader: AVAssetResourceLoader,
        shouldWaitForLoadingOfRequestedResource loadingRequest:
            AVAssetResourceLoadingRequest
    ) -> Bool {
        guard let url = loadingRequest.request.url else {
            loadingRequest.finishLoading(
                with: NSError(domain: "Invalid URL", code: -1, userInfo: nil))
            return false
        }

        // Verificar se já foi enviado
        if sentFragments.contains(url.absoluteString) {
            loadingRequest.finishLoading()
            return true
        }

        let cacheRequest = URLRequest(url: url)
        if let cachedResponse = cacheConfig.cachedResponse(for: cacheRequest) {
            loadingRequest.dataRequest?.respond(with: cachedResponse.data)
            loadingRequest.finishLoading()
            return true
        }

        // Se não estiver no cache, baixe os dados e adicione ao cache
        let sessionConfig = URLSessionConfiguration.default
        let session = URLSession(configuration: sessionConfig)
        let task = session.dataTask(with: cacheRequest) {
            data, response, error in
            if let error = error {
                loadingRequest.finishLoading(with: error)
                return
            }

            if let data = data, let response = response {
                let cachedResponse = CachedURLResponse(
                    response: response, data: data)
                self.cacheConfig.storeCachedResponse(
                    cachedResponse, for: cacheRequest)
                loadingRequest.dataRequest?.respond(with: data)
                // Salvar como enviado
                self.registerFragmentSent(url: url)
                loadingRequest.finishLoading()
            } else {
                loadingRequest.finishLoading(
                    with: NSError(domain: "No Data", code: -1, userInfo: nil))
            }
        }
        task.resume()

        return true
    }
}
