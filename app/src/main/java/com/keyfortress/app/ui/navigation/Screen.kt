package com.keyfortress.app.ui.navigation

sealed class Screen(val route: String) {
    object Lock : Screen("lock")
    object Home : Screen("home")
    object Detail : Screen("detail/{accountId}") {
        fun createRoute(accountId: Long) = "detail/$accountId"
    }
    object Edit : Screen("edit?accountId={accountId}") {
        fun createRoute(accountId: Long = 0L) = "edit?accountId=$accountId"
    }
    object Settings : Screen("settings")
}
