package com.example.shootinggame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.rememberUpdatedState
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
import kotlin.random.Random

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
val playerSize: Float = 100f // 플레이어 크기
val playerMaxHp: Int = 10 // 플레이어 최대 체력 설정
val playerShootTime: Long = 250L // 플레이어 레이저 발사 쿨타임 설정 (밀리초)
val laserWidth: Float = 10f // 레이저 크기
val laserHeight: Float = 30f
val enemySize: Float = 100f // 적 크기
val enemySpawnTime: Long = 10000L // 적 출현 시간
val enemyShootTime: Long = 2000L // 적 탄환 발사 시간
val enemyMoveSpeed: Float = 3f // 적 이동 속도
val enemyBulletSize: Float = 15f // 적 탄환 크기
val enemyBulletSpeed: Float = 8f // 적 탄환 속도

interface GameEntity { // 엔티티들이 가질 기본 속성
    val x: Float
    val y: Float
    val width: Float
    val height: Float
    val isAlive: Boolean
    fun bounds(): Rect = Rect(x, y, x + width, y + height)
}

fun Rect.overlaps(other: Rect): Boolean { // 두 Rect가 겹치는지 확인하는 함수
    return this.left < other.right && this.right > other.left &&
            this.top < other.bottom && this.bottom > other.top
}

data class Player( // 플레이어 엔티티
    override val x: Float,
    override val y: Float,
    override val width: Float = playerSize,
    override val height: Float = playerSize,
    var health: Int = 3,
    override val isAlive: Boolean = true
) : GameEntity

data class Laser( // 레이저 엔티티
    override val x: Float = 0f,
    override val y: Float = 0f,
    override val width: Float = laserWidth,
    override val height: Float = laserHeight,
    val speed: Float = 15f, // 레이저 이동 속도
    override val isAlive: Boolean = true
) : GameEntity

data class Enemy(
    override val x: Float,
    override val y: Float,
    override val width: Float = enemySize,
    override val height: Float = enemySize,
    var health: Int = 8, // health번 피격당하면 소멸
    override val isAlive: Boolean = true,
    val targetY: Float, // 멈춰야 할 최종 y 위치
    val isMoving: Boolean = true, // 이동 중인지 체크
    var lastShotTime: Long = 0L // 마지막 탄환 발사 시간
) : GameEntity

data class EnemyBullet(
    override val x: Float,
    override val y: Float,
    override val width: Float = enemyBulletSize,
    override val height: Float = enemyBulletSize,
    val speed: Float = enemyBulletSpeed,
    val velX: Float, // x축 속도
    val velY: Float, // y축 속도
    override val isAlive: Boolean = true
) : GameEntity

data class GameState( // 엔티티들의 상태를 저장
    val player: Player,
    val lasers: List<Laser>,
    val enemies: List<Enemy>,
    val enemyBullets: List<EnemyBullet>
)

