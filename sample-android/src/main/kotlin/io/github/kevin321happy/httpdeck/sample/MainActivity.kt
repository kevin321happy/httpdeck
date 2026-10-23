package io.github.kevin321happy.httpdeck.sample

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import io.github.kevin321happy.httpdeck.HttpDeck
import okhttp3.OkHttpClient
import okhttp3.Request

class MainActivity : Activity() {

    private lateinit var client: OkHttpClient
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val builder = OkHttpClient.Builder()
        HttpDeck.install(application, builder)
        client = builder.build()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            setBackgroundColor(Color.WHITE)
        }

        val title = TextView(this).apply {
            text = "HttpDeck Sample"
            textSize = 24f
            setTextColor(Color.parseColor("#111827"))
        }
        status = TextView(this).apply {
            text = "Tap GET, then open HttpDeck."
            textSize = 14f
            setTextColor(Color.parseColor("#4B5563"))
            setPadding(0, 24, 0, 24)
        }
        val getButton = Button(this).apply {
            text = "GET https://httpbin.org/get"
            setOnClickListener { doGet() }
        }
        val openButton = Button(this).apply {
            text = "Open HttpDeck"
            setOnClickListener { HttpDeck.show(this@MainActivity) }
        }

        root.addView(title)
        root.addView(status)
        root.addView(getButton, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        root.addView(openButton, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        setContentView(root)
    }

    private fun doGet() {
        status.text = "Requesting..."
        Thread({
            try {
                client.newCall(
                    Request.Builder().url("https://httpbin.org/get").build()
                ).execute().use { response ->
                    runOnUiThread {
                        status.text = "HTTP ${response.code}. Records: ${HttpDeck.records().size}"
                        Toast.makeText(this, "Captured HTTP ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (t: Throwable) {
                runOnUiThread {
                    status.text = "Request failed: ${t.message}"
                }
            }
        }, "httpdeck-sample").start()
    }
}