package com.example.iconwordgame

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    private lateinit var tvCurrent: TextView
    private lateinit var tvScore: TextView
    private lateinit var edtInput: EditText
    private lateinit var btnCheck: Button
    private lateinit var btnStart: Button

    private var score = 0
    private var currentWord = "사과"

    // Danh sách từ tiếng Hàn
    private val words = listOf(
        "바다", "학교", "의자", "자동차", "라면", "커피", "사람", "강아지", "고양이", "노래",
        "오이", "우유", "바람", "하늘", "나라", "모자", "연필", "가방", "시장", "컴퓨터"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvCurrent = findViewById(R.id.tvCurrent)
        tvScore = findViewById(R.id.tvScore)
        edtInput = findViewById(R.id.edtInput)
        btnCheck = findViewById(R.id.btnCheck)
        btnStart = findViewById(R.id.btnStart)

        tvCurrent.text = currentWord

        btnStart.setOnClickListener {
            startGame()
        }

        btnCheck.setOnClickListener {
            checkWord()
        }
    }

    private fun startGame() {
        score = 0
        tvScore.text = "점수: $score"
        currentWord = words.random()
        tvCurrent.text = currentWord
        edtInput.setText("")
    }

    private fun checkWord() {
        val userWord = edtInput.text.toString().trim()

        if (userWord.isEmpty()) {
            Toast.makeText(this, "단어를 입력하세요!", Toast.LENGTH_SHORT).show()
            return
        }

        // chữ cuối của từ hiện tại
        val lastChar = currentWord.last()

        // chữ đầu của từ người chơi nhập
        val firstChar = userWord.first()

        if (firstChar == lastChar) {
            score++
            tvScore.text = "점수: $score"
            currentWord = userWord
            tvCurrent.text = currentWord
            edtInput.setText("")
        } else {
            Toast.makeText(this, "틀렸습니다!", Toast.LENGTH_SHORT).show()
        }
    }
}