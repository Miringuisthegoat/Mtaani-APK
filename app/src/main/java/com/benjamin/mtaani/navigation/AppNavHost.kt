package com.benjamin.mtaani.navigation


import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.benjamin.mtaani.ui.screens.about.AboutScreen
import com.benjamin.mtaani.ui.screens.auth.LoginScreen
import com.benjamin.mtaani.ui.screens.auth.RegisterScreen
import com.benjamin.mtaani.ui.screens.home.HomeScreen
import com.benjamin.mtaani.ui.screens.report.ReportIssueScreen
import com.benjamin.mtaani.ui.screens.splash.SplashScreen


@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = ROUT_SPLASH
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(ROUT_SPLASH) {
            SplashScreen(navController = navController)
        }
        composable(ROUT_LOGIN) {
            LoginScreen(navController = navController)
        }
        composable(ROUT_REGISTER) {
            RegisterScreen(navController = navController)
        }
        composable(ROUT_HOME) {
            HomeScreen(navController = navController)
        }
        composable(ROUT_ABOUT) {
            AboutScreen(navController = navController)
        }
        composable(ROUT_REPORT_ISSUE) {
                ReportIssueScreen(navController = navController)
            }
        }

    }
