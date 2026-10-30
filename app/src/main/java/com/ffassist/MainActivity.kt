package com.ffassist

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast

class MainActivity : Activity() {

    companion object {
        const val REQ_OVERLAY = 1001
        const val REQ_ACCESSIBILITY = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQ_OVERLAY)
        } else {
            checkAccessibility()
        }
    }

    private fun checkAccessibility() {
        val enabled = isAccessibilityEnabled()
        if (!enabled) {
            Toast.makeText(this,
                "Ative o FF Assist em Acessibilidade > FF Assist", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } else {
            startServices()
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val prefString = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return prefString.contains("$packageName/${AssistAccessibility::class.java.name}")
    }

    private fun startServices() {
        startForegroundService(Intent(this, HttpServerService::class.java))
        Toast.makeText(this, "FF Assist ativo — abra o site no navegador", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_OVERLAY) {
            if (Settings.canDrawOverlays(this)) checkAccessibility()
            else Toast.makeText(this, "Permissão de overlay necessária", Toast.LENGTH_LONG).show()
        }
    }
}
