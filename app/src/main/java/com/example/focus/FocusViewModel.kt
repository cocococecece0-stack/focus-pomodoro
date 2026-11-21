package com.example.focus

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.focus.data.SessionStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class FocusUiState(
    val mode: Mode = Mode.Focus,
    val remainingSeconds: Int = Mode.Focus.durationSeconds,
    val isRunning: Boolean = false,
    val completedToday: Int = 0,
) {
    /** 0f at the start of a session, 1f when it is finished. */
    val progress: Float
        get() = 1f - remainingSeconds.toFloat() / mode.durationSeconds
}

class FocusViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SessionStore(application)

    private val _state = MutableStateFlow(FocusUiState())
    val state: StateFlow<FocusUiState> = _state.asStateFlow()

    private var ticker: Job? = null

    init {
        viewModelScope.launch {
            store.todayCount.collect { count ->
                _state.update { it.copy(completedToday = count) }
            }
        }
    }

    fun toggleTimer() {
        if (_state.value.isRunning) pause() else start()
    }

    fun reset() {
        pause()
        _state.update { it.copy(remainingSeconds = it.mode.durationSeconds) }
    }

    fun selectMode(mode: Mode) {
        pause()
        _state.update { it.copy(mode = mode, remainingSeconds = mode.durationSeconds) }
    }

    private fun start() {
        if (_state.value.isRunning) return
        _state.update { it.copy(isRunning = true) }
        ticker = viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                if (_state.value.isRunning) tick()
            }
        }
    }

    private fun pause() {
        _state.update { it.copy(isRunning = false) }
        ticker?.cancel()
        ticker = null
    }

    private fun tick() {
        val current = _state.value
        if (current.remainingSeconds > 1) {
            _state.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }
        } else {
            finishSession()
        }
    }

    private fun finishSession() {
        val current = _state.value
        if (current.mode == Mode.Focus) {
            viewModelScope.launch { store.increment() }
            // Long break after every four completed focus sessions.
            val next = if ((current.completedToday + 1) % 4 == 0) Mode.LongBreak else Mode.ShortBreak
            pause()
            _state.update { it.copy(mode = next, remainingSeconds = next.durationSeconds) }
        } else {
            pause()
            _state.update { it.copy(mode = Mode.Focus, remainingSeconds = Mode.Focus.durationSeconds) }
        }
    }
}
