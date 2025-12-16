package com.example.hangeulstudy

import android.app.Activity
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

    private var studyMode: String? = null
    private var reviewWords: MutableList<Word> = mutableListOf()
    private var reviewIndex = 0

    private val categories = listOf(
        "Emotion", "Action", "Personality", "Weather", "Food Ingredient",
        "Place", "Hobby", "Nature", "State", "Time Expression"
    )
    private var lastCategory: String? = null
    private var selectedDifficulty: Difficulty = Difficulty.RANDOM
    private lateinit var binding: ActivityStudyBinding
    private val gptRepo = GPTRepository()
    private val usedWords = mutableSetOf<String>()
    private lateinit var tts: TextToSpeech
    private var isTtsReady = false

    private var currentWord: Word? = null
    private val favoriteWords = mutableSetOf<Word>()

    private val favoritesActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            handleFavoritesUpdate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        studyMode = intent.getStringExtra("study_mode")
        favoriteWords.addAll(FavoritesStorage.load(this))

        tts = TextToSpeech(this, this)
        setSpeakButtonEnabled(false)

        if (studyMode == "review") {
            setupReviewMode()
        } else {
            setupNormalMode()
        }

        binding.btnNext.setOnClickListener {
            if (studyMode == "review") {
                fetchNextReviewWord()
            } else {
                fetchNewWord()
            }
        }
        binding.btnSpeak.setOnClickListener { speakOut() }
        binding.btnFavorite.setOnClickListener { toggleFavorite() }
        binding.btnShowFavorites.setOnClickListener {
            val intent = Intent(this, FavoritesActivity::class.java)
            intent.putParcelableArrayListExtra("favoriteWords", ArrayList(favoriteWords))
            favoritesActivityLauncher.launch(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        if (studyMode != "review") {
             handleFavoritesUpdate()
        }
    }

    private fun handleFavoritesUpdate() {
        val previouslyFavoritedCount = favoriteWords.size
        favoriteWords.clear()
        favoriteWords.addAll(FavoritesStorage.load(this))

        if (studyMode == "review") {
            reviewWords.clear()
            reviewWords.addAll(favoriteWords)
            reviewWords.shuffle()
            if(reviewIndex >= reviewWords.size) reviewIndex = 0
            fetchNextReviewWord()
        } else {
            currentWord?.isBookmarked = favoriteWords.any { it.korean == currentWord?.korean }
            updateFavoriteButtonState()
        }

        if (favoriteWords.size < previouslyFavoritedCount) {
            // A word might have been removed
        }
    }

    private fun setupReviewMode() {
        binding.btnShowFavorites.visibility = android.view.View.GONE
        reviewWords.addAll(favoriteWords)
        reviewWords.shuffle()
        fetchNextReviewWord()
    }

    private fun setupNormalMode() {
        selectedDifficulty = intent.getStringExtra("difficulty")
            ?.let { Difficulty.valueOf(it) }
            ?: Difficulty.RANDOM
        fetchNewWord()
    }

    private fun fetchNextReviewWord() {
        if (reviewWords.isEmpty()) {
            showError(getString(R.string.review_no_favorites))
            binding.btnNext.isEnabled = false
            return
        }

        if (reviewIndex >= reviewWords.size) {
            Toast.makeText(this, getString(R.string.review_all_words_completed), Toast.LENGTH_SHORT).show()
            reviewIndex = 0
        }

        currentWord = reviewWords[reviewIndex]
        currentWord?.isBookmarked = true
        updateUi(currentWord!!)
        reviewIndex++
    }

    private fun toggleFavorite() {
        currentWord?.let { word ->
            word.isBookmarked = !word.isBookmarked

            if (word.isBookmarked) {
                favoriteWords.add(word)
            } else {
                favoriteWords.remove(word)
                if (studyMode == "review") {
                    reviewWords.remove(word)
                    if (reviewIndex > 0) reviewIndex--
                }
            }

            FavoritesStorage.save(this, favoriteWords)
            updateFavoriteButtonState()

            Toast.makeText(
                this,
                if (word.isBookmarked) getString(R.string.added_to_favorites) else getString(R.string.removed_from_favorites),
                Toast.LENGTH_SHORT
            ).show()

            if (studyMode == "review" && reviewWords.isEmpty()) {
                fetchNextReviewWord()
            }
        }
    }

    private fun updateFavoriteButtonState() {
        val isFavorite = currentWord?.isBookmarked ?: false
        binding.btnFavorite.setImageResource(
            if (isFavorite) android.R.drawable.btn_star_big_on
            else android.R.drawable.btn_star_big_off
        )
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
                val randomCategory = categories.filter { it != lastCategory }.random()
                lastCategory = randomCategory
                val difficultyEnum = if (selectedDifficulty == Difficulty.RANDOM) {
                    listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD).random()
                } else {
                    selectedDifficulty
                }
                val difficultyLabel = difficultyEnum.label
                val prompt = """You are an API that creates Korean word quizzes.

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
사랑:love:나는 너를 정말 사랑해.""".trimIndent()

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

                    if (usedWords.contains(newWord.korean) || containsEnglish(newWord.example)) {
                        showError("Invalid word. Retrying...")
                        fetchNewWord()
                        return@launch
                    }

                    newWord.isBookmarked = favoriteWords.any { it.korean == newWord.korean }
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
        return text.any { it in 'a'..'z' || it in 'A'..'Z' }
    }

    private fun updateUi(word: Word) {
        binding.txtKorean.text = word.korean
        binding.txtMeaning.text = word.meaning
        binding.txtExample.text = word.example
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
        binding.txtExample.text = if (studyMode == "review") "" else "Check internet or API key."
        binding.btnNext.isEnabled = studyMode != "review"
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
