package com.example.finalui1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.hangeulstudy.DifficultySelectActivity
import com.example.hangeulstudy.StudyActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadLocale() // 앱 시작 시 저장된 언어 설정 불러오기
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 메인 화면 버튼들
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStudy = findViewById<Button>(R.id.btnStudy)
        val btnReview = findViewById<Button>(R.id.btnReview)
        val btnSettings = findViewById<ImageView>(R.id.btnSettings)

        // 게임 시작 → GameSelectActivity (미니게임 선택 화면)
        btnStart.setOnClickListener {
            startActivity(
                Intent(this, GameSelectActivity::class.java)
            )
        }

        // 공부하기 → 한글 학습 모듈
        btnStudy.setOnClickListener {
            startActivity(
                Intent(this, DifficultySelectActivity::class.java)
            )
        }

        // 복습하기 → 즐겨찾기 단어 학습
        btnReview.setOnClickListener {
            val intent = Intent(this, StudyActivity::class.java)
            intent.putExtra("study_mode", "review")
            startActivity(intent)
        }

        // 설정 버튼 클릭 시 언어 설정 다이얼로그 표시
        btnSettings.setOnClickListener {
            showLanguageDialog()
        }
    }

    private fun showLanguageDialog() {
        val languages = arrayOf(
            getString(R.string.korean),
            getString(R.string.english),
            getString(R.string.chinese)
        )
        val langCodes = arrayOf("ko", "en", "zh")

        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.language_setting)
        builder.setItems(languages) { _, which ->
            val selectedLangCode = langCodes[which]
            changeLocale(selectedLangCode)
        }
        builder.create().show()
    }

    private fun changeLocale(langCode: String) {
        saveLocale(langCode) // 언어 변경 시 설정 저장

        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        recreate() // 액티비티를 다시 시작하여 언어 변경사항을 즉시 적용
    }

    private fun saveLocale(langCode: String) {
        val prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("My_Lang", langCode)
        editor.apply()
    }

    private fun loadLocale() {
        val prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val language = prefs.getString("My_Lang", "") // 기본값은 시스템 언어
        if (!language.isNullOrEmpty()) {
            val locale = Locale(language)
            Locale.setDefault(locale)
            val config = resources.configuration
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)
        }
    }
}
