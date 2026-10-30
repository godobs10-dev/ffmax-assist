package com.ffassist

import android.app.*
import android.content.Intent
import android.os.IBinder
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject
import android.os.Build

class HttpServerService : Service() {

    private lateinit var server: AssistServer

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, buildNotification())
        server = AssistServer(8765, this)
        server.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        server.stop()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    class AssistServer(port: Int, private val svc: HttpServerService) : NanoHTTPD(port) {

        override fun serve(session: IHTTPSession): Response {
            val headers = mapOf(
                "Access-Control-Allow-Origin" to "*",
                "Access-Control-Allow-Methods" to "POST, OPTIONS",
                "Access-Control-Allow-Headers" to "Content-Type"
            )
            if (session.method == Method.OPTIONS) {
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "").apply {
                    headers.forEach { addHeader(it.key, it.value) }
                }
            }
            if (session.method == Method.POST && session.uri == "/cmd") {
                val body = mutableMapOf<String, String>()
                session.parseBody(body)
                val raw = body["postData"] ?: return jsonResponse(false, "empty body", headers)
                return try {
                    val json = JSONObject(raw)
                    val feature = json.getString("feature")
                    val state = json.getBoolean("state")
                    handleCommand(feature, state)
                    jsonResponse(true, null, headers)
                } catch (e: Exception) {
                    jsonResponse(false, e.message ?: "parse error", headers)
                }
            }
            if (session.method == Method.GET && session.uri == "/") {
                val html = svc.assets.open("index.html").bufferedReader().readText()
                return newFixedLengthResponse(Response.Status.OK, "text/html", html).apply {
                    headers.forEach { addHeader(it.key, it.value) }
                }
            }
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "not found")
        }

        private fun handleCommand(feature: String, state: Boolean) {
            when (feature) {
                "aim" -> AssistState.aimbotEnabled = state
                "esp" -> {
                    AssistState.espEnabled = state
                    val intent = Intent(svc, EspOverlayService::class.java)
                    intent.putExtra("state", state)
                    if (state) svc.startService(intent)
                    else svc.stopService(intent)
                }
            }
        }

        private fun jsonResponse(ok: Boolean, error: String?, headers: Map<String, String>): Response {
            val json = JSONObject().apply {
                put("ok", ok)
                if (error != null) put("error", error)
            }
            return newFixedLengthResponse(Response.Status.OK, "application/json", json.toString()).apply {
                headers.forEach { addHeader(it.key, it.value) }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel("ffassist", "FF Assist", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(chan)
        }
    }

    private fun buildNotification(): Notification {
        return Notification.Builder(this, "ffassist")
            .setContentTitle("FF Assist")
            .setContentText("Serviço ativo — porta 8765")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }
}
