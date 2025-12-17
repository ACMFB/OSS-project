package com.example.hangeulstudy

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hangeulstudy.data.FavoritesStorage
import com.example.hangeulstudy.databinding.ActivityFavoritesBinding

class FavoritesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavoritesBinding
    private val favoriteWords = mutableListOf<Word>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "My Words"

        val initialWords = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("favoriteWords", Word::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Word>("favoriteWords")
        }
        initialWords?.let { favoriteWords.addAll(it) }

        val adapter = WordAdapter(favoriteWords) { wordToDelete ->
            val position = favoriteWords.indexOf(wordToDelete)
            if (position != -1) {
                favoriteWords.removeAt(position)
                binding.recyclerView.adapter?.notifyItemRemoved(position)
            }
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAndSave()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun finishAndSave() {
        // Save the final state of favorites to storage
        FavoritesStorage.save(this, favoriteWords.toSet())
        setResult(Activity.RESULT_OK) // Signal that changes were made
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finishAndSave()
        return true
    }
}
