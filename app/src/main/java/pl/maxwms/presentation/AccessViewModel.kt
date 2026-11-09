package pl.maxwms.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pl.maxwms.domain.model.AccessState
import pl.maxwms.domain.model.ThemeMode
import pl.maxwms.domain.repository.LicenseRepository
import pl.maxwms.domain.repository.SettingsRepository
import javax.inject.Inject

@HiltViewModel
class AccessViewModel @Inject constructor(
    licenseRepository: LicenseRepository,
) : ViewModel() {
    val access: StateFlow<AccessState> = licenseRepository.observeAccess()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccessState.Checking)
}

@HiltViewModel
class ThemeViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = settingsRepository.settings
        .map { it.themeMode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)
}
