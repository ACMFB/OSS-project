package com.example.finalui1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class GameSelectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_select)

        val btnShooting = findViewById<Button>(R.id.btnShootingGame)
        val btnIconWord = findViewById<Button>(R.id.btnIconWordGame)
        val btnGridGame = findViewById<Button>(R.id.btnGridGame)

        btnShooting.setOnClickListener {
            val intent = Intent(this, GameDetailActivity::class.java).apply {
                putExtra("GAME_TITLE_RES_ID", R.string.title_shooting_game)
                putExtra("GAME_DESCRIPTION_RES_ID", R.string.desc_shooting_game)
                putExtra("GAME_IMAGE", R.drawable.s_game_icon) // 아이콘 변경
                putExtra("GAME_CLASS", com.example.shootinggame.MainActivity::class.java)
            }
            startActivity(intent)
        }

        btnIconWord.setOnClickListener {
            val intent = Intent(this, GameDetailActivity::class.java).apply {
                putExtra("GAME_TITLE_RES_ID", R.string.title_icon_word_game)
                putExtra("GAME_DESCRIPTION_RES_ID", R.string.desc_icon_word_game)
                putExtra("GAME_IMAGE", R.drawable.icon_word_game_icon) // 아이콘 변경
                putExtra("GAME_CLASS", com.example.iconwordgame.MainActivity::class.java)
            }
            startActivity(intent)
        }

        btnGridGame.setOnClickListener {
            val intent = Intent(this, GameDetailActivity::class.java).apply {
                putExtra("GAME_TITLE_RES_ID", R.string.title_grid_game)
                putExtra("GAME_DESCRIPTION_RES_ID", R.string.desc_grid_game)
                putExtra("GAME_IMAGE", R.drawable.grid_game_icon) // 아이콘 변경
                putExtra("GAME_CLASS", com.example.hangulgrid.MainActivity::class.java)
            }
            startActivity(intent)
        }
    }
}
