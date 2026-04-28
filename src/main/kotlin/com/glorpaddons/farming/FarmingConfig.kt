package com.glorpaddons.farming

data class FarmingConfig(
    var mouseLockEnabled: Boolean = false,
    var mouseLockGroundOnly: Boolean = false,
    var pestHighlightEnabled: Boolean = false,
    var farmingHudEnabled: Boolean = false,
    var visitorHelperEnabled: Boolean = false,
    var gardenPlotsEnabled: Boolean = false
)
