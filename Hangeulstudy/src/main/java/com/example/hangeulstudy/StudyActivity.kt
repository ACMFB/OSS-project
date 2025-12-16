package com.example.hangeulstudy

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.DrawableCompat
import java.util.Locale
import java.util.UUID
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import com.example.hangeulstudy.data.FavoritesStorage
import kotlinx.coroutines.launch
import com.example.hangeulstudy.databinding.ActivityStudyBinding

class StudyActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    // 카테고리 목록
    private val categories = listOf(
        "Emotion",
        "Action",
        "Personality",
        "Weather",
        "Food Ingredient",
        "Place",
        "Hobby",
        "Nature",
        "State",
        "Time Expression"
    )

    // 이전 카테고리 기억 (연속 방지용)
    private var lastCategory: String? = null

    private var selectedDifficulty: Difficulty = Difficulty.RANDOM
    private lateinit var binding: ActivityStudyBinding
    private val gptRepo = GPTRepository()
    private val usedWords = mutableSetOf<String>()
    private lateinit var tts: TextToSpeech
    private var isTtsReady = false

    private var currentWord: Word? = null
    private val favoriteWords = mutableSetOf<Word>()

    // ActivityResultLauncher for FavoritesActivity
    private val favoritesActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val deletedWords = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableArrayListExtra("deletedWords", Word::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableArrayListExtra<Word>("deletedWords")
            }

            if (deletedWords != null) {
                favoriteWords.removeAll(deletedWords.toSet())
                updateFavoriteButtonState() // Update star icon on return
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        favoriteWords.addAll(
            FavoritesStorage.load(this)
        )

        selectedDifficulty = intent.getStringExtra("difficulty")
            ?.let { Difficulty.valueOf(it) }
            ?: Difficulty.RANDOM

        tts = TextToSpeech(this, this)
        setSpeakButtonEnabled(false)

        fetchNewWord()

        binding.btnNext.setOnClickListener { fetchNewWord() }
        binding.btnSpeak.setOnClickListener { speakOut() }
        binding.btnFavorite.setOnClickListener { toggleFavorite() }

        binding.btnShowFavorites.setOnClickListener {
            val intent = Intent(this, FavoritesActivity::class.java)
            intent.putParcelableArrayListExtra("favoriteWords", ArrayList(favoriteWords))
            favoritesActivityLauncher.launch(intent)
        }
    }

    private fun toggleFavorite() {
        currentWord?.let { word ->
            if (favoriteWords.contains(word)) {
                favoriteWords.remove(word)
            } else {
                favoriteWords.add(word)
            }

            FavoritesStorage.save(this, favoriteWords)
            updateFavoriteButtonState()

            Toast.makeText(
                this,
                if (favoriteWords.contains(word)) "Added to favorites"
                else "Removed from favorites",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    private fun updateFavoriteButtonState() {
        currentWord?.let { word ->
            val isFavorite = favoriteWords.contains(word)
            binding.btnFavorite.setImageResource(
                if (isFavorite) android.R.drawable.btn_star_big_on
                else android.R.drawable.btn_star_big_off
            )
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.KOREAN)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "The Language specified is not supported!")
            } else {
                setSpeakButtonEnabled(true)
                isTtsReady = true
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { runOnUiThread { setSpeakButtonEnabled(false) } }
                    override fun onDone(utteranceId: String?) { runOnUiThread { setSpeakButtonEnabled(true) } }
                    override fun onError(utteranceId: String?) { runOnUiThread { setSpeakButtonEnabled(true) } }
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
            Toast.makeText(this, "TTS is not ready yet.", Toast.LENGTH_SHORT).show()
            return
        }

        currentWord?.korean?.let { text ->
            if (text.isNotEmpty() && text != "Generating..." && text != "Error") {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
            }
        }
    }

    private fun fetchNewWord() {
        showLoading(true)
        currentWord = null
        updateFavoriteButtonState()

        lifecycleScope.launch {
            try {
                val usedWordsString = if (usedWords.isEmpty()) "none" else usedWords.joinToString(", ")

                // 🔹 카테고리 랜덤 (연속 방지)
                val randomCategory = categories
                    .filter { it != lastCategory }
                    .random()
                lastCategory = randomCategory

                // 🔹 난이도 랜덤
                val difficultyEnum = if (selectedDifficulty == Difficulty.RANDOM) {
                    listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD).random()
                } else {
                    selectedDifficulty
                }

                val difficultyLabel = difficultyEnum.label


                val prompt = """
You are an API that creates Korean word quizzes.

You MUST follow ALL rules strictly.

1. Respond ONLY in this exact format:
   KoreanWord:EnglishMeaning:KoreanExample

2. The KoreanExample MUST be written ONLY in Korean.
   - DO NOT include English words.
   - DO NOT mix languages.
   - If English appears, the response is INVALID.

3. Do NOT add explanations, symbols, or line breaks.

Pick ONE Korean word that matches:
- Category: $randomCategory
- Difficulty level: $difficultyLabel

Never use these words:
[$usedWordsString]

Example of a valid response:
사랑:love:나는 너를 정말 사랑해.
""".trimIndent()

                val responseText = gptRepo.askGPT(prompt)
                val lastLine = responseText.lines().lastOrNull { it.contains(":") } ?: responseText
                val parts = lastLine.split(":")

                if (parts.size >= 3) {
                    val newWord = Word(
                        korean = parts[0].trim(),
                        meaning = parts[1].trim(),
                        example = parts.subList(2, parts.size).joinToString(":").trim(),
                        difficulty = difficultyEnum
                    )

                    // 🔒 중복 + 영어 검증
                    if (usedWords.contains(newWord.korean) || containsEnglish(newWord.example)) {
                        showError("Invalid word generated. Please try again.")
                        return@launch
                    }

                    usedWords.add(newWord.korean)
                    currentWord = newWord
                    updateUi(newWord)
                } else {
                    showError("Invalid response format")
                }
            } catch (e: Exception) {
                showError(e.message ?: "API Error")
            } finally {
                showLoading(false)
            }
        }
    }
    private fun containsEnglish(text: String): Boolean {
        return text.any { it in 'A'..'Z' || it in 'a'..'z' }
    }


    private fun updateUi(word: Word) {
        binding.txtKorean.text = word.korean
        binding.txtMeaning.text = word.meaning
        binding.txtExample.text = word.example

        // 난이도 UI 표시
        binding.txtDifficulty.text = "난이도: ${word.difficulty.displayName}"
        binding.txtDifficulty.setBackgroundColor(word.difficulty.color)
        updateFavoriteButtonState()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.btnNext.isEnabled = !isLoading
        binding.btnFavorite.isEnabled = !isLoading
        if (isLoading) {
            binding.txtKorean.text = "Generating..."
            binding.txtMeaning.text = ""
            binding.txtExample.text = ""
        }
    }

    private fun showError(message: String) {
        binding.txtKorean.text = "Error"
        binding.txtMeaning.text = message
        binding.txtExample.text = "Check your internet connection or API key."
        binding.btnNext.isEnabled = true
        binding.btnFavorite.isEnabled = false
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}
