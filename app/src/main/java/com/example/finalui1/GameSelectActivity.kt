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
                putExtra("GAME_TITLE", "슈팅 게임")
                putExtra("GAME_DESCRIPTION", "하늘에서 떨어지는 올바른 단어를 맞춰 점수를 획득하는 게임입니다.")
                putExtra("GAME_IMAGE", android.R.drawable.sym_def_app_icon) // TODO: Replace with actual image
                putExtra("GAME_CLASS", com.example.shootinggame.MainActivity::class.java)
            }
            startActivity(intent)
        }

        btnIconWord.setOnClickListener {
            val intent = Intent(this, GameDetailActivity::class.java).apply {
                putExtra("GAME_TITLE", "아이콘 단어 게임")
                putExtra("GAME_DESCRIPTION", "제시된 아이콘(그림)을 보고 해당하는 한글 단어를 맞추는 게임입니다.")
                putExtra("GAME_IMAGE", android.R.drawable.sym_def_app_icon) // TODO: Replace with actual image
                putExtra("GAME_CLASS", com.example.iconwordgame.MainActivity::class.java)
            }
            startActivity(intent)
        }

        btnGridGame.setOnClickListener {
            val intent = Intent(this, GameDetailActivity::class.java).apply {
                putExtra("GAME_TITLE", "한글 그리드 게임")
                putExtra("GAME_DESCRIPTION", "격자판에 숨겨진 한글 단어를 찾아 완성하는 게임입니다.")
                putExtra("GAME_IMAGE", android.R.drawable.sym_def_app_icon) // TODO: Replace with actual image
                putExtra("GAME_CLASS", com.example.hangulgrid.MainActivity::class.java)
            }
            startActivity(intent)
        }
    }
}
