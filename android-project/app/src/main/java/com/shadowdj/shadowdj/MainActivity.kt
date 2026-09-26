package com.shadowdj.shadowdj

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.view.ViewGroup
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

        startPlaybackService()
    }

    private fun startPlaybackService() {

        try {

            val intent =
                Intent(
                    this,
                    PlaybackService::class.java
                )

            startService(intent)

            bindService(
                intent,
                connection,
                Context.BIND_AUTO_CREATE
            )

        } catch (_: Exception) {

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
                "Techno",
                "House",
                "EDM",
                "Rock",
                "R&B / Hip-Hop",
                "Country"
            )

        genreSpinner.adapter =
            android.widget.ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                genres
            )

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
                    ?: "Techno"

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
            playbackService?.playPause()
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
            playbackService?.previous()
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
            playbackService?.next()
        }

        root.addView(
            next,
            matchParams()
        )

        val shuffle =
            Button(this)

        shuffle.text =
            "SHUFFLE • OFF"

        var shuffleEnabled = false

        shuffle.setOnClickListener {

            shuffleEnabled =
                !shuffleEnabled

            playbackService
                ?.setShuffle(shuffleEnabled)

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
            "REPEAT • OFF"

        var repeatEnabled = false

        repeat.setOnClickListener {

            repeatEnabled =
                !repeatEnabled

            playbackService
                ?.setRepeat(repeatEnabled)

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

        val master =
            SeekBar(this)

        master.max = 100
        master.progress = 100

        master.setOnSeekBarChangeListener(
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
            master,
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

        val crossfader =
            SeekBar(this)

        crossfader.max = 100
        crossfader.progress = 50

        crossfader.setOnSeekBarChangeListener(
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
            crossfader,
            matchParams()
        )

        val testAudius =
            Button(this)

        testAudius.text =
            "TEST AUDIUS"

        testAudius.setOnClickListener {

            if (playbackService == null) {

                statusText.text =
                    "AUDIUS • SERVICE NOT CONNECTED"

                android.widget.Toast.makeText(
                    this,
                    "SHADOW DJ service is not connected",
                    android.widget.Toast.LENGTH_LONG
                ).show()

                return@setOnClickListener
            }

            statusText.text =
                "AUDIUS • STARTING..."

            playbackService
                ?.playOneAudiusTrack()

            statusText.text =
                "AUDIUS • PLAYING"
        }

        root.addView(
            testAudius,
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
