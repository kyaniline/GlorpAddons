package com.glorpaddons.storage

data class StorageConfig(
    var enabled: Boolean = true,
    var scrollSpeed: Int = 3       // rows per scroll event (1–10)
)
