package com.projectred.scrollguard.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.projectred.scrollguard.R
import com.projectred.scrollguard.data.PreferenceManager
import com.projectred.scrollguard.data.StatsManager
import com.projectred.scrollguard.databinding.ActivityMainBinding
import com.projectred.scrollguard.model.TargetApp
import com.projectred.scrollguard.service.ScrollGuardAccessibilityService
import com.projectred.scrollguard.ui.adapter.AppToggleAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var statsManager: StatsManager
    private lateinit var scheduleManager: com.projectred.scrollguard.data.ScheduleManager
    private lateinit var appAdapter: AppToggleAdapter

    private val statsListener = {
        runOnUiThread {
            updateStats()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)
        statsManager = StatsManager(this)
        scheduleManager = com.projectred.scrollguard.data.ScheduleManager(this)

        setupRecyclerView()
        setupMasterSwitch()
        setupPersistentNotificationSwitch()
        setupToastSwitch()
        setupHapticSwitch()
        setupScheduleControls()
        setupButtons()

        StatsManager.addListener(statsListener)
    }

    override fun onResume() {
        super.onResume()
        updateProtectionStatus()
        updateStats()
        com.projectred.scrollguard.service.OngoingNotificationHelper.update(this)
    }

    override fun onDestroy() {
        StatsManager.removeListener(statsListener)
        super.onDestroy()
    }

    private fun updateStats() {
        val totalBlocks = statsManager.getTodayTotalBlocks()
        val minutesSaved = statsManager.getTodayTimeSavedMinutes()
        val ig = statsManager.getTodayBlocksForApp(TargetApp.INSTAGRAM)
        val fb = statsManager.getTodayBlocksForApp(TargetApp.FACEBOOK)
        val yt = statsManager.getTodayBlocksForApp(TargetApp.YOUTUBE)
        val tt = statsManager.getTodayBlocksForApp(TargetApp.TIKTOK)

        binding.tvTotalBlocks.text = "$totalBlocks Kali"
        binding.tvTimeSaved.text = "$minutesSaved Menit"
        binding.tvStatsBreakdown.text = "Instagram: $ig • Facebook: $fb • YouTube: $yt • TikTok: $tt"

        com.projectred.scrollguard.service.OngoingNotificationHelper.update(this)
    }

    private fun setupRecyclerView() {
        appAdapter = AppToggleAdapter(preferenceManager) { _, _ ->
            // Perubahan status sakelar per aplikasi disimpan otomatis
        }
        binding.rvAppsList.layoutManager = LinearLayoutManager(this)
        binding.rvAppsList.adapter = appAdapter
    }

    private fun setupMasterSwitch() {
        binding.switchMaster.isChecked = preferenceManager.isMasterEnabled
        binding.switchMaster.setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.isMasterEnabled = isChecked
            updateProtectionStatus()
            com.projectred.scrollguard.service.ScrollGuardTileService.requestTileUpdate(this)
            com.projectred.scrollguard.service.OngoingNotificationHelper.update(this)
        }
    }

    private fun setupPersistentNotificationSwitch() {
        binding.switchPersistentNotification.isChecked = preferenceManager.isPersistentNotificationEnabled
        binding.switchPersistentNotification.setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.isPersistentNotificationEnabled = isChecked
            if (isChecked) {
                requestNotificationPermissionIfNeeded()
                com.projectred.scrollguard.service.OngoingNotificationHelper.update(this)
            } else {
                com.projectred.scrollguard.service.OngoingNotificationHelper.cancel(this)
            }
        }
    }

    private fun setupToastSwitch() {
        binding.switchToast.isChecked = preferenceManager.isToastFeedbackEnabled
        binding.switchToast.setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.isToastFeedbackEnabled = isChecked
            if (isChecked) {
                requestNotificationPermissionIfNeeded()
            }
        }
    }

    private fun setupHapticSwitch() {
        binding.switchHaptic.isChecked = preferenceManager.isHapticFeedbackEnabled
        binding.switchHaptic.setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.isHapticFeedbackEnabled = isChecked
        }
    }

    private fun setupScheduleControls() {
        binding.switchSchedule.isChecked = scheduleManager.isScheduleEnabled
        updateScheduleSummary()

        binding.switchSchedule.setOnCheckedChangeListener { _, isChecked ->
            scheduleManager.isScheduleEnabled = isChecked
            updateScheduleSummary()
            updateProtectionStatus()
        }

        binding.btnPresetBedtime.setOnClickListener {
            scheduleManager.applyBedtimePreset()
            binding.switchSchedule.isChecked = true
            updateScheduleSummary()
            updateProtectionStatus()
        }

        binding.btnPresetWork.setOnClickListener {
            scheduleManager.applyWorkHoursPreset()
            binding.switchSchedule.isChecked = true
            updateScheduleSummary()
            updateProtectionStatus()
        }
    }

    private fun updateScheduleSummary() {
        if (scheduleManager.isScheduleEnabled) {
            binding.tvScheduleSummary.text = "Jadwal aktif: ${scheduleManager.getFormattedScheduleRange()}"
        } else {
            binding.tvScheduleSummary.text = "Aktifkan perlindungan hanya pada jam tertentu."
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    private fun setupButtons() {
        binding.btnOpenAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }

        binding.btnViewDiagnostics.setOnClickListener {
            val intent = Intent(this, DiagnosticActivity::class.java)
            startActivity(intent)
        }

        binding.btnSnooze5.setOnClickListener {
            preferenceManager.setSnooze(5)
            updateProtectionStatus()
            com.projectred.scrollguard.service.ScrollGuardTileService.requestTileUpdate(this)
            com.projectred.scrollguard.service.OngoingNotificationHelper.update(this)
        }

        binding.btnSnooze15.setOnClickListener {
            preferenceManager.setSnooze(15)
            updateProtectionStatus()
            com.projectred.scrollguard.service.ScrollGuardTileService.requestTileUpdate(this)
            com.projectred.scrollguard.service.OngoingNotificationHelper.update(this)
        }

        binding.btnSnooze30.setOnClickListener {
            preferenceManager.setSnooze(30)
            updateProtectionStatus()
            com.projectred.scrollguard.service.ScrollGuardTileService.requestTileUpdate(this)
            com.projectred.scrollguard.service.OngoingNotificationHelper.update(this)
        }

        binding.btnCancelSnooze.setOnClickListener {
            preferenceManager.clearSnooze()
            updateProtectionStatus()
            com.projectred.scrollguard.service.ScrollGuardTileService.requestTileUpdate(this)
            com.projectred.scrollguard.service.OngoingNotificationHelper.update(this)
        }
    }

    private fun updateProtectionStatus() {
        val isAccessibilityEnabled = isAccessibilityServiceEnabled(this)
        val isRunning = ScrollGuardAccessibilityService.isServiceRunning || isAccessibilityEnabled
        val isMasterOn = preferenceManager.isMasterEnabled
        val isSnooze = preferenceManager.isSnoozeActive()
        val isScheduleActive = scheduleManager.isWithinActiveSchedule()

        if (isSnooze && isRunning && isMasterOn) {
            val remaining = preferenceManager.getRemainingSnoozeMinutes()
            binding.cardStatus.setCardBackgroundColor(ContextCompat.getColor(this, R.color.status_amber_bg))
            binding.ivStatusIcon.setImageResource(R.drawable.ic_shield_alert)
            binding.tvStatusTitle.text = "Perlindungan Dijeda Sementara"
            binding.tvStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.status_amber_text))
            binding.tvStatusDesc.text = "Jeda aktif ($remaining menit tersisa). Perlindungan akan otomatis menyala kembali."
            binding.btnOpenAccessibility.text = "Lanjutkan Perlindungan Sekarang"
            binding.btnOpenAccessibility.setBackgroundColor(ContextCompat.getColor(this, R.color.status_green_bg))
            binding.btnOpenAccessibility.setOnClickListener {
                preferenceManager.clearSnooze()
                updateProtectionStatus()
            }

            binding.btnCancelSnooze.visibility = android.view.View.VISIBLE
            binding.tvSnoozeDesc.text = "Jeda aktif ($remaining menit tersisa). Ketuk Lanjutkan untuk menyalakan kembali."
        } else if (isRunning && isMasterOn && !isScheduleActive) {
            binding.btnCancelSnooze.visibility = android.view.View.GONE
            binding.tvSnoozeDesc.text = "Beri izin menonton sementara tanpa mematikan sakelar utama."

            binding.cardStatus.setCardBackgroundColor(ContextCompat.getColor(this, R.color.status_amber_bg))
            binding.ivStatusIcon.setImageResource(R.drawable.ic_shield_alert)
            binding.tvStatusTitle.text = "Perlindungan Standby"
            binding.tvStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.status_amber_text))
            binding.tvStatusDesc.text = "Di luar jam jadwal aktif (${scheduleManager.getFormattedScheduleRange()}). Perlindungan akan aktif otomatis saat jam tiba."
            binding.btnOpenAccessibility.text = "Kelola Layanan Aksesibilitas"
            binding.btnOpenAccessibility.setBackgroundColor(ContextCompat.getColor(this, R.color.surface_card_border))
            binding.btnOpenAccessibility.setOnClickListener {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
        } else if (isRunning && isMasterOn) {
            binding.btnCancelSnooze.visibility = android.view.View.GONE
            binding.tvSnoozeDesc.text = "Beri izin menonton sementara tanpa mematikan sakelar utama."

            binding.cardStatus.setCardBackgroundColor(ContextCompat.getColor(this, R.color.status_green_bg))
            binding.ivStatusIcon.setImageResource(R.drawable.ic_shield_check)
            binding.tvStatusTitle.text = getString(R.string.status_active_title)
            binding.tvStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.status_green_text))
            binding.tvStatusDesc.text = getString(R.string.status_active_desc)
            binding.btnOpenAccessibility.text = "Kelola Layanan Aksesibilitas"
            binding.btnOpenAccessibility.setBackgroundColor(ContextCompat.getColor(this, R.color.surface_card_border))
            binding.btnOpenAccessibility.setOnClickListener {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
        } else if (!isMasterOn) {
            binding.btnCancelSnooze.visibility = android.view.View.GONE
            binding.tvSnoozeDesc.text = "Beri izin menonton sementara tanpa mematikan sakelar utama."

            binding.cardStatus.setCardBackgroundColor(ContextCompat.getColor(this, R.color.status_amber_bg))
            binding.ivStatusIcon.setImageResource(R.drawable.ic_shield_alert)
            binding.tvStatusTitle.text = "Perlindungan Dijeda"
            binding.tvStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.status_amber_text))
            binding.tvStatusDesc.text = "Sakelar utama dimatikan. Aktifkan kembali sakelar untuk memblokir video pendek."
            binding.btnOpenAccessibility.text = getString(R.string.btn_open_accessibility)
            binding.btnOpenAccessibility.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_red))
            binding.btnOpenAccessibility.setOnClickListener {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
        } else {
            binding.btnCancelSnooze.visibility = android.view.View.GONE
            binding.tvSnoozeDesc.text = "Beri izin menonton sementara tanpa mematikan sakelar utama."

            binding.cardStatus.setCardBackgroundColor(ContextCompat.getColor(this, R.color.status_amber_bg))
            binding.ivStatusIcon.setImageResource(R.drawable.ic_shield_alert)
            binding.tvStatusTitle.text = getString(R.string.status_inactive_title)
            binding.tvStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.status_amber_text))
            binding.tvStatusDesc.text = getString(R.string.status_inactive_desc)
            binding.btnOpenAccessibility.text = getString(R.string.btn_open_accessibility)
            binding.btnOpenAccessibility.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_red))
            binding.btnOpenAccessibility.setOnClickListener {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedServiceName = "${context.packageName}/${ScrollGuardAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)

        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedServiceName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }
}
