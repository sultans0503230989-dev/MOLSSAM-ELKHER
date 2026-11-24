package com.salawat.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder

/** إشعار دائم بشعار البرنامج، بيخلّي التذكير شغّال حتى لو التطبيق اتقفل. */
class KeepService : Service() {
    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("keep", "التذكير شغّال", NotificationManager.IMPORTANCE_LOW)
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val note = Notification.Builder(this, "keep")
            .setSmallIcon(R.drawable.ic_notif)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.logo))
            .setContentTitle("صلوات الفتوحات على نبينا سيد السادات")
            .setContentText("التذكير شغّال ﷺ")
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(open)
            .build()
        if (Build.VERSION.SDK_INT >= 34)
            startForeground(7, note, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else
            startForeground(7, note)
        return START_STICKY
    }
}
