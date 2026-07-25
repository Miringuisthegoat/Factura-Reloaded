package com.benjamin.factura.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.benjamin.factura.ui.screens.ClientDetailScreen
import com.benjamin.factura.ui.screens.ClientListScreen
import com.benjamin.factura.ui.screens.CreateEditClientScreen
import com.benjamin.factura.ui.screens.CreateExpenseScreen
import com.benjamin.factura.ui.screens.CreateInvoiceScreen
import com.benjamin.factura.ui.screens.DashboardScreen
import com.benjamin.factura.ui.screens.ExpenseListScreen
import com.benjamin.factura.ui.screens.InvoiceDetailScreen
import com.benjamin.factura.ui.screens.InvoiceListScreen
import com.benjamin.factura.ui.screens.OnboardingScreen
import com.benjamin.factura.ui.screens.PaymentRecordScreen
import com.benjamin.factura.ui.screens.ReportsScreen
import com.benjamin.factura.ui.screens.SettingsScreen
import com.benjamin.factura.ui.screens.SplashScreen

@Composable
fun FacturaAppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = FacturaDestination.bottomNavDestinations.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    FacturaDestination.bottomNavDestinations.forEach { destination ->
                        NavigationBarItem(
                            icon = { destination.icon?.let { Icon(it, contentDescription = destination.title) } },
                            label = { Text(destination.title) },
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = FacturaDestination.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Splash Screen
            composable(FacturaDestination.Splash.route) {
                SplashScreen(
                    onNavigateToOnboarding = {
                        navController.navigate(FacturaDestination.Onboarding.route) {
                            popUpTo(FacturaDestination.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToDashboard = {
                        navController.navigate(FacturaDestination.Dashboard.route) {
                            popUpTo(FacturaDestination.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            // 2. Onboarding Screen
            composable(FacturaDestination.Onboarding.route) {
                OnboardingScreen(
                    onOnboardingComplete = {
                        navController.navigate(FacturaDestination.Dashboard.route) {
                            popUpTo(FacturaDestination.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            // 3. Dashboard Screen
            composable(FacturaDestination.Dashboard.route) {
                DashboardScreen(
                    onNavigateToCreateInvoice = { navController.navigate(FacturaDestination.CreateInvoice.createRoute()) },
                    onNavigateToInvoiceDetail = { invoiceId -> navController.navigate(FacturaDestination.InvoiceDetail.createRoute(invoiceId)) },
                    onNavigateToInvoices = { navController.navigate(FacturaDestination.InvoiceList.route) },
                    onNavigateToSettings = { navController.navigate(FacturaDestination.Settings.route) }
                )
            }

            // 4. Invoice List Screen
            composable(FacturaDestination.InvoiceList.route) {
                InvoiceListScreen(
                    onNavigateToCreateInvoice = { navController.navigate(FacturaDestination.CreateInvoice.createRoute()) },
                    onNavigateToInvoiceDetail = { invoiceId -> navController.navigate(FacturaDestination.InvoiceDetail.createRoute(invoiceId)) }
                )
            }

            // 5. Invoice Detail Screen
            composable(
                route = FacturaDestination.InvoiceDetail.route,
                arguments = listOf(navArgument("invoiceId") { type = NavType.StringType })
            ) { backStackEntry ->
                val invoiceId = backStackEntry.arguments?.getString("invoiceId") ?: ""
                InvoiceDetailScreen(
                    invoiceId = invoiceId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRecordPayment = { invId -> navController.navigate(FacturaDestination.PaymentRecord.createRoute(invId)) },
                    onNavigateToEditInvoice = { invId -> navController.navigate(FacturaDestination.CreateInvoice.createRoute(invId)) }
                )
            }

            // 6. Create / Edit Invoice Screen
            composable(
                route = FacturaDestination.CreateInvoice.route,
                arguments = listOf(navArgument("invoiceId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val invoiceId = backStackEntry.arguments?.getString("invoiceId")
                CreateInvoiceScreen(
                    invoiceIdToEdit = invoiceId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCreateClient = { navController.navigate(FacturaDestination.CreateEditClient.createRoute()) }
                )
            }

            // 7. Client List Screen
            composable(FacturaDestination.ClientList.route) {
                ClientListScreen(
                    onNavigateToCreateClient = { navController.navigate(FacturaDestination.CreateEditClient.createRoute()) },
                    onNavigateToClientDetail = { clientId -> navController.navigate(FacturaDestination.ClientDetail.createRoute(clientId)) }
                )
            }

            // 8. Client Detail Screen
            composable(
                route = FacturaDestination.ClientDetail.route,
                arguments = listOf(navArgument("clientId") { type = NavType.StringType })
            ) { backStackEntry ->
                val clientId = backStackEntry.arguments?.getString("clientId") ?: ""
                ClientDetailScreen(
                    clientId = clientId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEditClient = { cId -> navController.navigate(FacturaDestination.CreateEditClient.createRoute(cId)) },
                    onNavigateToCreateInvoice = { navController.navigate(FacturaDestination.CreateInvoice.createRoute()) },
                    onNavigateToInvoiceDetail = { invoiceId -> navController.navigate(FacturaDestination.InvoiceDetail.createRoute(invoiceId)) }
                )
            }

            // 9. Create / Edit Client Screen
            composable(
                route = FacturaDestination.CreateEditClient.route,
                arguments = listOf(navArgument("clientId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val clientId = backStackEntry.arguments?.getString("clientId")
                CreateEditClientScreen(
                    clientIdToEdit = clientId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 10. Expense List Screen
            composable(FacturaDestination.ExpenseList.route) {
                ExpenseListScreen(
                    onNavigateToCreateExpense = { navController.navigate(FacturaDestination.CreateExpense.createRoute()) },
                    onNavigateToEditExpense = { expenseId -> navController.navigate(FacturaDestination.CreateExpense.createRoute(expenseId)) }
                )
            }

            // 11. Create / Edit Expense Screen
            composable(
                route = FacturaDestination.CreateExpense.route,
                arguments = listOf(navArgument("expenseId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val expenseId = backStackEntry.arguments?.getString("expenseId")
                CreateExpenseScreen(
                    expenseIdToEdit = expenseId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 12. Payment Record Screen
            composable(
                route = FacturaDestination.PaymentRecord.route,
                arguments = listOf(navArgument("invoiceId") { type = NavType.StringType })
            ) { backStackEntry ->
                val invoiceId = backStackEntry.arguments?.getString("invoiceId") ?: ""
                PaymentRecordScreen(
                    invoiceId = invoiceId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 13. Settings Screen
            composable(FacturaDestination.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 14. Reports Screen
            composable(FacturaDestination.Reports.route) {
                ReportsScreen()
            }
        }
    }
}
