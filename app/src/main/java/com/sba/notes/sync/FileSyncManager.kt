package com.sba.notes.sync

import android.content.Context
import android.util.Log
import com.sba.notes.database.Notes
import com.sba.notes.database.NotesDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FileSyncManager(
    private val context: Context,
    private val notesDao: NotesDao,
    private val coroutineScope: CoroutineScope
) {
    
    companion object {
        private const val TAG = "FileSyncManager"
    }
    
    private val fileService = FileService(context)
    
    // Sync from database to files
    fun syncToFiles() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val notes = notesDao.getAllNotesSync()
                for (note in notes) {
                    fileService.saveNoteToFile(note)
                }
                Log.d(TAG, "Synced ${notes.size} notes to files")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing to files: ${e.message}", e)
            }
        }
    }
    
    // Sync from files to database
    fun syncFromFiles() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val fileNotes = fileService.readNotesFromFiles()
                val dbNotes = notesDao.getAllNotesSync()
                
                // Process each note from files
                for (fileNote in fileNotes) {
                    val existingNote = dbNotes.find { it.noteId == fileNote.noteId }
                    
                    if (existingNote == null) {
                        // New note from file - add to database
                        notesDao.insertNote(fileNote)
                        Log.d(TAG, "Imported new note from file: ${fileNote.title}")
                    } else {
                        // Note exists in both places - check which is newer
                        if (fileNote.date > existingNote.date) {
                            // File version is newer
                            val updatedNote = existingNote.copy(
                                title = fileNote.title,
                                description = fileNote.description,
                                date = fileNote.date
                            )
                            notesDao.updateNote(updatedNote)
                            Log.d(TAG, "Updated note from file: ${fileNote.title}")
                        }
                    }
                }
                
                Log.d(TAG, "Completed sync from files to database")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing from files: ${e.message}", e)
            }
        }
    }
    
    // Save a note to both database and file
    suspend fun saveNote(note: Notes) {
        withContext(Dispatchers.IO) {
            try {
                // Save to database first to get the ID
                if (note.noteId == 0L) {
                    notesDao.insertNote(note)
                } else {
                    notesDao.updateNote(note)
                }
                
                // Then save to file
                fileService.saveNoteToFile(note)
                Log.d(TAG, "Note saved to database and file: ${note.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving note: ${e.message}", e)
            }
        }
    }
    
    // Delete a note from both database and file
    suspend fun deleteNote(note: Notes) {
        withContext(Dispatchers.IO) {
            try {
                // Delete from database
                notesDao.deleteNote(note)
                
                // Then delete the file
                fileService.deleteNoteFile(note)
                Log.d(TAG, "Note deleted from database and file: ${note.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting note: ${e.message}", e)
            }
        }
    }
    
    // Delete all notes from both database and files
    suspend fun deleteAllNotes() {
        withContext(Dispatchers.IO) {
            try {
                // Delete from database
                notesDao.deleteAllNote()
                
                // Then delete all files
                fileService.deleteAllNoteFiles()
                Log.d(TAG, "All notes deleted from database and files")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting all notes: ${e.message}", e)
            }
        }
    }
}
