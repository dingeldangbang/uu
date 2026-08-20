package com.secureguard.enterprise.presentation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secureguard.enterprise.data.repository.SecureGuardRepository
import com.secureguard.enterprise.data.repository.SettingsRepository
import com.secureguard.enterprise.data.repository.SettingsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository,
    private val secureRepo: SecureGuardRepository
) : ViewModel() {

    val state: StateFlow<SettingsState> = repo.states

    fun toggle(key: String) {
        repo.set(key, !repo.states.value.isEnabled(key))
    }

    fun setRetention(days: Int) = repo.setRetention(days)

    fun requestDbReset() = repo.resetDb()

    fun confirmDbReset() = repo.clearDbResetFlag()

    /** Löscht alte Erkennungen & Alerts (Retention-Cleanup). */
    fun clearLogs() {
        viewModelScope.launch {
            secureRepo.purgeOld(secureRepo.retentionDaysOrDefault())
        }
    }
}
