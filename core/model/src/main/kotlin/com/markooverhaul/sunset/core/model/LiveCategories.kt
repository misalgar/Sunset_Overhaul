package com.markooverhaul.sunset.core.model

/**
 * The panel publishes PPV as a dated live category (`PAY PER VIEW 24/10/2026`). It is a separate
 * destination in Sunset, not a Live TV filter.
 */
fun isPpvCategory(name: String): Boolean {
    val value = name.trim()
    return value.contains("PAY PER VIEW", ignoreCase = true) ||
        value.startsWith("PPV", ignoreCase = true) ||
        value.contains("PAY-PER-VIEW", ignoreCase = true)
}
