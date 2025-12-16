package com.example.finalui1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.hangeulstudy.DifficultySelectActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 메인 화면 버튼들
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStudy = findViewById<Button>(R.id.btnStudy)
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
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        // 변경된 언어를 적용하기 위해 액티비티를 다시 시작합니다.
        recreate()
    }
}
