package com.sba.notes.sync

import android.content.Context
import android.os.Environment
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
    
    // Check if external storage is available for read and write
    private fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    // Check if external storage is available to at least read
    private fun isExternalStorageReadable(): Boolean {
        return Environment.getExternalStorageState() in 
            setOf(Environment.MEDIA_MOUNTED, Environment.MEDIA_MOUNTED_READ_ONLY)
    }
    
    // Sync from database to files
    fun syncToFiles() {
        if (!isExternalStorageWritable()) {
            Log.e(TAG, "External storage is not writable, skipping sync to files")
            return
        }
        
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val notes = notesDao.getAllNotesSync()
                var successCount = 0
                
                for (note in notes) {
                    if (fileService.saveNoteToFile(note)) {
                        successCount++
                    }
                }
                
                Log.d(TAG, "Synced $successCount/${notes.size} notes to files")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing to files: ${e.message}", e)
            }
        }
    }
    
    // Sync from files to database
    fun syncFromFiles() {
        if (!isExternalStorageReadable()) {
            Log.e(TAG, "External storage is not readable, skipping sync from files")
            return
        }
        
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val fileNotes = fileService.readNotesFromFiles()
                if (fileNotes.isEmpty()) {
                    Log.d(TAG, "No notes found in files to sync")
                    return@launch
                }
                
                val dbNotes = notesDao.getAllNotesSync()
                var importCount = 0
                var updateCount = 0
                
                // Process each note from files
                for (fileNote in fileNotes) {
                    val existingNote = dbNotes.find { it.noteId == fileNote.noteId }
                    
                    if (existingNote == null) {
                        // New note from file - add to database
                        notesDao.insertNote(fileNote)
                        importCount++
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
                            updateCount++
                            Log.d(TAG, "Updated note from file: ${fileNote.title}")
                        }
                    }
                }
                
                Log.d(TAG, "Completed sync from files: imported $importCount, updated $updateCount")
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
                
                // Then save to file if storage is available
                if (isExternalStorageWritable()) {
                    fileService.saveNoteToFile(note)
                    Log.d(TAG, "Note saved to database and file: ${note.title}")
                } else {
                    Log.d(TAG, "Note saved to database only (storage not writable): ${note.title}")
                }
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
                
                // Then delete the file if storage is available
                if (isExternalStorageWritable()) {
                    fileService.deleteNoteFile(note)
                    Log.d(TAG, "Note deleted from database and file: ${note.title}")
                } else {
                    Log.d(TAG, "Note deleted from database only (storage not writable): ${note.title}")
                }
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
                
                // Then delete all files if storage is available
                if (isExternalStorageWritable()) {
                    fileService.deleteAllNoteFiles()
                    Log.d(TAG, "All notes deleted from database and files")
                } else {
                    Log.d(TAG, "All notes deleted from database only (storage not writable)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting all notes: ${e.message}", e)
            }
        }
    }
}