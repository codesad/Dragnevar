package sh.stefan.dragnevar.teamsync

sealed interface TeamSyncConnectionState {
    data object Disconnected : TeamSyncConnectionState
    data object WaitingForParty : TeamSyncConnectionState
    data object Connecting : TeamSyncConnectionState
    data object Connected : TeamSyncConnectionState
    data class Error(val message: String) : TeamSyncConnectionState
}
