package dev.nikita_chernikov.lab5

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object DownloadStatusManager {
    private val _updateFlow = MutableSharedFlow<Unit>()
    val updateFlow = _updateFlow.asSharedFlow()

    suspend fun postUpdate() {
        _updateFlow.emit(Unit)
    }
}
