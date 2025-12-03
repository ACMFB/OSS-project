package com.example.hangeulstudy

import android.app.Activity
import android.content.Intent
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
import androidx.lifecycle.lifecycleScope
import com.example.hangeulstudy.databinding.ActivityMainBinding
import com.example.hangeulstudy.GPTRepository
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityMainBinding
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
        if (result.resultCode == Activity.RESULT_OK) {
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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            updateFavoriteButtonState()
            val message = if (favoriteWords.contains(word)) "Added to favorites." else "Removed from favorites."
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
        currentWord?.korean?.let { text ->
            if (isTtsReady && text.isNotEmpty() && text != "Generating..." && text != "Error") {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
            } else if (!isTtsReady) {
                Toast.makeText(this, "TTS is not ready yet.", Toast.LENGTH_SHORT).show()
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
                val prompt =   """
                    You are an API that creates Korean word quizzes.
                    When I request, you must respond only in the format 'KoreanWord:EnglishMeaning:KoreanExample'.
                    Do not add any other explanations, categories, parts of speech, or line breaks. Only the 'Word:Meaning:Example' format is allowed.

                    Pick a word randomly from one of the categories below:
                    [Emotion, Action, Personality, Weather, Food Ingredient, Place, Hobby, Nature, State, Time Expression]

                    And you must never use the words from the following list:
                    [$usedWordsString]

                    I'll emphasize again. Your response must be in the 'Word:Meaning:Example' format. For example, '사랑:love:나는 너를 사랑해.'
                    """.trimIndent()

                val responseText = gptRepo.askGPT(prompt)
                val lastLine = responseText.lines().lastOrNull { it.contains(":") } ?: responseText
                val parts = lastLine.split(":")

                if (parts.size >= 3) {
                    val newWord = Word(
                        korean = parts[0].trim(),
                        meaning = parts[1].trim(),
                        example = parts.subList(2, parts.size).joinToString(":").trim()
                    )

                    if (usedWords.contains(newWord.korean)) {
                        showError("Could not find a new word. Please try again.")
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

    private fun updateUi(word: Word) {
        binding.txtKorean.text = word.korean
        binding.txtMeaning.text = word.meaning
        binding.txtExample.text = word.example
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
