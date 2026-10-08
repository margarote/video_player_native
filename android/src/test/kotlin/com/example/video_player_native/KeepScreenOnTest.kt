package com.example.video_player_native

import android.view.Window
import android.view.WindowManager
import kotlin.test.AfterTest
import kotlin.test.Test
import org.mockito.Mockito

internal class KeepScreenOnTest {
  private val flag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
  private val window: Window = Mockito.mock(Window::class.java)
  private val first = Any()
  private val second = Any()

  @AfterTest
  fun releasePlayers() {
    KeepScreenOn.update(null, first, false)
    KeepScreenOn.update(null, second, false)
  }

  @Test
  fun keepsScreenOnWhilePlayingAndReleasesWhenItStops() {
    KeepScreenOn.update(window, first, true)
    Mockito.verify(window).addFlags(flag)
    Mockito.verify(window, Mockito.never()).clearFlags(flag)

    KeepScreenOn.update(window, first, false)
    Mockito.verify(window).clearFlags(flag)
  }

  @Test
  fun keepsScreenOnWhileAnotherPlayerIsStillPlaying() {
    KeepScreenOn.update(window, first, true)
    KeepScreenOn.update(window, second, true)

    KeepScreenOn.update(window, first, false)
    Mockito.verify(window, Mockito.never()).clearFlags(flag)

    KeepScreenOn.update(window, second, false)
    Mockito.verify(window).clearFlags(flag)
  }
}
