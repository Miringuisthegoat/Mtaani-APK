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
import com.benjamin.mtaani.ui.screens.community.CommunityFeedScreen
import com.benjamin.mtaani.ui.screens.detail.IssueDetailScreen
import com.benjamin.mtaani.ui.screens.home.HomeScreen
import com.benjamin.mtaani.ui.screens.maps.MapScreen
import com.benjamin.mtaani.ui.screens.onboarding.OnboardingScreen
import com.benjamin.mtaani.ui.screens.profile.ProfileScreen
import com.benjamin.mtaani.ui.screens.reports.MyReportsScreen
import com.benjamin.mtaani.ui.screens.reports.ReportIssueScreen
import com.benjamin.mtaani.ui.screens.splash.SplashScreen
import com.benjamin.mtaani.ui.screens.updates.UpdatesScreen


@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = ROUT_ONBOARDING
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(ROUT_SPLASH) {
            SplashScreen(navController = navController)
        }
        composable(ROUT_ONBOARDING) {
            OnboardingScreen(navController = navController)
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
        composable(ROUT_PROFILE) {
            ProfileScreen(navController = navController)
        }
        composable(ROUT_MY_REPORTS) {
            MyReportsScreen(navController = navController)
        }
        composable(ROUT_MAP) {
            MapScreen(navController = navController)
        }
        composable(ROUT_COMMUNITY_FEED) {
            CommunityFeedScreen(navController = navController)
        }
        composable(ROUT_UPDATES) {
            UpdatesScreen(navController = navController)
        }

        composable("$ROUT_ISSUE_DETAIL/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            IssueDetailScreen(navController = navController, issueId = id)
        }

        }

    }

