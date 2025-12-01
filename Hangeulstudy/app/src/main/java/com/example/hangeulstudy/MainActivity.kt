package com.example.hangeulstudy

import android.graphics.Color
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.lifecycleScope
import com.example.hangeulstudy.databinding.ActivityMainBinding
import com.example.hangeulstudy.Word
import com.example.hangeulstudy.GPTRepository
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityMainBinding
    private val gptRepo = GPTRepository()
    private val usedWords = mutableSetOf<String>()   // 중복 방지
    private lateinit var tts: TextToSpeech // TTS 엔진 추가
    private var isTtsReady = false // TTS 준비 상태 플래그

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tts = TextToSpeech(this, this) // TTS 초기화

        // 버튼 초기 상태 설정 (비활성화, 회색)
        setSpeakButtonEnabled(false)

        fetchNewWord()

        binding.btnNext.setOnClickListener {
            fetchNewWord()
        }

        // '소리 듣기' 버튼을 클릭하면 단어를 읽어주도록 변경
        binding.btnSpeak.setOnClickListener {
            speakOut()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // 한국어 설정
            val result = tts.setLanguage(Locale.KOREAN)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "The Language specified is not supported!")
                Toast.makeText(this, "한국어 음성 데이터가 없습니다.", Toast.LENGTH_SHORT).show()
            } else {
                // TTS 준비 완료, 버튼 활성화 (초록색)
                setSpeakButtonEnabled(true)
                isTtsReady = true
                // TTS 재생 상태를 감지하는 리스너 설정
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        // 음성 출력이 시작되면 버튼 비활성화 (회색)
                        runOnUiThread {
                            setSpeakButtonEnabled(false)
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        // 음성 출력이 끝나면 버튼 활성화 (초록색)
                        runOnUiThread {
                            setSpeakButtonEnabled(true)
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        // 오류 발생 시 버튼 활성화 (초록색)
                        runOnUiThread {
                            setSpeakButtonEnabled(true)
                        }
                    }
                })
            }
        } else {
            Log.e("TTS", "Initialization Failed!")
        }
    }

    private fun setSpeakButtonEnabled(isEnabled: Boolean) {
        binding.btnSpeak.isEnabled = isEnabled
        val color = if (isEnabled) Color.parseColor("#4CAF50") else Color.GRAY
        val drawable = DrawableCompat.wrap(binding.btnSpeak.drawable)
        DrawableCompat.setTint(drawable, color)
        binding.btnSpeak.setImageDrawable(drawable)
    }

    private fun speakOut() {
        if (!isTtsReady) {
            Toast.makeText(this, "음성 기능이 아직 준비되지 않았습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val text = binding.txtKorean.text.toString()
        // 생성 중... 또는 오류 메시지는 읽지 않도록 처리
        if (text.isNotEmpty() && text != "생성 중..." && text != "오류") {
            // 각 음성 출력에 고유 ID 부여
            val utteranceId = UUID.randomUUID().toString()
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    private fun fetchNewWord() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val usedWordsString = if (usedWords.isEmpty()) "없음" else usedWords.joinToString(", ")
                val prompt = """
                    너는 한국어 단어 퀴즈를 내는 API야.
                    내가 요청하면, 너는 반드시 '한국어단어:영어뜻:한국어예문' 형식으로만 응답해야 해.
                    다른 어떤 설명, 카테고리, 품사, 줄바꿈도 추가하면 안돼. 오직 '단어:뜻:예문' 형식만 허용돼.

                    아래 카테고리 중 하나를 무작위로 선택해서 단어를 골라줘:
                    [감정, 동작, 성격, 날씨, 음식재료, 장소, 취미, 자연, 상태, 시간 표현]

                    그리고 다음 단어 목록에 있는 단어는 절대로 사용하면 안돼:
                    [$usedWordsString]

                    다시 한 번 강조할게. 너의 응답은 반드시 '단어:뜻:예문' 형식이어야 해. 예를 들어 '사랑:love:나는 너를 사랑해.' 처럼 말이야.
                    """.trimIndent()

                val responseText = gptRepo.askGPT(prompt)

                // 모델이 추가적인 정보를 포함해서 응답할 경우, 마지막 줄만 파싱하도록 수정
                val lastLine = responseText.lines().lastOrNull { it.contains(":") } ?: responseText
                val parts = lastLine.split(":")

                if (parts.size >= 3) {
                    val word = Word(
                        korean = parts[0].trim(),
                        meaning = parts[1].trim(),
                        example = parts.subList(2, parts.size).joinToString(":").trim()
                    )

                    // 중복 방지 로직 (혹시 모를 경우를 대비해 유지)
                    if (usedWords.contains(word.korean)) {
                        showError("새로운 단어를 찾지 못했습니다. 다시 시도해주세요.")
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
        binding.btnNext.isEnabled = true // 오류 발생 시 버튼 활성화
    }

    override fun onDestroy() {
        // 앱이 종료될 때 TTS 자원 해제
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}
