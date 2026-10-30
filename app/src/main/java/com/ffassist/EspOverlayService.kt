package com.ffassist

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import kotlinx.coroutines.*

class EspOverlayService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var overlayView: EspView
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    companion object {
        const val REFRESH_MS = 50L
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val state = intent?.getBooleanExtra("state", false) ?: false
        if (!state) { stopSelf(); return START_NOT_STICKY }
        attachOverlay()
        startEspLoop()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        if (::overlayView.isInitialized) {
            try { wm.removeView(overlayView) } catch (_: Exception) {}
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun attachOverlay() {
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        overlayView = EspView(this)
        wm.addView(overlayView, params)
    }

    private fun startEspLoop() {
        scope.launch {
            while (isActive && AssistState.espEnabled) {
                val w = resources.displayMetrics.widthPixels.toFloat()
                val h = resources.displayMetrics.heightPixels.toFloat()
                val cx = w * 0.5f; val cy = h * 0.4f
                val bw = 80f; val bh = 140f

                synchronized(AssistState.espLock) {
                    AssistState.espBoxes.clear()
                    AssistState.espBoxes.add(RectF(cx - bw/2, cy - bh/2, cx + bw/2, cy + bh/2))
                }

                withContext(Dispatchers.Main) {
                    if (::overlayView.isInitialized) overlayView.invalidate()
                }
                delay(REFRESH_MS)
            }
        }
    }

    class EspView(context: Context) : View(context) {

        private val boxPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            isAntiAlias = true
        }

        private val cornerPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }

        private val cornerLen = 14f

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (!AssistState.espEnabled) return

            val boxes: List<RectF>
            synchronized(AssistState.espLock) {
                boxes = AssistState.espBoxes.toList()
            }

            for (box in boxes) {
                canvas.drawRect(box, boxPaint)
                drawCorners(canvas, box)
            }
        }

        private fun drawCorners(canvas: Canvas, r: RectF) {
            val l = cornerLen
            canvas.drawLine(r.left, r.top, r.left + l, r.top, cornerPaint)
            canvas.drawLine(r.left, r.top, r.left, r.top + l, cornerPaint)
            canvas.drawLine(r.right - l, r.top, r.right, r.top, cornerPaint)
            canvas.drawLine(r.right, r.top, r.right, r.top + l, cornerPaint)
            canvas.drawLine(r.left, r.bottom - l, r.left, r.bottom, cornerPaint)
            canvas.drawLine(r.left, r.bottom, r.left + l, r.bottom, cornerPaint)
            canvas.drawLine(r.right - l, r.bottom, r.right, r.bottom, cornerPaint)
            canvas.drawLine(r.right, r.bottom - l, r.right, r.bottom, cornerPaint)
        }
    }
}
