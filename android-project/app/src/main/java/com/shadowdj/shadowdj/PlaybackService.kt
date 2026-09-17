package com.shadowdj.shadowdj

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Base64
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

class PlaybackService : MediaSessionService() {

    inner class LocalBinder : Binder() {
        fun service(): PlaybackService = this@PlaybackService
    }

    private val binder = LocalBinder()

    lateinit var player: ExoPlayer
        private set

    private var mediaSession: MediaSession? = null

    private var master = 1f
    private var crossfader = 0.5f
    private var deckVolumeA = 1f
    private var deckVolumeB = 1f

    private var autoDJ = false
    private var loading = false
    private var currentGenre = "ALL"

    private val executor = Executors.newSingleThreadExecutor()

    private val queuedIds = HashSet<String>()

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this).build()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player.setAudioAttributes(
            audioAttributes,
            false
        )

        player.repeatMode = Player.REPEAT_MODE_OFF

        player.addListener(
            object : Player.Listener {

                override fun onMediaItemTransition(
                    mediaItem: MediaItem?,
                    reason: Int
                ) {
                    if (!autoDJ) return

                    val remaining =
                        player.mediaItemCount -
                        player.currentMediaItemIndex

                    if (remaining < 8) {
                        loadMoreTracks()
                    }
                }

                override fun onPlaybackStateChanged(
                    state: Int
                ) {
                    if (
                        autoDJ &&
                        state == Player.STATE_ENDED
                    ) {
                        loadMoreTracks()
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
        return if (
            intent?.action == ACTION_BIND_DJ
        ) {
            binder
        } else {
            super.onBind(intent)
        }
    }

    private fun getApiKey(): String {
        return try {
            val encoded =
                BuildConfig.AUDIUS_API_KEY_B64

            val bytes =
                Base64.decode(
                    encoded,
                    Base64.DEFAULT
                )

            String(
                bytes,
                Charsets.UTF_8
            )
        } catch (_: Exception) {
            ""
        }
    }

    fun startAutoDJ(
        genre: String = "ALL"
    ) {
        currentGenre = genre
        autoDJ = true
        loading = false

        player.clearMediaItems()
        queuedIds.clear()

        loadMoreTracks()
    }

    fun stopAutoDJ() {
        autoDJ = false
    }

    fun setGenre(
        genre: String
    ) {
        currentGenre = genre

        if (autoDJ) {
            player.clearMediaItems()
            queuedIds.clear()
            loadMoreTracks()
        }
    }

    fun playPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun next() {
        if (player.hasNextMediaItem) {
            player.seekToNext()
        }
    }

    fun previous() {
        if (player.hasPreviousMediaItem) {
            player.seekToPrevious()
        }
    }

    fun setShuffle(
        enabled: Boolean
    ) {
        player.shuffleModeEnabled = enabled
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
        master = value.coerceIn(0f, 1f)
        applyMixer()
    }

    fun setCrossfader(
        value: Float
    ) {
        crossfader = value.coerceIn(0f, 1f)
        applyMixer()
    }

    fun setDeckVolume(
        deck: String,
        value: Float
    ) {
        if (deck == "A") {
            deckVolumeA =
                value.coerceIn(0f, 1f)
        } else {
            deckVolumeB =
                value.coerceIn(0f, 1f)
        }

        applyMixer()
    }

    private fun applyMixer() {
        val volumeA =
            (1f - crossfader) *
            deckVolumeA *
            master

        val volumeB =
            crossfader *
            deckVolumeB *
            master

        player.volume =
            maxOf(
                volumeA,
                volumeB
            ).coerceIn(0f, 1f)
    }

    private fun loadMoreTracks() {

        if (loading || !autoDJ) {
            return
        }

        loading = true

        executor.execute {

            try {

                val apiKey =
                    getApiKey()

                if (apiKey.isBlank()) {
                    loading = false
                    return@execute
                }

                val genreQuery =
                    if (
                        currentGenre.isBlank() ||
                        currentGenre == "ALL"
                    ) {
                        ""
                    } else {
                        "&genre=" +
                        URLEncoder.encode(
                            currentGenre,
                            "UTF-8"
                        )
                    }

                val requestUrl =
                    "https://api.audius.co/v1/tracks/trending" +
                    "?limit=50" +
                    "&api_key=" +
                    URLEncoder.encode(
                        apiKey,
                        "UTF-8"
                    ) +
                    genreQuery

                val connection =
                    URL(requestUrl)
                        .openConnection()
                        as HttpURLConnection

                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 20000

                val response =
                    connection
                        .inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }

                connection.disconnect()

                val root =
                    org.json.JSONObject(
                        response
                    )

                val data =
                    root.optJSONArray(
                        "data"
                    ) ?: JSONArray()

                val newItems =
                    ArrayList<MediaItem>()

                for (
                    i in 0 until data.length()
                ) {

                    val track =
                        data.optJSONObject(i)
                            ?: continue

                    val id =
                        track.optString("id")

                    val title =
                        track.optString(
                            "title",
                            "SHADOW DJ"
                        )

                    val streamable =
                        track.optBoolean(
                            "isStreamable",
                            false
                        )

                    if (
                        id.isBlank() ||
                        !streamable ||
                        queuedIds.contains(id)
                    ) {
                        continue
                    }

                    val user =
                        track.optJSONObject(
                            "user"
                        )

                    val artist =
                        user?.optString(
                            "name",
                            "Audius Artist"
                        ) ?: "Audius Artist"

                    val streamUrl =
                        "https://api.audius.co/v1/tracks/" +
                        id +
                        "/stream?api_key=" +
                        URLEncoder.encode(
                            apiKey,
                            "UTF-8"
                        )

                    val item =
                        MediaItem.Builder()
                            .setUri(streamUrl)
                            .setMediaId(id)
                            .setTag(
                                "$artist — $title"
                            )
                            .build()

                    newItems.add(item)
                    queuedIds.add(id)

                    if (
                        newItems.size >= 25
                    ) {
                        break
                    }
                }

                if (newItems.isNotEmpty()) {

                    android.os.Handler(
                        mainLooper
                    ).post {

                        val wasEmpty =
                            player.mediaItemCount == 0

                        player.addMediaItems(
                            newItems
                        )

                        if (wasEmpty) {
                            player.prepare()
                            player.play()
                        }

                        loading = false
                    }

                } else {
                    loading = false
                }

            } catch (_: Exception) {
                loading = false
            }
        }
    }

    override fun onTaskRemoved(
        rootIntent: Intent?
    ) {
        player.play()
        super.onTaskRemoved(rootIntent)
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {

        executor.shutdownNow()

        mediaSession?.release()

        player.release()

        super.onDestroy()
    }

    companion object {
        const val ACTION_BIND_DJ =
            "com.shadowdj.shadowdj.BIND_DJ"
    }
}
