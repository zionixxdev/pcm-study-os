package dev.zionixx.pcmstudyos.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.zionixx.pcmstudyos.ui.AppViewModel
import dev.zionixx.pcmstudyos.ui.screens.AnalyticsScreen
import dev.zionixx.pcmstudyos.ui.screens.DashboardScreen
import dev.zionixx.pcmstudyos.ui.screens.FormulasScreen
import dev.zionixx.pcmstudyos.ui.screens.GoalsScreen
import dev.zionixx.pcmstudyos.ui.screens.ImportScreen
import dev.zionixx.pcmstudyos.ui.screens.MistakesScreen
import dev.zionixx.pcmstudyos.ui.screens.MoreScreen
import dev.zionixx.pcmstudyos.ui.screens.NotesScreen
import dev.zionixx.pcmstudyos.ui.screens.PlannerScreen
import dev.zionixx.pcmstudyos.ui.screens.PracticeScreen
import dev.zionixx.pcmstudyos.ui.screens.RevisionScreen
import dev.zionixx.pcmstudyos.ui.screens.SearchScreen
import dev.zionixx.pcmstudyos.ui.screens.SettingsScreen
import dev.zionixx.pcmstudyos.ui.screens.SyllabusScreen
import dev.zionixx.pcmstudyos.ui.screens.TestsScreen
import dev.zionixx.pcmstudyos.ui.screens.TimerScreen

object Routes {
    const val HOME = "home"
    const val PLANNER = "planner"
    const val SYLLABUS = "syllabus"
    const val REVISION = "revision"
    const val MORE = "more"
    const val TIMER = "timer"
    const val FORMULAS = "formulas"
    const val PRACTICE = "practice"
    const val MISTAKES = "mistakes"
    const val TESTS = "tests"
    const val NOTES = "notes"
    const val ANALYTICS = "analytics"
    const val GOALS = "goals"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val IMPORT = "import"
}

private data class BottomItem(val route: String, val label: String, val icon: ImageVector)

private val bottomItems = listOf(
    BottomItem(Routes.HOME, "Home", Icons.Filled.Home),
    BottomItem(Routes.PLANNER, "Planner", Icons.Filled.DateRange),
    BottomItem(Routes.SYLLABUS, "Syllabus", Icons.Filled.List),
    BottomItem(Routes.REVISION, "Revision", Icons.Filled.Refresh),
    BottomItem(Routes.MORE, "More", Icons.Filled.Menu),
)

@Composable
fun PcmApp(appViewModel: AppViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    fun navigate(route: String) {
        val target = route.substringBefore('?')
        if (target == currentRoute) return
        navController.navigate(target) {
            launchSingleTop = true
            if (target in bottomItems.map { it.route }) {
                popUpTo(Routes.HOME) { saveState = true }
                restoreState = true
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface) {
                bottomItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = { navigate(item.route) },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, style = androidx.compose.material3.MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOME) { DashboardScreen(onNavigate = ::navigate) }
            composable(Routes.PLANNER) { PlannerScreen() }
            composable(Routes.SYLLABUS) { SyllabusScreen(onOpenImport = { navigate(Routes.IMPORT) }) }
            composable(Routes.REVISION) { RevisionScreen() }
            composable(Routes.MORE) { MoreScreen(onNavigate = ::navigate) }
            composable(Routes.TIMER) { TimerScreen() }
            composable(Routes.FORMULAS) { FormulasScreen() }
            composable(Routes.PRACTICE) { PracticeScreen() }
            composable(Routes.MISTAKES) { MistakesScreen() }
            composable(Routes.TESTS) { TestsScreen() }
            composable(Routes.NOTES) { NotesScreen() }
            composable(Routes.ANALYTICS) { AnalyticsScreen() }
            composable(Routes.GOALS) { GoalsScreen() }
            composable(Routes.SEARCH) { SearchScreen(onNavigate = ::navigate) }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(Routes.IMPORT) { ImportScreen(onDone = { navController.popBackStack() }) }
        }
    }
}
