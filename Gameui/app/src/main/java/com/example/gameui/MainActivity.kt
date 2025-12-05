package com.example.gameui

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStudy = findViewById<Button>(R.id.btnStudy)
        val btnRanking = findViewById<Button>(R.id.btnRanking)
        val btnSound = findViewById<ImageButton>(R.id.btnSound)

        btnStart.setOnClickListener {
            Toast.makeText(this, "게임 시작!", Toast.LENGTH_SHORT).show()
        }

        btnStudy.setOnClickListener {
            Toast.makeText(this, "공부하기!", Toast.LENGTH_SHORT).show()
        }

        btnRanking.setOnClickListener {
            Toast.makeText(this, "랭킹!", Toast.LENGTH_SHORT).show()
        }

        btnSound.setOnClickListener {
            Toast.makeText(this, "사운드 버튼!", Toast.LENGTH_SHORT).show()
        }
    }
}