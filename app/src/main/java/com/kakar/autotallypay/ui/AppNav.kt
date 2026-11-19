package com.kakar.autotallypay.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kakar.autotallypay.ui.screens.AddBillScreen
import com.kakar.autotallypay.ui.screens.CheckerScreen
import com.kakar.autotallypay.ui.screens.HomeScreen
import com.kakar.autotallypay.ui.screens.InboxScreen
import com.kakar.autotallypay.ui.screens.KhataScreen
import com.kakar.autotallypay.ui.screens.LedgerScreen
import com.kakar.autotallypay.ui.screens.SettingsScreen

object Routes {
    const val HOME = "home"
    const val KHATA = "khata"
    const val CHECK = "check"
    const val INBOX = "inbox"
    const val SETTINGS = "settings"
    const val LEDGER = "ledger/{customerId}"
    const val ADD_BILL = "addBill?customerId={customerId}"

    fun ledger(customerId: Long) = "ledger/$customerId"
    fun addBill(customerId: Long?) =
        if (customerId == null) "addBill" else "addBill?customerId=$customerId"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNav(vm: MainViewModel) {
    val navController = rememberNavController()
    val strings by vm.strings.collectAsState()
    val notice by vm.notice.collectAsState()
    val sharedSmsText by vm.sharedSmsText.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.HOME

    // Show one-shot notices as a snackbar.
    LaunchedEffect(notice) {
        notice?.let {
            snackbarHostState.showSnackbar(strings.noticeText(it))
            vm.clearNotice()
        }
    }

    // A shared SMS opens the Inbox confirm dialog.
    LaunchedEffect(sharedSmsText) {
        if (sharedSmsText != null && currentRoute != Routes.INBOX) {
            navController.navigate(Routes.INBOX)
        }
    }

    val title = when {
        currentRoute == Routes.HOME -> strings.home
        currentRoute == Routes.KHATA -> strings.khata
        currentRoute == Routes.CHECK -> strings.didMoneyCome
        currentRoute == Routes.INBOX -> strings.inbox
        currentRoute == Routes.SETTINGS -> strings.settings
        currentRoute.startsWith("ledger") -> strings.khata
        currentRoute.startsWith("addBill") -> strings.addBill
        else -> strings.appName
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(title) },
                actions = {
                    if (currentRoute != Routes.SETTINGS) {
                        IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                            Icon(Icons.Filled.Settings, contentDescription = strings.settings)
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == Routes.HOME,
                    onClick = { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) } },
                    icon = { Icon(Icons.Filled.Home, contentDescription = strings.home) },
                    label = { Text(strings.home) }
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.KHATA || currentRoute.startsWith("ledger"),
                    onClick = { navController.navigate(Routes.KHATA) { popUpTo(Routes.HOME) } },
                    icon = { Icon(Icons.Filled.Person, contentDescription = strings.khata) },
                    label = { Text(strings.khata) }
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.CHECK,
                    onClick = { navController.navigate(Routes.CHECK) { popUpTo(Routes.HOME) } },
                    icon = { Icon(Icons.Filled.Search, contentDescription = strings.check) },
                    label = { Text(strings.check) }
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.INBOX,
                    onClick = { navController.navigate(Routes.INBOX) { popUpTo(Routes.HOME) } },
                    icon = { Icon(Icons.Filled.Mail, contentDescription = strings.inbox) },
                    label = { Text(strings.inbox) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    vm = vm,
                    onOpenInbox = { navController.navigate(Routes.INBOX) },
                    onOpenCheck = { navController.navigate(Routes.CHECK) },
                    onAddBill = { navController.navigate(Routes.addBill(null)) },
                    onAddCustomer = { navController.navigate(Routes.KHATA) }
                )
            }
            composable(Routes.KHATA) {
                KhataScreen(vm = vm, onOpenCustomer = { navController.navigate(Routes.ledger(it)) })
            }
            composable(Routes.CHECK) { CheckerScreen(vm = vm) }
            composable(Routes.INBOX) { InboxScreen(vm = vm) }
            composable(Routes.SETTINGS) { SettingsScreen(vm = vm) }
            composable(
                Routes.LEDGER,
                arguments = listOf(navArgument("customerId") { type = NavType.LongType })
            ) { entry ->
                val customerId = entry.arguments?.getLong("customerId") ?: -1L
                LedgerScreen(
                    vm = vm,
                    customerId = customerId,
                    onAddBill = { navController.navigate(Routes.addBill(it)) },
                    onCustomerDeleted = { navController.popBackStack() }
                )
            }
            composable(
                Routes.ADD_BILL,
                arguments = listOf(
                    navArgument("customerId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { entry ->
                val raw = entry.arguments?.getLong("customerId") ?: -1L
                AddBillScreen(
                    vm = vm,
                    preselectedCustomerId = if (raw == -1L) null else raw,
                    onSaved = { navController.popBackStack() }
                )
            }
        }
    }
}
