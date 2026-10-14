package com.keyfortress.app.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Built-in categories and helper methods.
 */
object CategoryConstants {
    const val ALL = "全部"
    const val STARRED = "收藏"
    const val SOCIAL = "社交"
    const val WORK = "工作"
    const val FINANCE = "金融"
    const val ENTERTAINMENT = "娱乐"
    const val OTHER = "其它"

    val DEFAULT_CATEGORIES = listOf(
        SOCIAL,
        WORK,
        FINANCE,
        ENTERTAINMENT,
        OTHER
    )

    val FILTER_CATEGORIES = listOf(
        ALL,
        STARRED,
        SOCIAL,
        WORK,
        FINANCE,
        ENTERTAINMENT,
        OTHER
    )

    fun getCategoryIcon(category: String): ImageVector {
        return when (category) {
            ALL -> Icons.Default.AllInclusive
            STARRED -> Icons.Default.Star
            SOCIAL -> Icons.Default.Chat
            WORK -> Icons.Default.Work
            FINANCE -> Icons.Default.AccountBalance
            ENTERTAINMENT -> Icons.Default.Movie
            else -> Icons.Default.Category
        }
    }
}
