package com.example.hangeulstudy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WordAdapter(
    private val words: List<Word>,
    private val onDeleteClick: (Word) -> Unit
) : RecyclerView.Adapter<WordAdapter.WordViewHolder>() {

    class WordViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val koreanTextView: TextView = view.findViewById(R.id.item_txtKorean)
        val meaningTextView: TextView = view.findViewById(R.id.item_txtMeaning)
        val exampleTextView: TextView = view.findViewById(R.id.item_txtExample)
        val deleteButton: ImageButton = view.findViewById(R.id.btnDeleteFavorite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_word, parent, false)
        return WordViewHolder(view)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        val word = words[holder.adapterPosition]
        holder.koreanTextView.text = word.korean
        holder.meaningTextView.text = word.meaning
        holder.exampleTextView.text = word.example

        holder.deleteButton.setOnClickListener {
            onDeleteClick(words[holder.adapterPosition])
        }
    }

    override fun getItemCount() = words.size
}
