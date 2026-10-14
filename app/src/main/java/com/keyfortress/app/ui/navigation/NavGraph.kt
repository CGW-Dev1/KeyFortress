package com.keyfortress.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.keyfortress.app.security.AppLockManager
import com.keyfortress.app.ui.detail.DetailScreen
import com.keyfortress.app.ui.detail.DetailViewModel
import com.keyfortress.app.ui.edit.EditScreen
import com.keyfortress.app.ui.edit.EditViewModel
import com.keyfortress.app.ui.home.HomeScreen
import com.keyfortress.app.ui.home.HomeViewModel
import com.keyfortress.app.ui.lock.LockScreen
import com.keyfortress.app.ui.lock.LockViewModel
import com.keyfortress.app.ui.settings.SettingsScreen
import com.keyfortress.app.ui.settings.SettingsViewModel

@Composable
fun KeyFortressNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val isLocked by AppLockManager.isLocked.collectAsState()

    // Whenever app is locked, immediately return to Lock screen
    LaunchedEffect(isLocked) {
        if (isLocked) {
            navController.navigate(Screen.Lock.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Lock.route
    ) {
        composable(Screen.Lock.route) {
            val lockViewModel: LockViewModel = viewModel()
            LockScreen(
                viewModel = lockViewModel,
                onUnlocked = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Lock.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = viewModel()
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToAdd = {
                    navController.navigate(Screen.Edit.createRoute(0L))
                },
                onNavigateToDetail = { accountId ->
                    navController.navigate(Screen.Detail.createRoute(accountId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onLockApp = {
                    navController.navigate(Screen.Lock.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument("accountId") { type = NavType.LongType })
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getLong("accountId") ?: 0L
            val detailViewModel: DetailViewModel = viewModel()
            DetailScreen(
                accountId = accountId,
                viewModel = detailViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { editAccountId ->
                    navController.navigate(Screen.Edit.createRoute(editAccountId))
                }
            )
        }

        composable(
            route = Screen.Edit.route,
            arguments = listOf(navArgument("accountId") {
                type = NavType.LongType
                defaultValue = 0L
            })
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getLong("accountId") ?: 0L
            val editViewModel: EditViewModel = viewModel()
            EditScreen(
                accountId = accountId,
                viewModel = editViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel = viewModel()
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
