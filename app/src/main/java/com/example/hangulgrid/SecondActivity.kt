package com.example.hangulgrid

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class SecondActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val score = intent.getIntExtra("score", 0)

        setContent {
            ResultScreen(score)
        }
    }
}

@Composable
fun ResultScreen(score: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("게임 종료!", fontSize = 32.sp)
        Spacer(Modifier.height(20.dp))
        Text("최종 점수: $score", fontSize = 28.sp)
        Spacer(Modifier.height(40.dp))

        Button(
            onClick = {
                // Restart app
                // Go back to MainActivity
            }
        ) {
            Text("다시 시작하기")
        }
    }
}
