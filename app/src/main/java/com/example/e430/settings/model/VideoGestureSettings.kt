package com.example.e430.settings.model

data class VideoGestureSettings(
    val doubleTapPlayPause: Boolean = true,
    val doubleTapRewind: Boolean = true,
    val doubleTapForward: Boolean = true,
    val horizontalSwipeSeek: Boolean = true,
    val fullscreenBrightnessSwipe: Boolean = true,
    val fullscreenVolumeSwipe: Boolean = true,
    val rewindSeconds: Int = DEFAULT_SEEK_SECONDS,
    val forwardSeconds: Int = DEFAULT_SEEK_SECONDS,
) {
    companion object {
        const val DEFAULT_SEEK_SECONDS = 10
    }
}
