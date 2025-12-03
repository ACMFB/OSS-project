package com.example.finalfinalfinal   // <- 여기 줄은 네 프로젝트에 원래 있던 package 줄 그대로 두기!

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 우리가 만든 activity_main.xml을 화면으로 쓴다는 뜻
        setContentView(R.layout.activity_main)
    }
}
