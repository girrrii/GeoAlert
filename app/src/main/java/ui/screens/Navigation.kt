package com.giri.geoalert.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.giri.geoalert.viewmodel.GeoAlertViewModel

@Composable
fun AppNavigation(viewModel: GeoAlertViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = "Map") },
                    label = { Text("Map") },
                    selected = currentRoute == "map",
                    onClick = { navController.navigate("map") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "Alerts") },
                    label = { Text("Alerts") },
                    selected = currentRoute == "alerts",
                    onClick = { navController.navigate("alerts") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "History") },
                    label = { Text("History") },
                    selected = currentRoute == "history",
                    onClick = { navController.navigate("history") }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "map",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("map") {
                MapScreen(viewModel = viewModel)
            }
            composable("alerts") {
                AlertListScreen(viewModel = viewModel)
            }
            composable("history") {
                HistoryScreen(viewModel = viewModel)
            }
        }
    }
}