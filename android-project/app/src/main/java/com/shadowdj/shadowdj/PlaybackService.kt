package com.shadowdj.shadowdj

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {

    inner class LocalBinder : Binder() {
        fun service(): PlaybackService =
            this@PlaybackService
    }

    private val binder = LocalBinder()

    lateinit var player: ExoPlayer
        private set

    private var mediaSession: MediaSession? = null

    private val audiusClient =
        AudiusClient()

    private var autoDJEnabled = false
    private var autoDJGenre = "ALL"

    fun testAudius(): Boolean {
        return audiusClient.testConnection()
    }

    fun getOneAudiusStreamUrl(): String? {
        return audiusClient.getOneStreamUrl()
    }

    fun getAudiusDiagnostic(): String {
        return audiusClient.getDiagnostic()
    }

    fun playOneAudiusTrack() {

        Thread {

            val streamUrl =
                getOneAudiusStreamUrl()

            android.os.Handler(
                mainLooper
            ).post {

                if (streamUrl.isNullOrBlank()) {

                    android.widget.Toast.makeText(
                        this,
                        "AUDIUS: No stream URL found",
                        android.widget.Toast.LENGTH_LONG
                    ).show()

                    return@post
                }

                android.widget.Toast.makeText(
                    this,
                    "AUDIUS: Stream URL found",
                    android.widget.Toast.LENGTH_LONG
                ).show()

                player.setMediaItem(
                    androidx.media3.common.MediaItem.fromUri(
                        streamUrl
                    )
                )

                player.prepare()
                player.play()
            }

        }.start()
    }

    override fun onCreate() {

        super.onCreate()

        player =
            ExoPlayer.Builder(this)
                .build()

        val audioAttributes =
            AudioAttributes.Builder()
                .setUsage(
                    C.USAGE_MEDIA
                )
                .setContentType(
                    C.AUDIO_CONTENT_TYPE_MUSIC
                )
                .build()

        player.setAudioAttributes(
            audioAttributes,
            false
        )

        player.repeatMode =
            Player.REPEAT_MODE_OFF

        player.addListener(
            object : Player.Listener {

                override fun onPlaybackStateChanged(
                    playbackState: Int
                ) {

                    if (
                        playbackState ==
                        Player.STATE_ENDED &&
                        autoDJEnabled
                    ) {
                        playNextAutoDJTrack()
                    }
                }
            }
        )

        mediaSession =
            MediaSession.Builder(
                this,
                player
            ).build()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {

        return binder
    }

    fun playPause() {

        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun next() {

        if (player.hasNextMediaItem()) {

            player.seekToNext()

        } else if (autoDJEnabled) {

            playNextAutoDJTrack()
        }
    }

    fun previous() {

        if (player.hasPreviousMediaItem()) {
            player.seekToPrevious()
        }
    }

    fun setShuffle(
        enabled: Boolean
    ) {

        player.shuffleModeEnabled =
            enabled
    }

    fun setRepeat(
        enabled: Boolean
    ) {

        player.repeatMode =
            if (enabled) {
                Player.REPEAT_MODE_ALL
            } else {
                Player.REPEAT_MODE_OFF
            }
    }

    fun setMaster(
        value: Float
    ) {

        player.volume =
            value.coerceIn(
                0f,
                1f
            )
    }

    fun setCrossfader(
        value: Float
    ) {
        // Mixer control will be added later.
    }

    fun setDeckVolume(
        deck: String,
        value: Float
    ) {
        // Deck mixer control will be added later.
    }

    fun startAutoDJ(
        genre: String = "ALL"
    ) {

        autoDJGenre =
            if (genre.isBlank()) {
                "ALL"
            } else {
                genre
            }

        autoDJEnabled = true

        playNextAutoDJTrack()
    }

    fun stopAutoDJ() {

        autoDJEnabled = false

        player.pause()
    }

    private fun playNextAutoDJTrack() {

        if (!autoDJEnabled) {
            return
        }

        Thread {

            val streamUrl =
                audiusClient.getNextStreamUrl(
                    autoDJGenre
                )

            android.os.Handler(
                mainLooper
            ).post {

                if (!autoDJEnabled) {
                    return@post
                }

                if (streamUrl.isNullOrBlank()) {

                    android.widget.Toast.makeText(
                        this,
                        "AUTO DJ: No playable track found",
                        android.widget.Toast.LENGTH_LONG
                    ).show()

                    return@post
                }

                player.setMediaItem(
                    androidx.media3.common.MediaItem.fromUri(
                        streamUrl
                    )
                )

                player.prepare()
                player.play()
            }

        }.start()
    }

    fun setGenre(
        genre: String
    ) {

        autoDJGenre =
            if (genre.isBlank()) {
                "ALL"
            } else {
                genre
            }
    }

    override fun onGetSession(
        controllerInfo:
            MediaSession.ControllerInfo
    ): MediaSession? {

        return mediaSession
    }

    override fun onTaskRemoved(
        rootIntent: Intent?
    ) {

        super.onTaskRemoved(
            rootIntent
        )
    }

    override fun onDestroy() {

        mediaSession?.release()

        player.release()

        super.onDestroy()
    }

    companion object {

        const val ACTION_BIND_DJ =
            "com.shadowdj.shadowdj.BIND_DJ"
    }
}
