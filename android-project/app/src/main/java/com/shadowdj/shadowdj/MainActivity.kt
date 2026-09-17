package com.shadowdj.shadowdj

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "SHADOW DJ"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            setBackgroundColor(0xFF101010.toInt())
        }

        val titleText = TextView(this).apply {
            text = "SHADOW DJ"
            textSize = 32f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
        }

        val status = TextView(this).apply {
            text = "APP STARTED SUCCESSFULLY"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(0xFFBBBBBB.toInt())
        }

        root.addView(titleText)
        root.addView(status)

        setContentView(root)
    }
}
