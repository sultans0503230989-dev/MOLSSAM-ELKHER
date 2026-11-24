package com.salawat.reminder

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView

class MainActivity : Activity() {
    companion object { @Volatile var foreground = false }

    private lateinit var web: WebView
    private var mp: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        setContentView(web)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.addJavascriptInterface(Bridge(), "Android")
        web.loadUrl("file:///android_asset/index.html")
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
    }

    override fun onDestroy() { mp?.release(); super.onDestroy() }
    override fun onResume() { super.onResume(); foreground = true }
    override fun onPause() { foreground = false; super.onPause() }

    private fun playSalawat() {
        try {
            mp?.release()
            mp = MediaPlayer.create(this, R.raw.salawat)
            mp?.start()
        } catch (e: Exception) {}
    }

    inner class Bridge {
        @JavascriptInterface fun start(mins: Double, order: String, json: String, speak: Boolean) =
            Scheduler.start(applicationContext, mins, order, json, speak)
        @JavascriptInterface fun stop() = Scheduler.stop(applicationContext)
        @JavascriptInterface fun speak() { runOnUiThread { playSalawat() } }
    }
}
