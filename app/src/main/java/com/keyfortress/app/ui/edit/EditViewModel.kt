package com.keyfortress.app.ui.edit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keyfortress.app.data.model.CategoryConstants
import com.keyfortress.app.data.repository.AccountItem
import com.keyfortress.app.data.repository.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class EditUiState(
    val accountId: Long = 0L,
    val title: String = "",
    val accountName: String = "",
    val password: String = "",
    val linkedContact: String = "",
    val category: String = CategoryConstants.SOCIAL,
    val notes: String = "",
    val isStarred: Boolean = false,
    val isEditMode: Boolean = false,
    val errorMessage: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

class EditViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AccountRepository(application)

    private val _uiState = MutableStateFlow(EditUiState())
    val uiState: StateFlow<EditUiState> = _uiState.asStateFlow()

    fun loadAccount(id: Long) {
        if (id == 0L) return
        viewModelScope.launch {
            val account = repository.getAccountByIdFlow(id).firstOrNull()
            account?.let { acc ->
                _uiState.value = EditUiState(
                    accountId = acc.id,
                    title = acc.title,
                    accountName = acc.accountName,
                    password = acc.password,
                    linkedContact = acc.linkedContact,
                    category = acc.category,
                    notes = acc.notes,
                    isStarred = acc.isStarred,
                    isEditMode = true
                )
            }
        }
    }

    fun onTitleChange(title: String) {
        _uiState.value = _uiState.value.copy(title = title, errorMessage = "")
    }

    fun onAccountNameChange(accountName: String) {
        _uiState.value = _uiState.value.copy(accountName = accountName, errorMessage = "")
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password, errorMessage = "")
    }

    fun onLinkedContactChange(contact: String) {
        _uiState.value = _uiState.value.copy(linkedContact = contact)
    }

    fun onCategoryChange(category: String) {
        _uiState.value = _uiState.value.copy(category = category)
    }

    fun onNotesChange(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun onStarredChange(isStarred: Boolean) {
        _uiState.value = _uiState.value.copy(isStarred = isStarred)
    }

    fun saveAccount() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.value = state.copy(errorMessage = "请输入平台名称")
            return
        }
        if (state.accountName.isBlank()) {
            _uiState.value = state.copy(errorMessage = "请输入账号")
            return
        }
        if (state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "请输入密码")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true)
            try {
                repository.saveAccount(
                    AccountItem(
                        id = state.accountId,
                        title = state.title.trim(),
                        accountName = state.accountName.trim(),
                        password = state.password,
                        linkedContact = state.linkedContact.trim(),
                        category = state.category,
                        notes = state.notes.trim(),
                        isStarred = state.isStarred
                    )
                )
                _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = "保存失败: ${e.localizedMessage}")
            }
        }
    }
}
