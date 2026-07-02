package com.example.server

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.*
import java.net.ServerSocket
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class ServerService : Service() {
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "server_channel"
        private const val PORT = 8080
        private const val DATA_DIR = "server_data"
        
        fun start(context: Context) {
            context.startService(Intent(context, ServerService::class.java))
        }
        fun stop(context: Context) {
            context.stopService(Intent(context, ServerService::class.java))
        }
    }

    private var serverSocket: ServerSocket? = null
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Сервер запущен на порту $PORT"))
        
        // Создаём папку для данных
        val dataDir = File(filesDir, DATA_DIR)
        if (!dataDir.exists()) dataDir.mkdirs()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startServer()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopServer()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startServer() {
        if (isRunning) return
        isRunning = true
        
        Thread {
            try {
                serverSocket = ServerSocket(PORT)
                while (isRunning) {
                    val client = serverSocket?.accept() ?: break
                    handleClient(client)
                }
            } catch (e: Exception) {
                if (isRunning) e.printStackTrace()
            }
        }.start()
    }

    private fun stopServer() {
        isRunning = false
        try { serverSocket?.close() } catch (_: Exception) {}
    }

    private fun handleClient(client: java.net.Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 3) return

            val method = parts[0]
            val path = parts[1]

            // Читаем заголовки
            var contentLength = 0
            while (true) {
                val line = reader.readLine()
                if (line.isNullOrEmpty()) break
                if (line.startsWith("Content-Length:")) {
                    contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
                }
            }

            val writer = PrintWriter(client.getOutputStream(), true)

            when {
                method == "POST" && path == "/api/json" -> {
                    val bodyChars = CharArray(contentLength)
                    reader.read(bodyChars, 0, contentLength)
                    val json = String(bodyChars)
                    val hash = sha256(json)
                    
                    // Сохраняем файл
                    val dataDir = File(filesDir, DATA_DIR)
                    val file = File(dataDir, hash)
                    if (!file.exists()) {
                        file.writeText(json)
                    }
                    
                    writer.println("HTTP/1.1 200 OK")
                    writer.println("Content-Type: application/json")
                    writer.println()
                    writer.println("{\"status\":\"ok\",\"hash\":\"$hash\"}")
                }

                method == "GET" && path.startsWith("/api/json/") -> {
                    val hash = path.substringAfter("/api/json/")
                    val dataDir = File(filesDir, DATA_DIR)
                    val file = File(dataDir, hash)
                    if (file.exists()) {
                        val data = file.readText()
                        writer.println("HTTP/1.1 200 OK")
                        writer.println("Content-Type: application/json")
                        writer.println("Content-Length: ${data.length}")
                        writer.println()
                        writer.println(data)
                    } else {
                        writer.println("HTTP/1.1 404 Not Found")
                        writer.println()
                        writer.println("Not found")
                    }
                }

                else -> {
                    writer.println("HTTP/1.1 404 Not Found")
                    writer.println()
                    writer.println("Not found")
                }
            }

            writer.flush()
            client.close()
        } catch (e: Exception) {
            e.printStackTrace()
            try { client.close() } catch (_: Exception) {}
        }
    }

    private fun sha256(input: String): String {
        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "JSON Server",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JSON Server")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
