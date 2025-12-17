package com.example.hangeulstudy

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.hangeulstudy.databinding.ActivityDifficultySelectBinding

class DifficultySelectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDifficultySelectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDifficultySelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnEasy.setOnClickListener {
            startStudy(Difficulty.EASY)
        }

        binding.btnMedium.setOnClickListener {
            startStudy(Difficulty.MEDIUM)
        }

        binding.btnHard.setOnClickListener {
            startStudy(Difficulty.HARD)
        }

        binding.btnRandom.setOnClickListener {
            startStudy(Difficulty.RANDOM)
        }
    }
    private fun startStudy(difficulty: Difficulty) {
        val intent = Intent(this, StudyActivity::class.java)
        intent.putExtra("difficulty", difficulty.name)
        startActivity(intent)
        finish()
    }
}
