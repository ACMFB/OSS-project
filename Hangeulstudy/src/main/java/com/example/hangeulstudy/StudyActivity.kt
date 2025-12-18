package com.example.hangeulstudy

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.lifecycleScope
import com.example.hangeulstudy.data.DifficultyCache
import com.example.hangeulstudy.data.FavoritesStorage
import com.example.hangeulstudy.databinding.ActivityStudyBinding
import java.util.ArrayList
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.launch

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
    private lateinit var difficultyCache: MutableMap<String, Difficulty>

    private val favoritesActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            handleFavoritesUpdate()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startSpeechToText()
        } else {
            Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_SHORT).show()
        }
    }

    private val sttLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText: ArrayList<String>? =
                result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!spokenText.isNullOrEmpty()) {
                val recognizedText = spokenText[0]
                val correctWord = currentWord?.korean

                if (recognizedText.equals(correctWord, ignoreCase = true)) {
                    Toast.makeText(this, getString(R.string.stt_correct, recognizedText), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getString(R.string.stt_try_again, recognizedText), Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        studyMode = intent.getStringExtra("study_mode")
        favoriteWords.addAll(FavoritesStorage.load(this))
        difficultyCache = DifficultyCache.load(this)

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
        binding.btnPronounce.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startSpeechToText()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
        binding.btnFavorite.setOnClickListener { toggleFavorite() }
        binding.btnShowFavorites.setOnClickListener {
            val intent = Intent(this, FavoritesActivity::class.java)
            intent.putParcelableArrayListExtra("favoriteWords", ArrayList(favoriteWords))
            favoritesActivityLauncher.launch(intent)
        }
    }

    private fun startSpeechToText() {
        if (currentWord == null) {
            Toast.makeText(this, getString(R.string.no_word_to_practice), Toast.LENGTH_SHORT).show()
            return
        }
        val sttIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREAN.toString())
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.stt_prompt, currentWord?.korean))
        }
        try {
            sttLauncher.launch(sttIntent)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.stt_not_available), Toast.LENGTH_SHORT).show()
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
    private fun inferDifficulty(word: String): Difficulty {
        return when {
            word.length <= 2 -> Difficulty.EASY
            word.length <= 3 -> Difficulty.MEDIUM
            else -> Difficulty.HARD
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
    private fun isValidForDifficulty(word: String, difficulty: Difficulty): Boolean {
        return when (difficulty) {
            Difficulty.EASY -> word.length <= 2
            Difficulty.MEDIUM -> word.length in 3..4
            Difficulty.HARD -> word.length >= 4 &&  !word.endsWith("하다") &&
                    !word.endsWith("되다") &&
                    !word.endsWith("있다") &&
                    !word.endsWith("없다")
            Difficulty.RANDOM -> true
        }
    }
    private fun speakOut() {
        if (!isTtsReady) {
            Toast.makeText(this, getString(R.string.tts_not_ready), Toast.LENGTH_SHORT).show()
            return
        }

        currentWord?.korean?.let { text ->
            if (text.isNotEmpty() && text != getString(R.string.generating_word) && text != getString(R.string.error_message)) {
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

                val difficultyGuideline = when (difficultyEnum) {
                    Difficulty.EASY -> "- Difficulty Guideline: Easy words are common in everyday conversation (e.g., 사랑, 학교, 먹다)."
                    Difficulty.MEDIUM -> "- Difficulty Guideline: Medium words are more specific or less frequent (e.g., 소중하다, 문화, 발전)."
                    Difficulty.HARD -> "- Difficulty Guideline: Hard words are academic, technical, or archaic (e.g., 고고학, 형이상학, 변증법)."
                    else -> difficultyEnum // Handle RANDOM or any other case
                }

                val prompt = """You are an API that creates Korean word quizzes.

You MUST follow ALL rules strictly.

1. Respond ONLY in this exact format:
   KoreanWord:EnglishMeaning:KoreanExample

2. The KoreanExample MUST be written ONLY in Korean.
   - DO NOT include English words.
   - DO NOT mix languages.

3. Do NOT add explanations, symbols, or line breaks.

For HARD difficulty:
- Avoid common daily conversation words
- Prefer academic, abstract, or technical vocabulary

Pick ONE Korean word that matches:
- Category: $randomCategory
- Difficulty level: $difficultyLabel
${difficultyGuideline}

Never use these words:
[$usedWordsString]

Example of a valid response:
사랑:love:나는 너를 정말 사랑해.""".trimIndent()

                val responseText = gptRepo.askGPT(prompt)
                val lastLine = responseText.lines().lastOrNull { it.contains(":") } ?: responseText
                val parts = lastLine.split(":")

                if (parts.size >= 3) {
                    val wordName = parts[0].trim()
                    val meaning = parts[1].trim()
                    val example = parts.subList(2, parts.size).joinToString(":").trim()

                    if (usedWords.contains(wordName) || containsEnglish(example)) {
                        showError(getString(R.string.error_message))
                        fetchNewWord()
                        return@launch
                    }

                    if (selectedDifficulty != Difficulty.RANDOM &&
                        !isValidForDifficulty(wordName, selectedDifficulty)
                    ) {
                        fetchNewWord()
                        return@launch
                    }


                    val finalDifficulty = when {
                        // 사용자가 난이도를 명시적으로 선택한 경우
                        selectedDifficulty != Difficulty.RANDOM ->
                            selectedDifficulty

                        // RANDOM일 때만 캐시 사용
                        difficultyCache.containsKey(wordName) ->
                            difficultyCache[wordName]!!

                        // RANDOM + 캐시 없음
                        else ->
                            inferDifficulty(wordName)
                    }

                    if (selectedDifficulty == Difficulty.RANDOM &&
                        !difficultyCache.containsKey(wordName)
                    ) {
                        difficultyCache[wordName] = finalDifficulty
                        DifficultyCache.save(this@StudyActivity, difficultyCache)
                    }

                    val newWord = Word(
                        korean = wordName,
                        meaning = meaning,
                        example = example,
                        difficulty = finalDifficulty
                    )

                    newWord.isBookmarked = favoriteWords.any { it.korean == wordName }

                    usedWords.add(wordName)
                    currentWord = newWord
                    updateUi(newWord)
                }
                else {
                    showError(getString(R.string.error_message))
                }
            } catch (e: Exception) {
                showError(e.message ?: getString(R.string.error_message))
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

        binding.txtDifficulty.text = getString(R.string.difficulty_label, word.difficulty.displayName)
        binding.txtDifficulty.setBackgroundColor(word.difficulty.color)

        updateFavoriteButtonState()
    }


    private fun showLoading(isLoading: Boolean) {
        binding.btnNext.isEnabled = !isLoading
        binding.btnFavorite.isEnabled = !isLoading
        if (isLoading) {
            binding.txtKorean.text = getString(R.string.generating_word)
            binding.txtMeaning.text = ""
            binding.txtExample.text = ""
        }
    }

    private fun showError(message: String) {
        binding.txtKorean.text = getString(R.string.error_message)
        binding.txtMeaning.text = message
        binding.txtExample.text = if (studyMode == "review") "" else getString(R.string.error_check_connection)
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
