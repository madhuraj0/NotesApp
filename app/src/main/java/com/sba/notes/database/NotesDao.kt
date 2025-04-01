package com.sba.notes.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface NotesDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNote(note: Notes)

    @Query("SELECT * from notes ORDER BY date DESC")
    fun getAllNotes(): LiveData<List<Notes>>
    
    // New method: Get all notes synchronously (not LiveData) for file syncing
    @Query("SELECT * from notes ORDER BY date DESC")
    suspend fun getAllNotesSync(): List<Notes>

    @Delete
    suspend fun deleteNote(note: Notes)

    @Query("DELETE FROM notes")
    suspend fun deleteAllNote()

    @Update
    suspend fun updateNote(note: Notes)
}
