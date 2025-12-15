package com.example.finalui1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.hangeulstudy.StudyActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 메인 화면 버튼들
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStudy = findViewById<Button>(R.id.btnStudy)

        //  게임 시작 → GameSelectActivity (미니게임 선택 화면)
        btnStart.setOnClickListener {
            startActivity(
                Intent(this, GameSelectActivity::class.java)
            )
        }

        //  공부하기 → 한글 학습 모듈
        btnStudy.setOnClickListener {
            startActivity(
                Intent(this, StudyActivity::class.java)
            )
        }

        // 시스템 바 패딩 처리 (지금 UI 유지용)

    }
}
