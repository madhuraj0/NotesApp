package com.sba.notes.database

import android.content.Context
import androidx.lifecycle.LiveData
import com.sba.notes.sync.FileSyncManager
import kotlinx.coroutines.CoroutineScope

class NotesRepository(
    private val noteDao: NotesDao,
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    // FileSyncManager for handling file synchronization
    private val fileSyncManager = FileSyncManager(context, noteDao, coroutineScope)

    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    val allNotes: LiveData<List<Notes>> = noteDao.getAllNotes()

    // No automatic sync on initialization - will be triggered explicitly when app opens

    suspend fun insertNote(note: Notes) {
        // Use FileSyncManager to save both to database and file
        fileSyncManager.saveNote(note)
    }
    
    suspend fun updateNote(note: Notes) {
        // Use FileSyncManager to update both database and file
        fileSyncManager.saveNote(note)
    }
    
    suspend fun deleteNote(note: Notes) {
        // Use FileSyncManager to delete from both database and file
        fileSyncManager.deleteNote(note)
    }
    
    suspend fun deleteAllNote() {
        // Use FileSyncManager to delete all notes from both database and files
        fileSyncManager.deleteAllNotes()
    }
    
    // Manually trigger sync from files to database
    fun syncFromFiles() {
        fileSyncManager.syncFromFiles()
    }
    
    // Manually trigger sync from database to files
    fun syncToFiles() {
        fileSyncManager.syncToFiles()
    }
}
