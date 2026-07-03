package com.example.server

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Инициализируем логгер
        Logger.init(this)
        Logger.log("=== MainActivity onCreate ===")

        tvStatus = findViewById(R.id.tvStatus)

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            Logger.log("Start button clicked")
            try {
                ServerService.start(this)
                tvStatus.text = "✅ Сервер запущен на порту 8080"
                Toast.makeText(this, "Сервер запущен", Toast.LENGTH_SHORT).show()
                Logger.log("Server started successfully")
            } catch (e: Exception) {
                Logger.log("Error starting server", e)
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        findViewById<Button>(R.id.btnStop).setOnClickListener {
            Logger.log("Stop button clicked")
            try {
                ServerService.stop(this)
                tvStatus.text = "⛔ Сервер остановлен"
                Toast.makeText(this, "Сервер остановлен", Toast.LENGTH_SHORT).show()
                Logger.log("Server stopped")
            } catch (e: Exception) {
                Logger.log("Error stopping server", e)
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        
        Logger.log("MainActivity initialized")
        Toast.makeText(this, "✅ Логи сохраняются в Download/ServerLogs/", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.log("=== MainActivity onDestroy ===")
    }
}
