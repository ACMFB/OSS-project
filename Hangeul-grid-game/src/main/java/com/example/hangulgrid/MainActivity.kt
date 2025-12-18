package com.example.hangulgrid

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HangulGameScreen()
        }
    }
}

// ==============================
//   GAME DATA
// ==============================

val consonants = listOf("ㄱ","ㄲ","ㄴ","ㄷ","ㄸ","ㄹ","ㅁ","ㅂ","ㅃ","ㅅ","ㅆ","ㅇ","ㅈ","ㅉ","ㅊ","ㅋ","ㅌ","ㅍ","ㅎ")
val vowels = listOf(
    "ㅏ","ㅐ","ㅑ","ㅒ","ㅓ","ㅔ","ㅕ","ㅖ","ㅗ","ㅘ","ㅙ","ㅚ",
    "ㅛ","ㅜ","ㅝ","ㅞ","ㅟ","ㅠ","ㅡ","ㅢ","ㅣ"
)
val pool = consonants + vowels

val praiseMessages = listOf("잘했어요!", "정답입니다!", "완벽해요!", "멋져요!", "좋아요!")

val wordList = listOf(
    "산","물","불","달","별","손","눈","밤","밥","집",
    "길","꽃","꿈","강","빛","차","안","열","힘","점"
)

// ==============================
//   HANGUL DECOMPOSE
// ==============================

fun decomposeHangul(s: String): List<String> {
    val ch = s[0].code
    if (ch !in 0xAC00..0xD7A3) return listOf(s)

    val base = ch - 0xAC00
    val choIdx = base / 588
    val jungIdx = (base % 588) / 28
    val jongIdx = base % 28

    val CHO = consonants
    val JUNG = vowels
    val JONG = listOf(
        "", "ㄱ","ㄲ","ㄳ","ㄴ","ㄵ","ㄶ","ㄷ","ㄹ","ㄺ","ㄻ","ㄼ","ㄽ","ㄾ","ㄿ","ㅀ","ㅁ",
        "ㅂ","ㅄ","ㅅ","ㅆ","ㅇ","ㅈ","ㅊ","ㅋ","ㅌ","ㅍ","ㅎ"
    )

    val res = mutableListOf<String>()
    res.add(CHO[choIdx])
    res.add(JUNG[jungIdx])
    if (JONG[jongIdx].isNotEmpty()) res.add(JONG[jongIdx])

    return res
}

fun makeBoard(parts: List<String>): List<List<String>> {
    val letters = MutableList(25) { pool.random() }

    for (p in parts) {
        if (!letters.contains(p)) {
            val idx = Random.nextInt(letters.size)
            letters[idx] = p
        }
    }
    return letters.chunked(5)
}

// ==============================
//   GAME UI
// ==============================

@Composable
fun HangulGameScreen() {

    val context = LocalContext.current

    var target by remember { mutableStateOf(wordList.random()) }
    var parts by remember { mutableStateOf(decomposeHangul(target)) }
    var board by remember { mutableStateOf(makeBoard(parts)) }

    var index by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var timeLeft by remember { mutableStateOf(60) }

    var praise by remember { mutableStateOf("") }
    var highlight by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var glowColor by remember { mutableStateOf(Color.Transparent) }

    // Timer and game over logic
    LaunchedEffect(timeLeft) {
        if (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        } else {
            // Time is up, go to result screen
            val intent = Intent(context, SecondActivity::class.java)
            intent.putExtra("score", score)
            context.startActivity(intent)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        // Top Info Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.score, score), fontSize = 22.sp)
            Text(stringResource(R.string.remaining_time, timeLeft), fontSize = 22.sp, color = if (timeLeft <= 10) Color.Red else Color.Black)
        }

        Spacer(Modifier.height(10.dp))

        Text(stringResource(R.string.presented_word, target), fontSize = 24.sp, color = Color(0xFF0066CC), modifier = Modifier.align(Alignment.CenterHorizontally))
        
        Spacer(Modifier.height(10.dp))

        if (praise.isNotEmpty()) {
            Text(
                praise,
                color = Color(0xFF22AA22),
                fontSize = 32.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(10.dp))
        }

        // Game Board
        Column {
            for (y in 0 until 5) {
                Row {
                    for (x in 0 until 5) {
                        val letter = board[y][x]
                        val isGlow = highlight?.first == x && highlight?.second == y

                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .padding(4.dp)
                                .background(
                                    if (isGlow) glowColor else Color(0xFFECECEC),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable(enabled = timeLeft > 0) { // Game over when time is 0
                                    val need = parts[index]

                                    if (letter == need) {
                                        glowColor = Color(0xFF88FF88)
                                        highlight = x to y
                                        index++

                                        if (index >= parts.size) {
                                            score++
                                            praise = praiseMessages.random()

                                            // The game continues until the time runs out
                                            target = wordList.random()
                                            parts = decomposeHangul(target)
                                            board = makeBoard(parts)
                                            index = 0
                                        }

                                    } else {
                                        glowColor = Color(0xFFFF8888)
                                        highlight = x to y
                                        index = 0
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(letter, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
