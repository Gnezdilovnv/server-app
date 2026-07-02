package com.example.server

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            ServerService.start(this)
            tvStatus.text = "✅ Сервер запущен на порту 8080"
        }

        findViewById<Button>(R.id.btnStop).setOnClickListener {
            ServerService.stop(this)
            tvStatus.text = "⛔ Сервер остановлен"
        }
    }
}
