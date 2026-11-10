package com.hamara.helper

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    private lateinit var cardControl: MaterialCardView
    private lateinit var tvStatus: TextView
    private lateinit var btnEnableService: MaterialButton
    private lateinit var tvOpenSettings: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cardControl = findViewById(R.id.cardControl)
        tvStatus = findViewById(R.id.tvStatus)
        btnEnableService = findViewById(R.id.btnEnableService)
        tvOpenSettings = findViewById(R.id.tvOpenSettings)

        btnEnableService.setOnClickListener {
            val isServiceRunning = isAccessibilityServiceEnabled(this, CaptchaAccessibilityService::class.java)
            if (!isServiceRunning) {
                openAccessibilitySettings()
            } else {
                // Toggle paused state
                val prefs = getSharedPreferences(CaptchaAccessibilityService.PREFS_NAME, Context.MODE_PRIVATE)
                val isCurrentlyPaused = prefs.getBoolean(CaptchaAccessibilityService.KEY_IS_PAUSED, false)
                prefs.edit().putBoolean(CaptchaAccessibilityService.KEY_IS_PAUSED, !isCurrentlyPaused).apply()
                updateServiceStatus()
            }
        }

        tvOpenSettings.setOnClickListener {
            openAccessibilitySettings()
        }
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun updateServiceStatus() {
        val isServiceRunning = isAccessibilityServiceEnabled(this, CaptchaAccessibilityService::class.java)

        if (!isServiceRunning) {
            tvStatus.text = getString(R.string.status_inactive)
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.crimson_red))
            cardControl.strokeColor = ContextCompat.getColor(this, R.color.crimson_red)

            btnEnableService.text = getString(R.string.btn_enable)
            btnEnableService.isEnabled = true
            btnEnableService.setBackgroundColor(ContextCompat.getColor(this, R.color.crimson_red))
            btnEnableService.setTextColor(ContextCompat.getColor(this, R.color.pure_white))

            tvOpenSettings.visibility = View.GONE
        } else {
            tvOpenSettings.visibility = View.VISIBLE

            val prefs = getSharedPreferences(CaptchaAccessibilityService.PREFS_NAME, Context.MODE_PRIVATE)
            val isPaused = prefs.getBoolean(CaptchaAccessibilityService.KEY_IS_PAUSED, false)

            if (isPaused) {
                tvStatus.text = getString(R.string.status_paused)
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.paused_amber))
                cardControl.strokeColor = ContextCompat.getColor(this, R.color.paused_amber)

                btnEnableService.text = getString(R.string.btn_resume)
                btnEnableService.isEnabled = true
                btnEnableService.setBackgroundColor(ContextCompat.getColor(this, R.color.active_green))
                btnEnableService.setTextColor(ContextCompat.getColor(this, R.color.black_bg))
            } else {
                tvStatus.text = getString(R.string.status_active)
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.active_green))
                cardControl.strokeColor = ContextCompat.getColor(this, R.color.active_green)

                btnEnableService.text = getString(R.string.btn_pause)
                btnEnableService.isEnabled = true
                btnEnableService.setBackgroundColor(ContextCompat.getColor(this, R.color.crimson_red))
                btnEnableService.setTextColor(ContextCompat.getColor(this, R.color.pure_white))
            }
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (enabledService in enabledServices) {
            val enabledServiceInfo = enabledService.resolveInfo.serviceInfo
            if (enabledServiceInfo.packageName == context.packageName &&
                enabledServiceInfo.name == serviceClass.name) {
                return true
            }
        }
        return false
    }
}
