package com.example.alfcapp.data.notes

import android.content.Context
import kotlinx.coroutines.flow.Flow

class NotesRepository private constructor(private val dao: NoteDao) {

    fun observeNotes(): Flow<List<NoteEntity>> = dao.observeAll()

    suspend fun upsert(note: NoteEntity): Long {
        return if (note.id == 0L) {
            dao.insert(note.copy(createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
        } else {
            dao.update(note.copy(updatedAt = System.currentTimeMillis()))
            note.id
        }
    }

    suspend fun delete(note: NoteEntity) = dao.delete(note)

    companion object {
        @Volatile private var INSTANCE: NotesRepository? = null

        fun getInstance(context: Context): NotesRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotesRepository(
                    NotesDatabase.getInstance(context).noteDao()
                ).also { INSTANCE = it }
            }
    }
}
