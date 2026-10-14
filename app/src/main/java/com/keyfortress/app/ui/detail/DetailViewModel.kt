package com.keyfortress.app.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keyfortress.app.data.repository.AccountItem
import com.keyfortress.app.data.repository.AccountRepository
import com.keyfortress.app.data.repository.HistoryItem
import com.keyfortress.app.security.ClipboardHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AccountRepository(application)

    private val _accountId = MutableStateFlow<Long?>(null)

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    val account: StateFlow<AccountItem?> = _accountId.flatMapLatest { id ->
        if (id != null) {
            repository.getAccountByIdFlow(id)
        } else {
            flowOf(null)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val history: StateFlow<List<HistoryItem>> = _accountId.flatMapLatest { id ->
        if (id != null) {
            repository.getPasswordHistoryFlow(id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun loadAccount(id: Long) {
        _accountId.value = id
    }

    fun toggleStarred() {
        val current = account.value ?: return
        viewModelScope.launch {
            repository.toggleStarred(current.id, !current.isStarred)
        }
    }

    fun deleteAccount(onDeleted: () -> Unit) {
        val current = account.value ?: return
        viewModelScope.launch {
            repository.deleteAccount(current.id)
            onDeleted()
        }
    }

    fun copyText(label: String, text: String, isSensitive: Boolean = false) {
        ClipboardHelper.copyToClipboard(
            context = getApplication(),
            label = label,
            text = text,
            isSensitive = isSensitive,
            scope = viewModelScope
        ) {
            if (isSensitive) {
                viewModelScope.launch {
                    _snackbarMessage.emit("密码已从剪贴板自动清理")
                }
            }
        }
        viewModelScope.launch {
            if (isSensitive) {
                _snackbarMessage.emit("密码已复制 (将在30秒后自动清除剪贴板)")
            } else {
                _snackbarMessage.emit("$label 已复制")
            }
        }
    }
}
