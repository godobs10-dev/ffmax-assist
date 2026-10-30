package com.ffassist

import android.graphics.RectF

object AssistState {
    @Volatile var aimbotEnabled: Boolean = false
    @Volatile var espEnabled:    Boolean = false

    val espBoxes: MutableList<RectF> = mutableListOf()
    val espLock = Any()
}
