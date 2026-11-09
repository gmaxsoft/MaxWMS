package pl.maxwms.domain.model

enum class AccessSource {
    PLAY_SUBSCRIPTION,
    CRM_LICENSE,
}

sealed interface AccessState {
    data object Checking : AccessState

    data object Locked : AccessState

    data class Unlocked(val source: AccessSource) : AccessState
}
