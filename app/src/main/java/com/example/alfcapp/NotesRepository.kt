package com.example.alfcapp

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class Note(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val content: String
)

class NotesRepository(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("notes_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val notesKey = "saved_notes"

    fun saveNote(note: Note) {
        val currentNotes = getNotes().toMutableList()
        currentNotes.add(note)
        saveList(currentNotes)
    }

    fun deleteNote(note: Note) {
        val currentNotes = getNotes().toMutableList()
        currentNotes.removeAll { it.id == note.id }
        saveList(currentNotes)
    }

    fun getNotes(): List<Note> {
        val json = sharedPreferences.getString(notesKey, null) ?: return emptyList()
        val type = object : TypeToken<List<Note>>() {}.type
        return gson.fromJson(json, type)
    }

    private fun saveList(notes: List<Note>) {
        val json = gson.toJson(notes)
        sharedPreferences.edit().putString(notesKey, json).apply()
    }
}