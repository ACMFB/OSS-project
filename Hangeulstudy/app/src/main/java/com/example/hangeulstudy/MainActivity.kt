package com.example.hangeulstudy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.hangeulstudy.databinding.ActivityMainBinding
import com.example.hangeulstudy.Word
import com.example.hangeulstudy.GPTRepository
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val gptRepo = GPTRepository()
    private val usedWords = mutableSetOf<String>()   // 중복 방지

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fetchNewWord()

        binding.btnNext.setOnClickListener {
            fetchNewWord()
        }
    }

    private fun fetchNewWord() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val responseText = gptRepo.askGPT(
                    """
                    아래 카테고리 중 하나를 무작위로 선택해.
                    [감정, 동작, 성격, 날씨, 음식재료, 장소, 취미, 자연, 상태, 시간 표현]

                    한국어 단어 하나를 고르고 영어 뜻과 예문을 '단어:영어뜻:한글예문' 형식으로 출력해.
                    조건:
                    1) 이전에 나온 단어와 절대 중복 불가
                    2) 같은 의미군도 금지
                    3) 예문은 무조건 해당 단어가 들어갈 것
                    4) 설명 없이 형식만 출력
                    """.trimIndent()
                )

                val parts = responseText.split(":")
                if (parts.size >= 3) {
                    val word = Word(
                        korean = parts[0].trim(),
                        meaning = parts[1].trim(),
                        example = parts.subList(2, parts.size).joinToString(":").trim()
                    )

                    // 중복 방지
                    if (usedWords.contains(word.korean)) {
                        fetchNewWord()
                        return@launch
                    }

                    usedWords.add(word.korean)
                    updateUi(word)
                } else {
                    showError("응답 형식 오류")
                }
            } catch (e: Exception) {
                showError(e.message ?: "API 오류")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun updateUi(word: Word) {
        binding.txtKorean.text = word.korean
        binding.txtMeaning.text = word.meaning
        binding.txtExample.text = word.example
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.btnNext.isEnabled = false
            binding.txtKorean.text = "생성 중..."
            binding.txtMeaning.text = ""
            binding.txtExample.text = ""
        } else {
            binding.btnNext.isEnabled = true
        }
    }

    private fun showError(message: String) {
        binding.txtKorean.text = "오류"
        binding.txtMeaning.text = message
        binding.txtExample.text = "인터넷 또는 API 키를 확인하세요."
    }
}
