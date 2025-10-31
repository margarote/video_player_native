## 1.0.2

### Fixed
* **Screen Rotation Support**: Fixed video playback issues during device rotation
  - Added `configChanges` to AndroidManifest.xml to prevent activity recreation during rotation
  - Implemented `onConfigurationChanged` in VideoPlayerActivity to handle orientation changes gracefully
  - Player no longer pauses during rotation (only when app goes to background)
  - Fixed audio stuttering when rotating device

* **Fullscreen Mode**: Improved fullscreen behavior
  - Automatic orientation change when entering/exiting fullscreen
  - Video position is now preserved when switching between portrait and landscape
  - Fixed video resetting to beginning when entering fullscreen
  - Added 500ms delay to ensure proper position restoration after orientation change

* **Player State Management**:
  - Enhanced onPause/onResume lifecycle to detect configuration changes
  - Player state is now properly preserved during orientation changes
  - Added debug logs for better troubleshooting

### Changed
* Updated AndroidManifest.xml with comprehensive `configChanges` flags for better rotation handling
* Improved VideoPlayerView initialization with position seeking when `durationInitial > 0`

## 1.0.1

* Initial stable release

## 0.0.1

* Initial development release
