package com.example.iconwordgame

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            IconWordGameScreen()
        }
    }
}

@Composable
fun IconWordGameScreen() {
    val context = LocalContext.current

    // 상태 변수들
    var score by remember { mutableStateOf(0) }
    var currentWord by remember { mutableStateOf("사과") }
    var userInput by remember { mutableStateOf("") }

    // 단어 목록
    val words = listOf(
        "바다", "학교", "의자", "자동차", "라면", "커피", "사람", "강아지", "고양이", "노래",
        "오이", "우유", "바람", "하늘", "나라", "모자", "연필", "가방", "시장", "컴퓨터"
    )

    fun startGame() {
        score = 0
        currentWord = words.random()
        userInput = ""
    }

    fun checkWord() {
        if (userInput.isBlank()) {
            Toast.makeText(context, "단어를 입력하세요!", Toast.LENGTH_SHORT).show()
            return
        }

        val lastChar = currentWord.last()
        val firstChar = userInput.first()

        if (firstChar == lastChar) {
            score++
            currentWord = userInput
            userInput = ""
        } else {
            Toast.makeText(context, "틀렸습니다!", Toast.LENGTH_SHORT).show()
        }
    }

    // UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "현재 단어",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = currentWord,
            fontSize = 32.sp,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Text(
            text = "점수: $score",
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = userInput,
            onValueChange = { userInput = it },
            label = { Text("단어 입력") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Button(onClick = { startGame() } ) {
                Text("게임 시작")
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(onClick = { checkWord() } ) {
                Text("확인")
            }
        }
    }
}
