package com.sba.notes.sync

import android.content.Context
import android.os.Environment
import android.util.Log
import com.sba.notes.database.Notes
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import org.json.JSONObject
import org.json.JSONArray
import java.io.IOException

class FileService(private val context: Context) {
    
    companion object {
        private const val TAG = "FileService"
        private const val NOTES_DIRECTORY = "Notes"
    }
    
    private val notesDir: File by lazy {
        File(context.getExternalFilesDir(null), NOTES_DIRECTORY).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    // Save a note to a file (JSON format)
    fun saveNoteToFile(note: Notes): Boolean {
        try {
            val noteFile = File(notesDir, "note_${note.noteId}.json")
            val jsonObject = JSONObject().apply {
                put("noteId", note.noteId)
                put("title", note.title)
                put("description", note.description)
                put("date", note.date)
            }
            
            FileOutputStream(noteFile).use { 
                it.write(jsonObject.toString().toByteArray()) 
            }
            
            Log.d(TAG, "Note saved to file: ${noteFile.absolutePath}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving note to file: ${e.message}", e)
            return false
        }
    }
    
    // Read all notes from files in the notes directory
    fun readNotesFromFiles(): List<Notes> {
        val notes = mutableListOf<Notes>()
        
        try {
            if (!notesDir.exists()) {
                Log.d(TAG, "Notes directory does not exist")
                return notes
            }
            
            notesDir.listFiles()?.filter { it.name.startsWith("note_") && it.name.endsWith(".json") }?.forEach { file ->
                try {
                    val jsonString = FileInputStream(file).bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(jsonString)
                    
                    val note = Notes(
                        noteId = jsonObject.optLong("noteId"),
                        title = jsonObject.optString("title", ""),
                        description = jsonObject.optString("description", ""),
                        date = jsonObject.optLong("date", System.currentTimeMillis())
                    )
                    
                    notes.add(note)
                    Log.d(TAG, "Read note from file: ${file.name}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading note from file ${file.name}: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading notes from files: ${e.message}", e)
        }
        
        return notes
    }
    
    // Delete a note file
    fun deleteNoteFile(note: Notes): Boolean {
        return try {
            val file = File(notesDir, "note_${note.noteId}.json")
            val result = if (file.exists()) file.delete() else true
            Log.d(TAG, "Deleted note file: ${file.absolutePath}, result: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting note file: ${e.message}", e)
            false
        }
    }
    
    // Delete all note files
    fun deleteAllNoteFiles(): Boolean {
        return try {
            var success = true
            
            notesDir.listFiles()?.filter { it.name.startsWith("note_") && it.name.endsWith(".json") }?.forEach { file ->
                val deleted = file.delete()
                if (!deleted) {
                    success = false
                    Log.e(TAG, "Failed to delete file: ${file.absolutePath}")
                }
            }
            
            Log.d(TAG, "Deleted all note files, success: $success")
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting all note files: ${e.message}", e)
            false
        }
    }
}