package com.example.focus

import androidx.annotation.StringRes

enum class Mode(@StringRes val labelRes: Int, val durationSeconds: Int) {
    Focus(R.string.mode_focus, 25 * 60),
    ShortBreak(R.string.mode_short_break, 5 * 60),
    LongBreak(R.string.mode_long_break, 15 * 60),
}
