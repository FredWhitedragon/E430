package com.example.e430.account.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.e430.account.data.AccountRepository
import com.example.e430.account.model.Account
import com.example.e430.core.network.E621Site
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountUiState(
    val account: Account? = null,
    val isRestoring: Boolean = true,
    val isLoggingIn: Boolean = false,
    val loginFailed: Boolean = false,
    val restoreFailed: Boolean = false,
    val isSavingBlacklist: Boolean = false,
    val blacklistSaveFailed: Boolean = false,
    val blacklistSaved: Boolean = false,
)

class AccountViewModel(
    private val repository: AccountRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                _uiState.value = AccountUiState(
                    account = repository.restore(),
                    isRestoring = false,
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _uiState.value = AccountUiState(
                    isRestoring = false,
                    restoreFailed = true,
                )
            }
        }
    }

    fun login(site: E621Site, username: String, apiKey: String) {
        if (_uiState.value.isLoggingIn || username.isBlank() || apiKey.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingIn = true, loginFailed = false) }
            try {
                val account = repository.login(site, username, apiKey)
                _uiState.value = AccountUiState(account = account, isRestoring = false)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoggingIn = false, loginFailed = true) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.value = AccountUiState(isRestoring = false)
        }
    }

    fun updateBlacklist(blacklistedTags: String) {
        val account = _uiState.value.account ?: return
        if (_uiState.value.isSavingBlacklist) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSavingBlacklist = true,
                    blacklistSaveFailed = false,
                    blacklistSaved = false,
                )
            }
            try {
                val updated = repository.updateBlacklist(account, blacklistedTags.trim())
                _uiState.update {
                    it.copy(
                        account = updated,
                        isSavingBlacklist = false,
                        blacklistSaved = true,
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isSavingBlacklist = false, blacklistSaveFailed = true)
                }
            }
        }
    }

    fun clearLoginError() {
        _uiState.update { it.copy(loginFailed = false, restoreFailed = false) }
    }

    companion object {
        fun factory(repository: AccountRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AccountViewModel(repository) as T
                }
            }
    }
}
