package com.example.shootinggame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
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
val keywordList = listOf( // 제시어 리스트
    "평범한", "웃는", "실망한", "놀란", "졸린", "즐거운", "큰", "작은",
    "빨강색", "주황색", "노랑색", "초록색", "파랑색", "검은색", "하양색", "분홍색"
)
val keywordResetTime: Long = 10000L // 제시어 변경 주기 설정 (밀리초)
val playerMaxHp: Int = 10 // 플레이어 최대 체력 설정
val playerShootTime: Long = 250L // 플레이어 레이저 발사 쿨타임 설정 (밀리초)



interface GameEntity { // 엔티티들이 가질 기본 속성
    val x: Float
    val y: Float
    val width: Float
    val height: Float
    val isAlive: Boolean
    fun bounds(): Rect = Rect(x, y, x + width, y + height)
}

data class Player( // 플레이어 엔티티
    override val x: Float,
    override val y: Float,
    override val width: Float = 100f, // 임시 크기
    override val height: Float = 100f,
    var health: Int = 3,
    override val isAlive: Boolean = true
) : GameEntity

data class Laser( // 레이저 엔티티
    override val x: Float = 0f,
    override val y: Float = 0f,
    override val width: Float = 10f, // 임시 크기
    override val height: Float = 30f,
    val speed: Float = 15f, // 레이저 이동 속도
    override val isAlive: Boolean = true
) : GameEntity

data class GameState( // 엔티티들의 상태를 저장
    val player: Player,
    val lasers: List<Laser>,
    val enemies: List<GameEntity> = emptyList(), // 미구현
    val enemyBullets: List<GameEntity> = emptyList() // 미구현
)

@Composable
fun ShootingGame(name: String, modifier: Modifier = Modifier) {
    var isInitialized by remember { mutableStateOf(false) } // 상태 초기화 여부 저장
    var screenBounds by remember { mutableStateOf(IntSize.Zero) } // 화면 크기 저장
    val density = LocalDensity.current.density // Dp와 Px의 전환을 위한 밀도 정보 저장
    var gameState by remember { // 엔티티 상태 변화 저장
        mutableStateOf(
            GameState(
                player = Player(x = 0f, y = 0f),
                lasers = emptyList()
            )
        )
    }

    var pauseCheck by remember { mutableStateOf(false) } // 일시정지 상태 체크
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

    GameLoop(
        gameState = gameState,
        onUpdateState = { newState -> gameState = newState },
        screenBounds = screenBounds,
        playerShootTime = playerShootTime
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                screenBounds = size
                if (!isInitialized && size != IntSize.Zero) {
                    gameState = gameState.copy(
                        player = gameState.player.copy( // 플레이어 초기 위치 설정
                            x = (size.width - gameState.player.width * density) / 2f,
                            y = size.height - gameState.player.height * density - 50.dp.toPx(density)
                        )
                    )
                    isInitialized = true
                }
            }
            .pointerInput(Unit) { // 플레이어 드래그 이동
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val newX = gameState.player.x + dragAmount.x
                    val newY = gameState.player.y + dragAmount.y
                    val maxX = screenBounds.width - gameState.player.width * density // 화면 경계 체크
                    val maxY = screenBounds.height - gameState.player.height * density

                    gameState = gameState.copy(
                        player = gameState.player.copy(
                            x = newX.coerceIn(0f, maxX),
                            y = newY.coerceIn(0f, maxY)
                        )
                    )
                }
            }
    ) {
        PlayerView(gameState.player) // 플레이어 렌더링
        gameState.lasers.forEach { laser -> // 레이저 렌더링
            LaserView(laser)
        }
    }

    // 플레이어 출현 함수
    // 랜덤 확률 -> 시간 따라 적 생성 함수로 전달
}

fun Dp.toPx(density: Float): Float = this.value * density // Dp를 Px로 변환
fun Float.toDp(density: Float): Dp = Dp(this / density) // Px를 Dp로 변환

@Composable
fun GameLoop(
    gameState: GameState,
    onUpdateState: (GameState) -> Unit,
    screenBounds: IntSize,
    playerShootTime: Long
) {
    val currentGameState by rememberUpdatedState(gameState)
    val updateState by rememberUpdatedState(onUpdateState)

    var lastFireTime by remember { mutableStateOf(0L) } // 타이머
    val density = LocalDensity.current.density
    val screenHeight = screenBounds.height.toFloat()

    LaunchedEffect(true) {
        while (true) {
            val currentTime = System.currentTimeMillis()
            var newState = currentGameState.copy()

            if (currentTime - lastFireTime >= playerShootTime && newState.player.isAlive) { // 레이저 발사
                val player = newState.player
                val laserX = player.x + player.width * density / 2f - Laser().width * density / 2f // 플레이어 앞에 생성
                val laserY = player.y - Laser().height * density

                val newLaser = Laser(
                    x = laserX,
                    y = laserY,
                    width = Laser().width * density,
                    height = Laser().height * density
                )

                newState = newState.copy(lasers = newState.lasers + newLaser)
                lastFireTime = currentTime
            }

            val updatedLasers = newState.lasers.mapNotNull { laser ->
                val newY = laser.y - laser.speed // 레이저 이동

                // 화면 끝(Y <= 0)에 닿으면 소멸
                if (newY < 0) {
                    null // 리스트에서 제거
                } else {
                    // TODO: 레이저와 적 충돌 감지 추가

                    laser.copy(y = newY)
                }
            }

            // TODO: 플레이어와 적 및 적 탄환 충돌 감지와 체력 감소 추가

            newState = newState.copy(lasers = updatedLasers)

            updateState(newState)
            delay(16)
        }
    }
}

@Composable
fun PlayerView(player: Player) {
    val density = LocalDensity.current.density
    if (player.isAlive) {
        Image(
            painter = painterResource(id = R.drawable.shooting_player),
            contentDescription = "Player Ship",
            modifier = Modifier
                .absoluteOffset {
                    IntOffset(
                        x = player.x.toInt(),
                        y = player.y.toInt()
                    )
                }
                .size(player.width.toDp(density), player.height.toDp(density))
        )
    }
}

@Composable
fun LaserView(laser: Laser) {
    val density = LocalDensity.current.density
    Image(
        painter = painterResource(id = R.drawable.shooting_player_bullet),
        contentDescription = "Laser",
        modifier = Modifier
            .absoluteOffset {
                IntOffset(
                    x = laser.x.toInt(),
                    y = laser.y.toInt()
                )
            }
            .size(laser.width.toDp(density), laser.height.toDp(density))
    )
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