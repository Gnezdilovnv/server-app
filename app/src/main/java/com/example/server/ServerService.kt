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
        Logger.log("=== ServerService onCreate ===")
        
        try {
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, createNotification("Сервер запущен на порту $PORT"))
            Logger.log("Foreground service started")
            
            // Создаём папку для данных
            val dataDir = File(filesDir, DATA_DIR)
            if (!dataDir.exists()) {
                dataDir.mkdirs()
                Logger.log("Data directory created: ${dataDir.absolutePath}")
            }
            Logger.log("Data directory: ${dataDir.absolutePath}")
        } catch (e: Exception) {
            Logger.log("Error in onCreate", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.log("onStartCommand called")
        startServer()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.log("=== ServerService onDestroy ===")
        stopServer()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startServer() {
        if (isRunning) {
            Logger.log("Server already running")
            return
        }
        isRunning = true
        Logger.log("Starting server on port $PORT")
        
        Thread {
            try {
                serverSocket = ServerSocket(PORT)
                Logger.log("Server socket created on port $PORT")
                while (isRunning) {
                    val client = serverSocket?.accept() ?: break
                    Logger.log("Client connected: ${client.inetAddress.hostAddress}")
                    handleClient(client)
                }
            } catch (e: Exception) {
                if (isRunning) {
                    Logger.log("Server error", e)
                }
            }
        }.start()
    }

    private fun stopServer() {
        Logger.log("Stopping server")
        isRunning = false
        try {
            serverSocket?.close()
            Logger.log("Server socket closed")
        } catch (e: Exception) {
            Logger.log("Error closing server socket", e)
        }
    }

    private fun handleClient(client: java.net.Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))
            val requestLine = reader.readLine()
            if (requestLine == null) {
                client.close()
                return
            }
            
            Logger.log("Request: $requestLine")
            val parts = requestLine.split(" ")
            if (parts.size < 3) {
                client.close()
                return
            }

            val method = parts[0]
            val path = parts[1]

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
                    Logger.log("POST /api/json: ${json.take(100)}...")
                    
                    val hash = sha256(json)
                    val dataDir = File(filesDir, DATA_DIR)
                    val file = File(dataDir, hash)
                    if (!file.exists()) {
                        file.writeText(json)
                        Logger.log("Saved new file: $hash")
                    } else {
                        Logger.log("File already exists: $hash")
                    }
                    
                    writer.println("HTTP/1.1 200 OK")
                    writer.println("Content-Type: application/json")
                    writer.println()
                    writer.println("{\"status\":\"ok\",\"hash\":\"$hash\"}")
                }

                method == "GET" && path.startsWith("/api/json/") -> {
                    val hash = path.substringAfter("/api/json/")
                    Logger.log("GET /api/json/$hash")
                    val dataDir = File(filesDir, DATA_DIR)
                    val file = File(dataDir, hash)
                    if (file.exists()) {
                        val data = file.readText()
                        writer.println("HTTP/1.1 200 OK")
                        writer.println("Content-Type: application/json")
                        writer.println("Content-Length: ${data.length}")
                        writer.println()
                        writer.println(data)
                        Logger.log("File found: $hash")
                    } else {
                        writer.println("HTTP/1.1 404 Not Found")
                        writer.println()
                        writer.println("Not found")
                        Logger.log("File not found: $hash")
                    }
                }

                else -> {
                    Logger.log("Unknown endpoint: $method $path")
                    writer.println("HTTP/1.1 404 Not Found")
                    writer.println()
                    writer.println("Not found")
                }
            }

            writer.flush()
            client.close()
            Logger.log("Client disconnected")
        } catch (e: Exception) {
            Logger.log("Error handling client", e)
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
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "JSON Server",
                    NotificationManager.IMPORTANCE_LOW
                )
                val manager = getSystemService(NotificationManager::class.java)
                manager?.createNotificationChannel(channel)
                Logger.log("Notification channel created")
            } catch (e: Exception) {
                Logger.log("Error creating notification channel", e)
            }
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
