package com.shadowdj.shadowdj

import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Rational
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var playbackService: PlaybackService? = null
    private var bound = false

    private lateinit var statusText: TextView

    private val connection =
        object : ServiceConnection {

            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?
            ) {
                val binder =
                    service as? PlaybackService.LocalBinder

                playbackService =
                    binder?.service()

                bound =
                    playbackService != null

                statusText.text =
                    if (bound) {
                        "SHADOW DJ • READY"
                    } else {
                        "SHADOW DJ • OFFLINE"
                    }
            }

            override fun onServiceDisconnected(
                name: ComponentName?
            ) {
                playbackService = null
                bound = false

                statusText.text =
                    "SHADOW DJ • DISCONNECTED"
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        title = "SHADOW DJ"

        buildInterface()

        /*
         * Start the playback service after the interface
         * has been created so a service problem cannot
         * prevent the main screen from being displayed.
         */
        startPlaybackService()
    }

    private fun startPlaybackService() {

        try {

            val intent =
                Intent(
                    this,
                    PlaybackService::class.java
                )

            if (Build.VERSION.SDK_INT >= 26) {

                startForegroundService(intent)

            } else {

                startService(intent)
            }

            bindService(
                intent,
                connection,
                Context.BIND_AUTO_CREATE
            )

        } catch (e: Exception) {

            statusText.text =
                "SHADOW DJ • SERVICE ERROR"
        }
    }

    private fun buildInterface() {

        val root =
            LinearLayout(this)

        root.orientation =
            LinearLayout.VERTICAL

        root.setPadding(
            24,
            24,
            24,
            24
        )

        root.setBackgroundColor(
            0xFF101010.toInt()
        )

        val title =
            TextView(this)

        title.text =
            "SHADOW DJ"

        title.textSize = 30f
        title.gravity = Gravity.CENTER

        title.setTextColor(
            0xFFFFFFFF.toInt()
        )

        root.addView(
            title,
            matchParams()
        )

        statusText =
            TextView(this)

        statusText.text =
            "SHADOW DJ • STARTING..."

        statusText.textSize = 16f
        statusText.gravity = Gravity.CENTER

        statusText.setTextColor(
            0xFFBBBBBB.toInt()
        )

        root.addView(
            statusText,
            matchParams()
        )

        val genreLabel =
            TextView(this)

        genreLabel.text =
            "MUSIC GENRE"

        genreLabel.textSize = 15f

        genreLabel.setTextColor(
            0xFFFFFFFF.toInt()
        )

        root.addView(
            genreLabel,
            matchParams()
        )

        val genreSpinner =
            Spinner(this)

        val genres =
            arrayOf(
                "ALL",
                "Electronic",
                "House",
                "Tech House",
                "Deep House",
                "Techno",
                "Trance",
                "Drum & Bass",
                "Dubstep",
                "Disco",
                "Electro",
                "Progressive House",
                "Hardstyle",
                "Jersey Club",
                "Future Bass",
                "Tropical House"
            )

        val adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                genres
            )

        genreSpinner.adapter =
            adapter

        root.addView(
            genreSpinner,
            matchParams()
        )

        val autoDJ =
            Button(this)

        autoDJ.text =
            "START AUTO DJ"

        autoDJ.setOnClickListener {

            val genre =
                genreSpinner
                    .selectedItem
                    ?.toString()
                    ?: "ALL"

            playbackService
                ?.startAutoDJ(genre)

            statusText.text =
                "AUTO DJ • $genre"
        }

        root.addView(
            autoDJ,
            matchParams()
        )

        val stopDJ =
            Button(this)

        stopDJ.text =
            "STOP AUTO DJ"

        stopDJ.setOnClickListener {

            playbackService
                ?.stopAutoDJ()

            statusText.text =
                "AUTO DJ • STOPPED"
        }

        root.addView(
            stopDJ,
            matchParams()
        )

        val playPause =
            Button(this)

        playPause.text =
            "▶ / ⏸  PLAY / PAUSE"

        playPause.setOnClickListener {

            playbackService
                ?.playPause()
        }

        root.addView(
            playPause,
            matchParams()
        )

        val previous =
            Button(this)

        previous.text =
            "⏮  PREVIOUS"

        previous.setOnClickListener {

            playbackService
                ?.previous()
        }

        root.addView(
            previous,
            matchParams()
        )

        val next =
            Button(this)

        next.text =
            "NEXT  ⏭"

        next.setOnClickListener {

            playbackService
                ?.next()
        }

        root.addView(
            next,
            matchParams()
        )

        val shuffle =
            Button(this)

        shuffle.text =
            "SHUFFLE"

        var shuffleEnabled = false

        shuffle.setOnClickListener {

            shuffleEnabled =
                !shuffleEnabled

            playbackService
                ?.setShuffle(
                    shuffleEnabled
                )

            shuffle.text =
                if (shuffleEnabled) {
                    "SHUFFLE • ON"
                } else {
                    "SHUFFLE • OFF"
                }
        }

        root.addView(
            shuffle,
            matchParams()
        )

        val repeat =
            Button(this)

        repeat.text =
            "REPEAT"

        var repeatEnabled = false

        repeat.setOnClickListener {

            repeatEnabled =
                !repeatEnabled

            playbackService
                ?.setRepeat(
                    repeatEnabled
                )

            repeat.text =
                if (repeatEnabled) {
                    "REPEAT • ON"
                } else {
                    "REPEAT • OFF"
                }
        }

        root.addView(
            repeat,
            matchParams()
        )

        val masterLabel =
            TextView(this)

        masterLabel.text =
            "MASTER VOLUME"

        masterLabel.setTextColor(
            0xFFFFFFFF.toInt()
        )

        root.addView(
            masterLabel,
            matchParams()
        )

        val masterSeek =
            SeekBar(this)

        masterSeek.max = 100
        masterSeek.progress = 100

        masterSeek.setOnSeekBarChangeListener(
            object :
                SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    playbackService
                        ?.setMaster(
                            progress / 100f
                        )
                }

                override fun onStartTrackingTouch(
                    seekBar: SeekBar?
                ) {
                }

                override fun onStopTrackingTouch(
                    seekBar: SeekBar?
                ) {
                }
            }
        )

        root.addView(
            masterSeek,
            matchParams()
        )

        val crossLabel =
            TextView(this)

        crossLabel.text =
            "CROSSFADER   A ←──────→ B"

        crossLabel.setTextColor(
            0xFFFFFFFF.toInt()
        )

        root.addView(
            crossLabel,
            matchParams()
        )

        val crossfaderSeek =
            SeekBar(this)

        crossfaderSeek.max = 100
        crossfaderSeek.progress = 50

        crossfaderSeek.setOnSeekBarChangeListener(
            object :
                SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    playbackService
                        ?.setCrossfader(
                            progress / 100f
                        )
                }

                override fun onStartTrackingTouch(
                    seekBar: SeekBar?
                ) {
                }

                override fun onStopTrackingTouch(
                    seekBar: SeekBar?
                ) {
                }
            }
        )

        root.addView(
            crossfaderSeek,
            matchParams()
        )

        val youtube =
            Button(this)

        youtube.text =
            "YOUTUBE"

        youtube.setOnClickListener {

            openWebPlayer(
                "https://www.youtube.com"
            )
        }

        root.addView(
            youtube,
            matchParams()
        )

        val jango =
            Button(this)

        jango.text =
            "JANGO"

        jango.setOnClickListener {

            openWebPlayer(
                "https://www.jango.com"
            )
        }

        root.addView(
            jango,
            matchParams()
        )

        val pip =
            Button(this)

        pip.text =
            "YOUTUBE PICTURE-IN-PICTURE"

        pip.setOnClickListener {
            enterYoutubePip()
        }

        root.addView(
            pip,
            matchParams()
        )

        setContentView(root)
    }

    private fun matchParams():
        LinearLayout.LayoutParams {

        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {

            setMargins(
                0,
                8,
                0,
                8
            )
        }
    }

    private fun openWebPlayer(
        url: String
    ) {

        val webView =
            WebView(this)

        webView.settings.javaScriptEnabled =
            true

        webView.settings.domStorageEnabled =
            true

        webView.settings.mediaPlaybackRequiresUserGesture =
            false

        webView.settings.allowFileAccess =
            false

        webView.settings.allowContentAccess =
            false

        webView.settings.mixedContentMode =
            WebSettings.MIXED_CONTENT_NEVER_ALLOW

        webView.webViewClient =
            WebViewClient()

        webView.loadUrl(url)

        setContentView(webView)
    }

    private fun enterYoutubePip() {

        if (Build.VERSION.SDK_INT >= 26) {

            val params =
                PictureInPictureParams.Builder()
                    .setAspectRatio(
                        Rational(16, 9)
                    )
                    .build()

            enterPictureInPictureMode(
                params
            )
        }
    }

    override fun onUserLeaveHint() {

        super.onUserLeaveHint()

        if (Build.VERSION.SDK_INT >= 26) {

            val params =
                PictureInPictureParams.Builder()
                    .setAspectRatio(
                        Rational(16, 9)
                    )
                    .build()

            enterPictureInPictureMode(
                params
            )
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {

        super.onPictureInPictureModeChanged(
            isInPictureInPictureMode,
            newConfig
        )
    }

    override fun onDestroy() {

        if (bound) {

            unbindService(
                connection
            )

            bound = false
        }

        super.onDestroy()
    }
}