@Composable
fun ShootingGame(name: String, modifier: Modifier = Modifier) {
    var isInitialized by remember { mutableStateOf(false) } // 상태 초기화 여부 저장
    val density = LocalDensity.current.density // Dp와 Px의 전환을 위한 밀도 정보 저장
    var gameState by remember { // 엔티티 상태 변화 저장
        mutableStateOf(
            GameState(
                player = Player(x = 260.0f, y = 836.0f), // TODO: 플레이어 초기 생성 위치가 0, 0으로 고정되는 문제 발생하여 일단 임의로 설정함.
                lasers = emptyList(),
                enemies = emptyList(),
                enemyBullets = emptyList()
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

    val backgroundGif = R.drawable.shooting_background // 배경 이미지 설정
    var gifLoadingComplete by remember { mutableStateOf(false) } // gif 로딩 확인용 변수

    BackgroundLoop( // 배경 gif 재생
        gifImage = backgroundGif,
        onLoadingComplete = { success -> gifLoadingComplete = success }
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit, pauseCheck) { // 플레이어 드래그 이동
                val playerWidthPx = playerSize * density
                val playerHeightPx = playerSize * density

                detectDragGestures { change, dragAmount ->
                    if (pauseCheck) return@detectDragGestures

                    change.consume()
                    val newX = gameState.player.x + dragAmount.x
                    val newY = gameState.player.y + dragAmount.y

                    val maxX = size.width - (playerWidthPx / 2f)
                    val maxY = size.height - playerHeightPx

                    gameState = gameState.copy(
                        player = gameState.player.copy(
                            x = newX.coerceIn(0f, maxX.toFloat()),
                            y = newY.coerceIn(0f, maxY.toFloat())
                        )
                    )
                }
            }
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()

        LaunchedEffect(screenWidthPx, screenHeightPx) {
            if (!isInitialized) {
                val playerWidthPx = playerSize * density
                val playerHeightPx = playerSize * density
                val bottomPaddingPx = 50.dp.toPx(density)

                val initialX = (screenWidthPx - playerWidthPx) / 2f
                val initialY = screenHeightPx - playerHeightPx - bottomPaddingPx

                gameState = gameState.copy(
                    player = gameState.player.copy(x = initialX, y = initialY)
                )
                isInitialized = true
            }
        }

        GameLoop(
            gameState = gameState,
            onUpdateState = { newState -> gameState = newState },
            screenWidthPx = screenWidthPx, // 🎯 NEW: Px 값 전달
            screenHeightPx = screenHeightPx, // 🎯 NEW: Px 값 전달
            playerShootTime = playerShootTime,
            pauseCheck = pauseCheck
        )

        if (isInitialized) {
            PlayerView(gameState.player) // 플레이어 렌더링

            gameState.lasers.forEach { laser -> // 레이저 렌더링
                LaserView(laser)
            }

            gameState.enemies.forEach { enemy -> // 적 렌더링
                EnemyView(enemy)
            }

            gameState.enemyBullets.forEach { bullet -> // 적 탄환 렌더링
                EnemyBulletView(bullet)
            }
        }

        CreateInterface( // 인터페이스 표시
            pauseCheck = pauseCheck,
            gifLoadingComplete = gifLoadingComplete,
            currentScore = currentScore,
            playerHp = playerHp,
            currentKeyword = currentKeyword,
            onPauseToggle = { pauseCheck = !pauseCheck },
            onQuitToggle = { pauseCheck = !pauseCheck } // TODO: 나중에 게임 종료 코드로 바꾸기
        )
    }
}

fun Dp.toPx(density: Float): Float = this.value * density // Dp를 Px로 변환
fun Float.toDp(density: Float): Dp = Dp(this / density) // Px를 Dp로 변환

@Composable
fun GameLoop(
    gameState: GameState,
    onUpdateState: (GameState) -> Unit,
    screenWidthPx: Float,
    screenHeightPx: Float,
    playerShootTime: Long,
    pauseCheck: Boolean
) {
    val currentGameState by rememberUpdatedState(gameState)
    val updateState by rememberUpdatedState(onUpdateState)

    var lastFireTime by remember { mutableStateOf(0L) } // 플레이어 레이저 타이머
    val density = LocalDensity.current.density

    val halfScreenY = screenHeightPx / 2f

    val enemyWidthPx = enemySize * density
    val enemyHeightPx = enemySize * density
    val enemyBulletWidthPx = enemyBulletSize * density
    val enemyBulletHeightPx = enemyBulletSize * density

    val playerLaserOffsetPx = 23.5f * density // 플레이어 레이저 오프셋을 픽셀 단위로 미리 계산

    val randomGenerator = remember { Random(System.currentTimeMillis()) }

    LaunchedEffect(pauseCheck) {
        if (!pauseCheck) { // 일시정지 상태가 아닐 때만 실행
            var lastEnemySpawnTime = System.currentTimeMillis() // 적 생성 타이머

            while (isActive) {
                val currentTime = System.currentTimeMillis()
                var newState = currentGameState.copy()
                val player = newState.player

                // 적 생성
                if (currentTime - lastEnemySpawnTime >= enemySpawnTime) {
                    val maxX = screenWidthPx - enemyWidthPx
                    val validatedMaxX = if (maxX < 0) 0f else maxX
                    val randomX = randomGenerator.nextFloat() * validatedMaxX
                    val maxTargetY = halfScreenY - enemyHeightPx
                    val minTargetY = 100.dp.toPx(density)
                    val rangeY = maxTargetY - minTargetY
                    val randomTargetY = if (rangeY <= 0) minTargetY else randomGenerator.nextFloat() * rangeY + minTargetY

                    val newEnemy = Enemy(
                        x = randomX, y = -enemyHeightPx, targetY = randomTargetY,
                        width = enemyWidthPx, height = enemyHeightPx
                    )
                    newState = newState.copy(enemies = newState.enemies + newEnemy)
                    lastEnemySpawnTime = currentTime
                }

                // 플레이어 레이저 발사
                if (currentTime - lastFireTime >= playerShootTime && player.isAlive) {
                    val playerWidthPx = player.width * density
                    val laserWidthPx = laserWidth * density
                    val laserHeightPx = laserHeight * density

                    val centerCorrection = (playerWidthPx / 2f) - (laserWidthPx / 2f)
                    val laserX = player.x + centerCorrection - playerLaserOffsetPx
                    val laserY = player.y - laserHeightPx

                    val newLaser = Laser(
                        x = laserX, y = laserY, width = laserWidthPx, height = laserHeightPx
                    )
                    newState = newState.copy(lasers = newState.lasers + newLaser)
                    lastFireTime = currentTime
                }

                // 적 이동 및 적 탄환 발사
                val newEnemyBullets = mutableListOf<EnemyBullet>() // 새로 발사된 탄환을 모을 리스트
                val updatedEnemies = newState.enemies.map { enemy ->
                    var currentEnemy = enemy

                    // 이동
                    if (currentEnemy.isMoving) {
                        val newY = enemy.y + enemyMoveSpeed
                        currentEnemy = if (newY >= enemy.targetY) {
                            currentEnemy.copy(y = enemy.targetY, isMoving = false)
                        } else {
                            currentEnemy.copy(y = newY)
                        }
                    }

                    // 발사
                    if (!currentEnemy.isMoving && currentTime - currentEnemy.lastShotTime >= enemyShootTime) {
                        // 탄환 방향 및 위치 계산
                        val playerCenter = player.x + (player.width * density) / 2f
                        val playerCenterY = player.y + (player.height * density) / 2f
                        val enemyCenter = currentEnemy.x + currentEnemy.width / 2f
                        val enemyCenterY = currentEnemy.y + currentEnemy.height / 2f
                        val dx = playerCenter - enemyCenter
                        val dy = playerCenterY - enemyCenterY
                        val angle = kotlin.math.atan2(dy, dx)
                        val velX = enemyBulletSpeed * kotlin.math.cos(angle)
                        val velY = enemyBulletSpeed * kotlin.math.sin(angle)
                        val bulletX = enemyCenter - enemyBulletWidthPx / 2f
                        val bulletY = enemyCenterY - enemyBulletHeightPx / 2f

                        val newBullet = EnemyBullet(
                            x = bulletX, y = bulletY, velX = velX, velY = velY,
                            width = enemyBulletWidthPx, height = enemyBulletHeightPx
                        )
                        newEnemyBullets.add(newBullet) // 새 탄환 리스트에 추가
                        currentEnemy = currentEnemy.copy(lastShotTime = currentTime)
                    }
                    currentEnemy
                }

                // 플레이어 레이저 이동 및 화면 밖 소멸
                val lasersAfterMove = newState.lasers.mapNotNull { laser ->
                    val newY = laser.y - laser.speed
                    laser.takeIf { newY >= -laser.height }?.copy(y = newY) // 레이저가 완전히 사라지면 제거
                }

                // 적 탄환 이동 및 화면 밖 소멸
                val enemyBulletsAfterMove = newState.enemyBullets.mapNotNull { bullet ->
                    val newX = bullet.x + bullet.velX
                    val newY = bullet.y + bullet.velY
                    val outOfBounds =
                        newX < -bullet.width ||
                                newX > screenWidthPx ||
                                newY < -bullet.height ||
                                newY > screenHeightPx
                    bullet.takeIf { !outOfBounds }?.copy(x = newX, y = newY)
                }

                // 이동 결과 반영 및 탄환 추가
                newState = newState.copy(
                    enemies = updatedEnemies,
                    lasers = lasersAfterMove,
                    enemyBullets = enemyBulletsAfterMove + newEnemyBullets // 새로 발사된 탄환 추가
                )

                // 충돌 처리를 위해 현재 상태를 변수에 복사
                var currentLasers = newState.lasers.toMutableList()
                var currentEnemies = newState.enemies.toMutableList()
                var currentEnemyBullets = newState.enemyBullets.toMutableList()
                var playerHealth = player.health
                val playerBounds = player.bounds()

                // 레이저와 적 충돌
                val lasersToRemove = mutableSetOf<Laser>()

                currentEnemies = currentEnemies.map { enemy ->
                    if (!enemy.isAlive) return@map enemy

                    var updatedEnemy = enemy
                    val enemyBounds = enemy.bounds()

                    currentLasers.forEach { laser ->
                        // 적과 겹치는지 확인
                        if (laser.isAlive && !lasersToRemove.contains(laser) && laser.bounds().overlaps(enemyBounds)) {
                            lasersToRemove.add(laser) // 레이저 제거 목록에 추가

                            // 적 체력 감소
                            val newHealth = updatedEnemy.health - 1
                            updatedEnemy = if (newHealth <= 0) {
                                updatedEnemy.copy(health = 0, isAlive = false) // 사망 처리
                            } else {
                                updatedEnemy.copy(health = newHealth)
                            }
                        }
                    }
                    updatedEnemy // 업데이트된 적 상태 반환
                }.toMutableList()

                // 충돌한 레이저 제거
                currentLasers.removeAll(lasersToRemove)

                // 플레이어와 적 탄환 충돌
                currentEnemyBullets.removeAll { bullet ->
                    if (playerBounds.overlaps(bullet.bounds())) {
                        playerHealth-- // 플레이어 체력 감소
                        true // 탄환 제거
                    } else {
                        false
                    }
                }

                // 플레이어와 적 본체 충돌
                currentEnemies.removeAll { enemy ->
                    if (playerBounds.overlaps(enemy.bounds())) {
                        playerHealth-- // 플레이어 체력 감소
                        true // 적 제거
                    } else {
                        false
                    }
                }

                newState = newState.copy(
                    player = player.copy(health = playerHealth),
                    lasers = currentLasers,
                    enemies = currentEnemies.filter { it.isAlive }, // 체력 0 이하로 떨어진 적 제거
                    enemyBullets = currentEnemyBullets
                )

                updateState(newState)
                delay(16)
            }
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
fun EnemyView(enemy: Enemy) {
    val density = LocalDensity.current.density
    if (enemy.isAlive) {
        Image(
            painter = painterResource(id = R.drawable.shooting_enemy_default),
            contentDescription = "Enemy Ship",
            modifier = Modifier
                .absoluteOffset {
                    IntOffset(
                        x = enemy.x.toInt(),
                        y = enemy.y.toInt()
                    )
                }
                .size(enemy.width.toDp(density), enemy.height.toDp(density))
        )
    }
}

@Composable
fun EnemyBulletView(bullet: EnemyBullet) {
    val density = LocalDensity.current.density
    if (bullet.isAlive) {
        Image(
            painter = painterResource(id = R.drawable.shooting_enemy_bullet),
            contentDescription = "Enemy Bullet",
            modifier = Modifier
                .absoluteOffset {
                    IntOffset(
                        x = bullet.x.toInt(),
                        y = bullet.y.toInt()
                    )
                }
                .size(bullet.width.toDp(density), bullet.height.toDp(density))
        )
    }
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
    gifLoadingComplete: Boolean,
    currentScore: Int,
    playerHp: Int,
    currentKeyword: String,
    onPauseToggle: () -> Unit,
    onQuitToggle: () -> Unit
) {
    val pauseImage = painterResource(R.drawable.shooting_pause) // 이미지 설정들
    val pauseBackgroundImage = painterResource(R.drawable.shooting_pause_background)
    val resumeImage = painterResource(R.drawable.shooting_resume_button)
    val quitImage = painterResource(R.drawable.shooting_quit_button)

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
            // TODO: 에러 띄우고 게임 종료
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