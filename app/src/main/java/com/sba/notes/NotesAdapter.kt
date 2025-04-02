package com.sba.notes

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sba.notes.database.Notes
import androidx.recyclerview.widget.ListAdapter
import com.sba.notes.databinding.NoteLayoutBinding

class NotesAdapter : ListAdapter<Notes, NotesAdapter.NotesViewHolder>(NotesDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotesViewHolder {
        return NotesViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: NotesViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }
    
    fun getNote(position: Int): Notes = getItem(position)

    class NotesViewHolder private constructor(private val binding: NoteLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Notes) {
            binding.noteTitle.text = item.title
            binding.noteDesc.text = item.description
            
            // Handle visibility based on title content
            binding.noteTitle.visibility = if (item.title.isBlank()) View.GONE else View.VISIBLE
            
            // Navigate to edit fragment when note is clicked
            binding.noteConstrainLayout.setOnClickListener {
                val action = AllNotesFragmentDirections
                    .actionAllNotesFragmentToEditNoteFragment(item)
                it.findNavController().navigate(action)
            }
            
            binding.noteConstrainLayout.setOnLongClickListener {
                Log.d("Adapter", "Note ID: ${item.noteId}")
                true
            }
        }
        
        companion object {
            fun from(parent: ViewGroup): NotesViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = NoteLayoutBinding.inflate(layoutInflater, parent, false)
                return NotesViewHolder(binding)
            }
        }
    }
}

class NotesDiffCallback : DiffUtil.ItemCallback<Notes>() {
    override fun areItemsTheSame(oldItem: Notes, newItem: Notes): Boolean {
        return oldItem.noteId == newItem.noteId
    }

    override fun areContentsTheSame(oldItem: Notes, newItem: Notes): Boolean {
        return oldItem == newItem
    }
}