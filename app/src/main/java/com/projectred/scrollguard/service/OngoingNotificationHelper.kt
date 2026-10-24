package com.projectred.scrollguard.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.projectred.scrollguard.R
import com.projectred.scrollguard.data.PreferenceManager
import com.projectred.scrollguard.data.ScheduleManager
import com.projectred.scrollguard.data.StatsManager
import com.projectred.scrollguard.ui.MainActivity

/**
 * Pembantu untuk membuat dan memperbarui notifikasi status permanen (Ongoing Notification).
 * Menampilkan statistik harian (video dicegah & waktu terselamatkan)
 * serta tombol interaktif (On/Off, Jeda 15 Mnt, Lanjutkan) langsung pada panel notifikasi HP.
 */
object OngoingNotificationHelper {

    const val CHANNEL_ID = "scrollguard_ongoing_status"
    const val NOTIFICATION_ID = 2471

    fun initChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Status Perlindungan Berjalan",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Menampilkan statistik harian dan kontrol cepat pemblokiran"
                setShowBadge(false)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun update(context: Context) {
        val pref = PreferenceManager(context)
        if (!pref.isPersistentNotificationEnabled) {
            cancel(context)
            return
        }

        initChannel(context)

        val stats = StatsManager(context)
        val schedule = ScheduleManager(context)

        val isMasterOn = pref.isMasterEnabled
        val isSnooze = pref.isSnoozeActive()
        val isScheduleActive = schedule.isWithinActiveSchedule()

        val totalBlocks = stats.getTodayTotalBlocks()
        val minutesSaved = stats.getTodayTimeSavedMinutes()

        // Teks statistik
        val statsText = if (totalBlocks > 0) {
            "Hari ini: $totalBlocks video dicegah (~$minutesSaved menit terselamatkan)"
        } else {
            "Hari ini: Belum ada video pendek dicegah"
        }

        // Tentukan Judul, Ikon, dan Aksi Cepat
        val title: String
        val iconRes: Int
        val actions = mutableListOf<NotificationCompat.Action>()

        when {
            isSnooze -> {
                val remaining = pref.getRemainingSnoozeMinutes()
                title = "Perlindungan Dijeda ($remaining mnt)"
                iconRes = R.drawable.ic_shield_alert

                // Aksi Lanjutkan
                val resumeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_RESUME
                }
                val resumePending = PendingIntent.getBroadcast(
                    context, 1, resumeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                actions.add(NotificationCompat.Action(0, "Lanjutkan", resumePending))
            }

            !isMasterOn -> {
                title = "Perlindungan Dimatikan"
                iconRes = R.drawable.ic_shield_alert

                // Aksi Nyalakan
                val onIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_TOGGLE_MASTER
                }
                val onPending = PendingIntent.getBroadcast(
                    context, 2, onIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                actions.add(NotificationCompat.Action(0, "Nyalakan", onPending))
            }

            !isScheduleActive -> {
                title = "Perlindungan Standby (${schedule.getFormattedScheduleRange()})"
                iconRes = R.drawable.ic_shield_alert

                // Aksi Matikan Master
                val offIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_TOGGLE_MASTER
                }
                val offPending = PendingIntent.getBroadcast(
                    context, 3, offIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                actions.add(NotificationCompat.Action(0, "Matikan", offPending))
            }

            else -> {
                title = "Perlindungan Aktif"
                iconRes = R.drawable.ic_shield_check

                // Aksi Jeda 15 Mnt
                val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_SNOOZE_15
                }
                val snoozePending = PendingIntent.getBroadcast(
                    context, 4, snoozeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                actions.add(NotificationCompat.Action(0, "Jeda 15 Mnt", snoozePending))

                // Aksi Matikan
                val offIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_TOGGLE_MASTER
                }
                val offPending = PendingIntent.getBroadcast(
                    context, 5, offIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                actions.add(NotificationCompat.Action(0, "Matikan", offPending))
            }
        }

        // Klik pada bodi notifikasi akan membuka MainActivity
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Otomatis muncul kembali jika notifikasi di-swipe atau dihapus oleh pengguna
        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_NOTIFICATION_DISMISSED
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            context, 99, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle("Project Red: $title")
            .setContentText(statsText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)

        for (action in actions) {
            builder.addAction(action)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, builder.build())
    }

    fun cancel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(NOTIFICATION_ID)
    }
}
