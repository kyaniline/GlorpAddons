package com.glorpaddons.commissions

data class Commission(val name: String, val current: Int, val total: Int) {
    val progress: Float get() = if (total > 0) current.toFloat() / total.toFloat() else 0f
    val isDone: Boolean get() = current >= total
}
