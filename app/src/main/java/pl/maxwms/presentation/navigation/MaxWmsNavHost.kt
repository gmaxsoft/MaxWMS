package pl.maxwms.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pl.maxwms.presentation.dashboard.DashboardScreen
import pl.maxwms.presentation.parts.PartFormScreen
import pl.maxwms.presentation.parts.PartListScreen
import pl.maxwms.presentation.scanner.ScannerScreen
import pl.maxwms.presentation.settings.SettingsScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val SCANNER = "scanner"
    const val SETTINGS = "settings"
    const val PARTS = "parts?filter={filter}"
    const val PART_FORM = "partForm?partId={partId}"

    fun parts(filter: String): String = "parts?filter=$filter"

    fun partForm(partId: String? = null): String = "partForm?partId=${partId.orEmpty()}"
}

@Composable
fun MaxWmsNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onScan = { navController.navigate(Routes.SCANNER) },
                onParts = { navController.navigate(Routes.parts("all")) },
                onLowStock = { navController.navigate(Routes.parts("low")) },
                onAddPart = { navController.navigate(Routes.partForm()) },
                onEditPart = { id -> navController.navigate(Routes.partForm(id)) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            route = Routes.PARTS,
            arguments = listOf(navArgument("filter") { type = NavType.StringType; defaultValue = "all" }),
        ) { entry ->
            PartListScreen(
                initialFilter = entry.arguments?.getString("filter") ?: "all",
                onBack = { navController.popBackStack() },
                onScan = { navController.navigate(Routes.SCANNER) },
                onAddPart = { navController.navigate(Routes.partForm()) },
                onEditPart = { id -> navController.navigate(Routes.partForm(id)) },
            )
        }
        composable(Routes.SCANNER) {
            ScannerScreen(
                onBack = { navController.popBackStack() },
                onEditPart = { id -> navController.navigate(Routes.partForm(id)) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.PART_FORM,
            arguments = listOf(navArgument("partId") { type = NavType.StringType; defaultValue = "" }),
        ) {
            PartFormScreen(onBack = { navController.popBackStack() })
        }
    }
}
