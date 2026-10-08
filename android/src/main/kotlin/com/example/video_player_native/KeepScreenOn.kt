package com.example.video_player_native

import android.view.Window
import android.view.WindowManager

/**
 * Mantém a tela acesa enquanto algum player estiver tocando.
 *
 * A flag vai na janela da Activity e não no PlayerView: como o PlayerView usa
 * SurfaceView, o Flutter hospeda a platform view num VirtualDisplay, e o
 * keepScreenOn da view não chega na janela que está na tela.
 */
internal object KeepScreenOn {
    private val playing = mutableSetOf<Any>()

    fun update(window: Window?, player: Any, isPlaying: Boolean) {
        if (isPlaying) playing.add(player) else playing.remove(player)
        if (window == null) return

        if (playing.isEmpty()) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
