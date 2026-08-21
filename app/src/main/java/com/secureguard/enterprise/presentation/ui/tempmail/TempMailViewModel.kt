package com.secureguard.enterprise.presentation.ui.tempmail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secureguard.enterprise.services.InboxResult
import com.secureguard.enterprise.services.OTPResult
import com.secureguard.enterprise.services.TempMailService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TempMailViewModel @Inject constructor(
    private val tempMailService: TempMailService
) : ViewModel() {

    private val _uiState = MutableStateFlow(TempMailUiState())
    val uiState: StateFlow<TempMailUiState> = _uiState.asStateFlow()

    val currentInbox: StateFlow<InboxResult?> = tempMailService.currentInbox
    val lastOTP: StateFlow<OTPResult?> = tempMailService.lastOTP
    val isProcessing: StateFlow<Boolean> = tempMailService.isProcessing

    fun createInbox() {
        viewModelScope.launch {
            addLog("📬 Erstelle neue Inbox...")
            val result = tempMailService.createInbox()
            if (result?.success == true) addLog("✅ Inbox erstellt: ${result.email}")
            else addLog("❌ Fehler beim Erstellen der Inbox")
        }
    }

    fun waitForOTP() {
        viewModelScope.launch {
            addLog("⏳ Warte auf OTP...")
            val result = tempMailService.waitForOTP()
            if (result?.success == true) {
                addLog("✅ OTP empfangen: ${result.otp}")
                addLog("📧 Von: ${result.from} | Betreff: ${result.subject}")
            } else {
                addLog("❌ Kein OTP empfangen (Timeout?)")
            }
        }
    }

    fun clearInbox() {
        tempMailService.clearInbox()
        addLog("🗑️ Inbox geleert")
    }

    private fun addLog(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _uiState.update { state -> state.copy(logEntries = state.logEntries + "[$timestamp] $message") }
    }
}

data class TempMailUiState(
    val logEntries: List<String> = emptyList()
)
