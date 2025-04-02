package com.sba.notes.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(entities = [Notes::class], version = 1, exportSchema = false)
abstract class NotesRoomDatabase : RoomDatabase() {

    abstract fun notesDao(): NotesDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: NotesRoomDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): NotesRoomDatabase {
            // If the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NotesRoomDatabase::class.java,
                    "note_database"
                )
                .addCallback(NoteDatabaseCallback(scope))
                // Uncomment and add migrations if you update the database schema in the future
                //.addMigrations(MIGRATION_1_2)
                .build()
                
                INSTANCE = instance
                instance
            }
        }
        
        // Define migrations for future database versions
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Example migration if you add a column in the future
                // database.execSQL("ALTER TABLE notes ADD COLUMN priority INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
    
    private class NoteDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch {
                    populateDatabase(database.notesDao())
                }
            }
        }

        suspend fun populateDatabase(notesDao: NotesDao) {
            // Sample notes to show on first launch
            notesDao.insertNote(Notes(title = "Note Tip #4", description = "Enjoy Your New Notes App"))
            notesDao.insertNote(
                Notes(
                    title = "Note Tip #3",
                    description = "Done with a note? swipe it away on any side to Delete it.\n" +
                        "\n" +
                        "Give it a try! You Will have 5 sec to Undo.\n\nSwipe>>>>>>>>"
                )
            )
            notesDao.insertNote(
                Notes(
                    title = "Note Tip #2",
                    description = "Three Dots in top right!\n\nClick it to change Theme or delete All Notes"
                )
            )
            notesDao.insertNote(
                Notes(
                    title = "Note Tip #1",
                    description = "Notes lets you quickly capture what's on your mind.\n" +
                        "\n" +
                        "To start a new note, use the \"Plus Button\" Below."
                )
            )
        }
    }
}