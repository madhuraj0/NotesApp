package com.sba.notes.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NotesRepository
    val allNotes: LiveData<List<Notes>>

    init {
        val notesDao = NotesRoomDatabase.getDatabase(application, viewModelScope).notesDao()
        repository = NotesRepository(notesDao, application.applicationContext, viewModelScope)
        allNotes = repository.allNotes
    }

    fun insertNote(note: Notes) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertNote(note)
    }
    
    fun deleteNote(note: Notes) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteNote(note)
    }
    
    fun deleteAllNote() = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteAllNote()
    }
    
    // Methods to manually trigger syncs
    fun syncFromFiles() {
        repository.syncFromFiles()
    }
    
    fun syncToFiles() {
        repository.syncToFiles()
    }
}