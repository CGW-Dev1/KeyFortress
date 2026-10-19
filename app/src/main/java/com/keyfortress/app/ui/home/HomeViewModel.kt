package com.keyfortress.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keyfortress.app.data.entity.CategoryEntity
import com.keyfortress.app.data.model.CategoryConstants
import com.keyfortress.app.data.repository.AccountItem
import com.keyfortress.app.data.repository.AccountRepository
import com.keyfortress.app.security.ClipboardHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AccountRepository(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(CategoryConstants.ALL)
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    // Multi-select deletion state
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedAccountIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedAccountIds: StateFlow<Set<Long>> = _selectedAccountIds.asStateFlow()

    // Dynamic categories from database
    val categories: StateFlow<List<CategoryEntity>> = repository.getCategoriesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val filterCategoryNames: StateFlow<List<String>> = categories.map { list ->
        listOf(CategoryConstants.ALL, CategoryConstants.STARRED) + list.map { it.name }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoryConstants.FILTER_CATEGORIES
    )

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

    // ================= Multi-Select Functions =================
    fun enterSelectionMode(initialSelectedId: Long? = null) {
        _isSelectionMode.value = true
        _selectedAccountIds.value = if (initialSelectedId != null) setOf(initialSelectedId) else emptySet()
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedAccountIds.value = emptySet()
    }

    fun toggleSelectAccount(id: Long) {
        val current = _selectedAccountIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _selectedAccountIds.value = current
    }

    fun toggleSelectAll() {
        val allIds = accounts.value.map { it.id }.toSet()
        if (_selectedAccountIds.value.size == allIds.size) {
            _selectedAccountIds.value = emptySet()
        } else {
            _selectedAccountIds.value = allIds
        }
    }

    fun deleteSelectedAccounts() {
        val idsToDelete = _selectedAccountIds.value.toList()
        if (idsToDelete.isEmpty()) return

        viewModelScope.launch {
            try {
                repository.deleteAccountsBatch(idsToDelete)
                val count = idsToDelete.size
                exitSelectionMode()
                _snackbarMessage.emit("已批量删除 $count 条密码记录")
            } catch (e: Exception) {
                _snackbarMessage.emit("删除失败: ${e.message}")
            }
        }
    }

    fun moveSelectedAccountsToCategory(targetCategory: String) {
        val idsToMove = _selectedAccountIds.value.toList()
        if (idsToMove.isEmpty()) return

        viewModelScope.launch {
            try {
                repository.updateCategoryForAccounts(idsToMove, targetCategory)
                val count = idsToMove.size
                exitSelectionMode()
                _snackbarMessage.emit("已将 $count 条记录分类更改为「$targetCategory」")
            } catch (e: Exception) {
                _snackbarMessage.emit("移动分类失败: ${e.message}")
            }
        }
    }

    fun addCategoryAndMoveSelected(name: String) {
        val idsToMove = _selectedAccountIds.value.toList()
        if (idsToMove.isEmpty()) return

        viewModelScope.launch {
            try {
                val trimmed = name.trim()
                if (trimmed.isNotBlank()) {
                    repository.addCategory(trimmed)
                }
                repository.updateCategoryForAccounts(idsToMove, trimmed)
                val count = idsToMove.size
                exitSelectionMode()
                _snackbarMessage.emit("已新建分类「$trimmed」并将 $count 条记录归入该分类")
            } catch (e: Exception) {
                _snackbarMessage.emit("操作失败: ${e.message}")
            }
        }
    }

    // ================= Category Management Functions =================
    fun addCategory(name: String) {
        viewModelScope.launch {
            val success = repository.addCategory(name)
            if (success) {
                _snackbarMessage.emit("分类「$name」创建成功")
            } else {
                _snackbarMessage.emit("创建分类失败或已存在")
            }
        }
    }

    fun updateCategory(id: Long, oldName: String, newName: String) {
        viewModelScope.launch {
            val success = repository.updateCategory(id, oldName, newName)
            if (success) {
                if (_selectedCategory.value == oldName) {
                    _selectedCategory.value = newName
                }
                _snackbarMessage.emit("分类已修改为「$newName」")
            } else {
                _snackbarMessage.emit("修改分类失败或名称冲突")
            }
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            val success = repository.deleteCategory(id)
            if (success) {
                _selectedCategory.value = CategoryConstants.ALL
                _snackbarMessage.emit("分类已删除，分组内账号已自动归入「其它」")
            } else {
                _snackbarMessage.emit("默认分类无法删除")
            }
        }
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
        viewModelScope.launch {
            val password = repository.getDecryptedPassword(account.id)
            ClipboardHelper.copyToClipboard(
                context = getApplication(),
                label = "密码",
                text = password,
                isSensitive = true,
                scope = viewModelScope
            ) {
                viewModelScope.launch {
                    _snackbarMessage.emit("密码已从剪贴板自动清理")
                }
            }
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
