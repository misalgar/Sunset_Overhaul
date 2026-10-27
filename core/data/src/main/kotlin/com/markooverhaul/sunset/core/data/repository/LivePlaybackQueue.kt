package com.markooverhaul.sunset.core.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The order the viewer is watching live channels in, so the player can zap up/down through the
 * same category, and the Live screen can put focus back on the channel after Back.
 *
 * The list is set by the screen that starts playback; it is never fetched here, so zapping costs no
 * provider requests.
 */
@Singleton
class LivePlaybackQueue @Inject constructor() {

    @Volatile
    var channelIds: List<String> = emptyList()
        private set

    @Volatile
    var categoryId: String? = null
        private set

    private val _lastPlayedChannelId = MutableStateFlow<String?>(null)

    /** Survives navigation so the Live screen can restore focus after the player closes. */
    val lastPlayedChannelId: StateFlow<String?> = _lastPlayedChannelId.asStateFlow()

    fun setQueue(channelIds: List<String>, categoryId: String?) {
        this.channelIds = channelIds
        this.categoryId = categoryId
    }

    fun markPlayed(channelId: String) {
        _lastPlayedChannelId.value = channelId
    }
}
