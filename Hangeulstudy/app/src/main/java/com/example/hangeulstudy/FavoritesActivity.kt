package com.example.hangeulstudy

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hangeulstudy.databinding.ActivityFavoritesBinding

class FavoritesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavoritesBinding
    private val favoriteWords = mutableListOf<Word>()
    private val deletedWords = mutableListOf<Word>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 액션바 설정
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "My Words" // 제목 변경

        // 데이터 초기화
        val initialWords = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("favoriteWords", Word::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Word>("favoriteWords")
        }
        initialWords?.let { favoriteWords.addAll(it) }

        // 어댑터 설정
        val adapter = WordAdapter(favoriteWords) { wordToDelete ->
            // 삭제 클릭 시 동작 정의
            val position = favoriteWords.indexOf(wordToDelete)
            if (position != -1) {
                // 1. 결과로 돌려줄 목록에 추가
                deletedWords.add(wordToDelete)
                // 2. 현재 화면의 목록에서 제거
                favoriteWords.removeAt(position)
                // 3. 어댑터에 아이템이 제거되었음을 알림
                binding.recyclerView.adapter?.notifyItemRemoved(position)
            }
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // 뒤로가기 콜백 설정
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishWithResult()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun finishWithResult() {
        val resultIntent = Intent()
        resultIntent.putParcelableArrayListExtra("deletedWords", ArrayList(deletedWords))
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        // Up 버튼 클릭 시에도 결과 전달 후 종료
        finishWithResult()
        return true
    }
}
