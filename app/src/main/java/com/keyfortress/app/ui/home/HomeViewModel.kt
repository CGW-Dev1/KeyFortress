package com.keyfortress.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keyfortress.app.data.model.CategoryConstants
import com.keyfortress.app.data.repository.AccountItem
import com.keyfortress.app.data.repository.AccountRepository
import com.keyfortress.app.security.ClipboardHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val searchQuery: String = "",
    val selectedCategory: String = CategoryConstants.ALL,
    val isLoading: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AccountRepository(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(CategoryConstants.ALL)
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    val accounts: StateFlow<List<AccountItem>> = combine(
        _searchQuery,
        _selectedCategory
    ) { query, category ->
        Pair(query, category)
    }.flatMapLatest { (query, category) ->
        if (query.isNotBlank()) {
            repository.searchAccountsFlow(query)
        } else {
            repository.getAccountsByCategoryFlow(category)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
        _searchQuery.value = "" // clear search when filtering categories
    }

    fun toggleStarred(account: AccountItem) {
        viewModelScope.launch {
            repository.toggleStarred(account.id, !account.isStarred)
        }
    }

    fun copyAccount(account: AccountItem) {
        ClipboardHelper.copyToClipboard(
            context = getApplication(),
            label = "账号",
            text = account.accountName,
            isSensitive = false,
            scope = viewModelScope
        )
        viewModelScope.launch {
            _snackbarMessage.emit("账号已复制到剪贴板")
        }
    }

    fun copyPassword(account: AccountItem) {
        ClipboardHelper.copyToClipboard(
            context = getApplication(),
            label = "密码",
            text = account.password,
            isSensitive = true,
            scope = viewModelScope
        ) {
            viewModelScope.launch {
                _snackbarMessage.emit("密码已从剪贴板自动清理")
            }
        }
        viewModelScope.launch {
            _snackbarMessage.emit("密码已复制 (将在30秒后自动清除剪贴板)")
        }
    }

    fun importSmartAccounts(items: List<AccountItem>, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val count = repository.saveAccountsBatch(items)
                _snackbarMessage.emit("成功智能导入 $count 条账号密码")
                onComplete(count)
            } catch (e: Exception) {
                _snackbarMessage.emit("导入失败: ${e.message}")
            }
        }
    }
}
