package com.example.focus.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.focusDataStore: DataStore<Preferences> by preferencesDataStore(name = "focus_stats")

/**
 * Keeps the number of focus sessions finished today.
 * The stored counter is scoped to a calendar date, so a new day starts from zero.
 */
class SessionStore(private val context: Context) {

    private val countKey = intPreferencesKey("completed_count")
    private val dateKey = stringPreferencesKey("completed_date")

    val todayCount: Flow<Int> = context.focusDataStore.data.map { prefs ->
        if (prefs[dateKey] == today()) prefs[countKey] ?: 0 else 0
    }

    suspend fun increment() {
        context.focusDataStore.edit { prefs ->
            val date = today()
            val current = if (prefs[dateKey] == date) prefs[countKey] ?: 0 else 0
            prefs[dateKey] = date
            prefs[countKey] = current + 1
        }
    }

    private fun today(): String = LocalDate.now().toString()
}
