package com.hamara.helper

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnEnableService: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        btnEnableService = findViewById(R.id.btnEnableService)

        btnEnableService.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }

    private fun updateServiceStatus() {
        val isServiceRunning = isAccessibilityServiceEnabled(this, CaptchaAccessibilityService::class.java)

        if (isServiceRunning) {
            tvStatus.text = getString(R.string.status_active)
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.active_green))
            btnEnableService.text = getString(R.string.btn_enabled)
            btnEnableService.isEnabled = false
            btnEnableService.setBackgroundColor(ContextCompat.getColor(this, R.color.card_stroke))
        } else {
            tvStatus.text = getString(R.string.status_inactive)
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.crimson_red))
            btnEnableService.text = getString(R.string.btn_enable)
            btnEnableService.isEnabled = true
            btnEnableService.setBackgroundColor(ContextCompat.getColor(this, R.color.crimson_red))
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
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
