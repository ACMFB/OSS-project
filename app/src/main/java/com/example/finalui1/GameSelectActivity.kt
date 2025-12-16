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
            startActivity(
                Intent(this, com.example.shootinggame.MainActivity::class.java)
            )
        }

        btnIconWord.setOnClickListener {
            startActivity(
                Intent(this, com.example.iconwordgame.MainActivity::class.java)
            )
        }

        btnGridGame.setOnClickListener {
            startActivity(
                Intent(this, com.example.hangulgrid.MainActivity::class.java)
            )
        }
    }
}
