package com.example.finalui1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class GameDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_detail)

        val gameTitleResId = intent.getIntExtra("GAME_TITLE_RES_ID", 0)
        val gameDescriptionResId = intent.getIntExtra("GAME_DESCRIPTION_RES_ID", 0)
        val gameImageResId = intent.getIntExtra("GAME_IMAGE", 0)
        val gameClass = intent.getSerializableExtra("GAME_CLASS") as Class<*>?

        val tvTitle = findViewById<TextView>(R.id.tvGameTitle)
        val ivImage = findViewById<ImageView>(R.id.ivGameImage)
        val tvDescription = findViewById<TextView>(R.id.tvGameDescription)
        val btnPlay = findViewById<Button>(R.id.btnPlay)

        if (gameTitleResId != 0) {
            tvTitle.setText(gameTitleResId)
        }
        if (gameDescriptionResId != 0) {
            tvDescription.setText(gameDescriptionResId)
        }
        if (gameImageResId != 0) {
            ivImage.setImageResource(gameImageResId)
        }

        btnPlay.setOnClickListener {
            if (gameClass != null) {
                val intent = Intent(this, gameClass)
                startActivity(intent)
            }
        }
    }
}
