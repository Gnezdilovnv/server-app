package com.example.server

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object Logger {
    private const val LOG_DIR = "ServerLogs"
    private const val LOG_FILE = "server_log.txt"
    private const val TAG = "ServerLogger"
    private var context: Context? = null
    private var isInitialized = false

    fun init(ctx: Context) {
        context = ctx
        isInitialized = true
        log("=== LOGGER INITIALIZED ===")
        log("Device: ${android.os.Build.MODEL}")
        log("Android: ${android.os.Build.VERSION.RELEASE}")
        log("App version: 1.0")
        Log.d(TAG, "Logger initialized")
    }

    fun log(message: String, throwable: Throwable? = null) {
        // Пишем в logcat
        if (throwable != null) {
            Log.e(TAG, message, throwable)
        } else {
            Log.d(TAG, message)
        }

        // Пишем в файл
        try {
            if (!isInitialized || context == null) {
                Log.w(TAG, "Logger not initialized")
                return
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            val logMessage = buildString {
                append("[")
                append(timestamp)
                append("] ")
                append(message)
                if (throwable != null) {
                    append("\n")
                    append(throwable.stackTraceToString())
                }
                append("\n")
            }

            val logFile = getLogFile()
            // Принудительно создаём папку и файл
            logFile.parentFile?.mkdirs()
            if (!logFile.exists()) {
                logFile.createNewFile()
                Log.d(TAG, "Created log file: ${logFile.absolutePath}")
            }

            // Записываем
            FileWriter(logFile, true).use { writer ->
                writer.write(logMessage)
                writer.flush()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to write log", e)
        }
    }

    fun clearLog() {
        try {
            val logFile = getLogFile()
            if (logFile.exists()) {
                logFile.delete()
                log("=== LOG CLEARED ===")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear log", e)
        }
    }

    private fun getLogFile(): File {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val logDir = File(downloadDir, LOG_DIR)
        if (!logDir.exists()) {
            logDir.mkdirs()
            Log.d(TAG, "Created log directory: ${logDir.absolutePath}")
        }
        return File(logDir, LOG_FILE)
    }
}
