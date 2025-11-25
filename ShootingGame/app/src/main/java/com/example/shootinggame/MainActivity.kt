package com.example.shootinggame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.decode.GifDecoder
import com.example.shootinggame.ui.theme.ShootingGameTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShootingGameTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ShootingGame(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

val juaFontFamily = FontFamily(Font(R.font.shooting_jua, FontWeight.Normal)) // 폰트 설정
val keywordList = listOf("사과", "바나나", "딸기", "포도", "오렌지", "수박", "레몬", "파인애플") // 임시 제시어 리스트
val keywordResetTime: Long = 10000L // 제시어 변경 주기 설정 (밀리초)
val playerMaxHp: Int = 10 // 플레이어 최대 체력 설정

@Composable
fun ShootingGame(name: String, modifier: Modifier = Modifier) {
    var pauseCheck by remember { mutableStateOf(false) } // 일시정지 상태 체크용 변수
    var currentScore by remember { mutableStateOf(0) }
    var currentKeyword by remember { mutableStateOf("") }
    var playerHp by remember { mutableStateOf(playerMaxHp) }

    val onKeywordSelected: (String) -> Unit = { newKeyword -> // RandomKeyword() 함수의 콜백으로 받은 스트링을 키워드로 저장
        currentKeyword = newKeyword
    }
    RandomKeyword( // keywordResetTime (밀리초) 마다 제시어가 keywordList 중에서 랜덤으로 선택
        keywordList = keywordList,
        keywordResetTime = keywordResetTime,
        pauseCheck = pauseCheck,
        onKeywordSelected = onKeywordSelected
    )

    CreateInterface( // 인터페이스 표시
        pauseCheck = pauseCheck,
        currentScore = currentScore,
        playerHp = playerHp,
        currentKeyword = currentKeyword,
        onPauseToggle = { pauseCheck = !pauseCheck },
        onQuitToggle = { pauseCheck = !pauseCheck } // 나중에 게임 종료 코드로 바꾸기
    )

    // 플레이어 출현 함수
    // 랜덤 확률 -> 시간 따라 적 생성 함수로 전달
}

@Composable
fun RandomKeyword(
    keywordList: List<String>,
    keywordResetTime: Long,
    pauseCheck: Boolean,
    onKeywordSelected: (String) -> Unit
) {
    LaunchedEffect(pauseCheck) {
        if (!pauseCheck) { // 일시정지 상태에서는 실행되지 않음
            while (isActive) {
                delay(keywordResetTime)
                onKeywordSelected(keywordList.random())
            }
        }
    }
}

@Composable
fun CreateInterface( // 배경 재생, 일시정지 기능 구현, 스코어 및 플레이어 체력 표시, 제시어 표시
    pauseCheck: Boolean,
    currentScore: Int,
    playerHp: Int,
    currentKeyword: String,
    onPauseToggle: () -> Unit,
    onQuitToggle: () -> Unit
) {
    val backgroundGif = R.drawable.shooting_background // 이미지 설정들
    val pauseImage = painterResource(R.drawable.shooting_pause)
    val pauseBackgroundImage = painterResource(R.drawable.shooting_pause_background)
    val resumeImage = painterResource(R.drawable.shooting_resume_button)
    val quitImage = painterResource(R.drawable.shooting_quit_button)

    var gifLoadingComplete by remember { mutableStateOf(false) } // gif 로딩 확인용 변수

    BackgroundLoop( // 배경 gif 재생
        gifImage = backgroundGif,
        onLoadingComplete = { success -> gifLoadingComplete = success }
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Text( // 현재 스코어 표시
            text = "Score: $currentScore",
            color = Color.White,
            fontFamily = juaFontFamily,
            fontSize = 40.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 25.dp)
        )
        Text( // 현재 플레이어의 남은 체력 표시
            text = "Hp: $playerHp / $playerMaxHp ",
            color = Color.White,
            fontFamily = juaFontFamily,
            fontSize = 30.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 75.dp)
        )
        Text( // 현재 제시어 표시
            text = "제시어: $currentKeyword",
            color = Color.White,
            fontFamily = juaFontFamily,
            fontSize = 40.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth

        if (gifLoadingComplete) { // gif 배경 로딩 완료 후 실행
            ImageButton( // 일시정지 버튼 표시
                painter = pauseImage,
                contentDescription = "일시정지 버튼",
                buttonSize = screenWidth / 8, // 화면 가로의 1/8 크기
                onClick = onPauseToggle,
                modifier = Modifier
                    .padding(20.dp)
                    .align(Alignment.TopStart)
            )

            if (pauseCheck) { // 일시정지 상태인지 체크 후 일시정지 화면 생성 및 삭제
                Image( // 일시정지 시 반투명한 검은 배경 표시
                    painter = pauseBackgroundImage,
                    contentDescription = "일시정지 화면 배경",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillHeight,
                    alpha = 0.5F
                )

                PauseScreen( // 일시정지 화면 함수 (계속하기 버튼과 그만두기 버튼 표시)
                    resumePainter = resumeImage,
                    quitPainter = quitImage,
                    buttonSize = screenWidth / 2, // 화면 가로의 1/2 크기
                    modifier = Modifier.align(Alignment.Center),
                    onResumeClicked = onPauseToggle,
                    onQuitClicked = onQuitToggle
                )
            }
        } else { // gif 배경 로딩 실패 시
            // 에러 띄우고 게임 종료
        }
    }
}

@Composable
fun PauseScreen(
    resumePainter: Painter,
    quitPainter: Painter,
    buttonSize: Dp,
    modifier: Modifier = Modifier,
    onResumeClicked: () -> Unit,
    onQuitClicked: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        ImageButton( // 계속하기 버튼 표시
            painter = resumePainter,
            contentDescription = "계속하기 버튼",
            buttonSize = buttonSize,
            onClick = onResumeClicked,
            modifier = modifier
        )
        ImageButton( // 그만두기 버튼 표시
            painter = quitPainter,
            contentDescription = "그만두기 버튼",
            buttonSize = buttonSize,
            onClick = onQuitClicked,
            modifier = modifier
        )
    }
}

@Composable
fun ImageButton( // 이미지를 버튼으로 만드는 함수
    painter: Painter,
    contentDescription: String,
    buttonSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier
            .size(buttonSize)
            .clickable(onClick = onClick)
    )
}

@Composable
fun GifLoader( // gif 이미지 로딩 함수 -> BackgroundLoop() 함수용
    gifResId: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    onLoadingComplete: (Boolean) -> Unit
) {
    val context = LocalContext.current

    val imageLoader = ImageLoader.Builder(context)
        .components {
                add(GifDecoder.Factory())
        }
        .build()

    AsyncImage(
        model = gifResId,
        contentDescription = "Animated GIF Image",
        imageLoader = imageLoader,
        modifier = modifier,
        contentScale = contentScale,
        onState = { state ->
            if (state is AsyncImagePainter.State.Success || state is AsyncImagePainter.State.Error) { // 로딩 성공 or 로딩 실패 시
                onLoadingComplete(state is AsyncImagePainter.State.Success) // 로딩 여부 전달
            }
        }
    )
}

@Composable
fun BackgroundLoop(
    gifImage: Int,
    onLoadingComplete: (Boolean) -> Unit
) { // gif 이미지를 표시 및 재생
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        GifLoader(
            gifResId = gifImage,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            onLoadingComplete = onLoadingComplete
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ShootingGamePreview() {
    ShootingGameTheme {
        ShootingGame("Android")
    }
}