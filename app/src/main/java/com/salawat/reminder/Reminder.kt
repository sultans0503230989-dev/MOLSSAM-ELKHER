package com.salawat.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.provider.Settings
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import java.io.File
import kotlin.random.Random

object Scheduler {
    const val PREFS = "sal"

    private fun pi(c: Context) = PendingIntent.getBroadcast(
        c, 0, Intent(c, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun start(c: Context, mins: Double, order: String, json: String, speak: Boolean) {
        File(c.filesDir, "d.json").writeText(json)
        c.getSharedPreferences(PREFS, 0).edit()
            .putLong("ms", (mins * 60000).toLong()).putString("order", order)
            .putInt("idx", 0).putBoolean("on", true).putBoolean("spk", speak).apply()
        schedule(c)
    }

    fun schedule(c: Context) {
        val ms = c.getSharedPreferences(PREFS, 0).getLong("ms", 0)
        if (ms <= 0) return
        val am = c.getSystemService(AlarmManager::class.java)
        val at = System.currentTimeMillis() + ms
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms())
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi(c))
        else
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi(c))
    }

    fun stop(c: Context) {
        c.getSystemService(AlarmManager::class.java).cancel(pi(c))
        c.getSharedPreferences(PREFS, 0).edit().putBoolean("on", false).apply()
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val p = c.getSharedPreferences(Scheduler.PREFS, 0)
        if (!p.getBoolean("on", false)) return
        Scheduler.schedule(c)
        if (MainActivity.foreground) return
        val arr = try { JSONArray(File(c.filesDir, "d.json").readText()) } catch (e: Exception) { return }
        val n = arr.length()
        if (n == 0) return
        val idx = p.getInt("idx", 0)
        val k = if (p.getString("order", "seq") == "rnd") Random.nextInt(n) else idx % n
        p.edit().putInt("idx", idx + 1).apply()
        val item = arr.getJSONArray(k)

        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("salawat", "تذكير الصلاة على النبي", NotificationManager.IMPORTANCE_HIGH)
        )
        val cardIntent = Intent(c, CardActivity::class.java)
            .putExtra("k", k)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val open = PendingIntent.getActivity(
            c, k + 1, cardIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val text = item.getString(1)
        val note = android.app.Notification.Builder(c, "salawat")
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(item.getString(0))
            .setContentText(text)
            .setStyle(android.app.Notification.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
            .setAutoCancel(true)
            .build()
        nm.notify(1, note)
        if (Settings.canDrawOverlays(c)) {
            try { c.startActivity(cardIntent) } catch (e: Exception) {}
        }
        if (p.getBoolean("spk", true)) play(c, goAsync())
    }

    private fun play(c: Context, pr: PendingResult) {
        var done = false
        val finish = { if (!done) { done = true; pr.finish() } }
        val mp = MediaPlayer.create(c.applicationContext, R.raw.salawat)
        if (mp == null) {
            finish()
        } else {
            mp.setOnCompletionListener { it.release(); finish() }
            mp.start()
            Handler(Looper.getMainLooper()).postDelayed({ finish() }, 20000)
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (c.getSharedPreferences(Scheduler.PREFS, 0).getBoolean("on", false)) Scheduler.schedule(c)
    }
}
