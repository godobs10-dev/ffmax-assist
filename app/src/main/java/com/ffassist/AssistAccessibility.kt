package com.ffassist

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.*

class AssistAccessibility : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    companion object {
        var AIM_X_RATIO  = 0.5f
        var AIM_Y_RATIO  = 0.38f
        var TOUCH_MS     = 60L
        var INTERVAL_MS  = 120L
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        startAimbotLoop()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() { scope.cancel() }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun startAimbotLoop() {
        scope.launch {
            while (isActive) {
                if (AssistState.aimbotEnabled) {
                    fireShoulderTap()
                }
                delay(INTERVAL_MS)
            }
        }
    }

    private fun fireShoulderTap() {
        val metrics = resources.displayMetrics
        val x = metrics.widthPixels.toFloat() * AIM_X_RATIO
        val y = metrics.heightPixels.toFloat() * AIM_Y_RATIO

        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, TOUCH_MS)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {}
            override fun onCancelled(gestureDescription: GestureDescription?) {}
        }, null)
    }
}
