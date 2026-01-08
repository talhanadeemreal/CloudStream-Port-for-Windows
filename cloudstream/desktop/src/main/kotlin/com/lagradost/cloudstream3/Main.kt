package com.lagradost.cloudstream3

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.lagradost.cloudstream3.ui.screens.*
import com.lagradost.cloudstream3.ui.theme.CloudStreamTheme
import com.lagradost.cloudstream3.plugins.PluginLoader

enum class Screen {
    HOME, SEARCH, EXTENSIONS, SETTINGS
}

sealed class NavigationState {
    object Home : NavigationState()
    object Search : NavigationState()
    object Extensions : NavigationState()
    object Settings : NavigationState()
    data class Details(val result: SearchResult) : NavigationState()
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "CloudStream - Windows Desktop"
    ) {
        // Load plugins on startup
        LaunchedEffect(Unit) {
            PluginLoader.loadAllPlugins()
        }
        
        CloudStreamTheme {
            var navigationState by remember { mutableStateOf<NavigationState>(NavigationState.Search) }
            
            Surface(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Navigation Rail (only show when not in Details view)
                    if (navigationState !is NavigationState.Details) {
                        NavigationRail {
                            NavigationRailItem(
                                selected = navigationState is NavigationState.Home,
                                onClick = { navigationState = NavigationState.Home },
                                icon = { Icon(Icons.Default.Home, "Home") },
                                label = { Text("Home") }
                            )
                            NavigationRailItem(
                                selected = navigationState is NavigationState.Search,
                                onClick = { navigationState = NavigationState.Search },
                                icon = { Icon(Icons.Default.Search, "Search") },
                                label = { Text("Search") }
                            )
                            NavigationRailItem(
                                selected = navigationState is NavigationState.Extensions,
                                onClick = { navigationState = NavigationState.Extensions },
                                icon = { Icon(Icons.Default.Extension, "Extensions") },
                                label = { Text("Extensions") }
                            )
                            NavigationRailItem(
                                selected = navigationState is NavigationState.Settings,
                                onClick = { navigationState = NavigationState.Settings },
                                icon = { Icon(Icons.Default.Settings, "Settings") },
                                label = { Text("Settings") }
                            )
                        }
                    }
                    
                    // Content Area
                    when (val state = navigationState) {
                        is NavigationState.Home -> HomeScreen()
                        is NavigationState.Search -> SearchScreen(
                            onResultClick = { result ->
                                navigationState = NavigationState.Details(result)
                            }
                        )
                        is NavigationState.Extensions -> ExtensionsScreen()
                        is NavigationState.Settings -> SettingsScreen()
                        is NavigationState.Details -> DetailsScreen(
                            result = state.result,
                            onBack = { navigationState = NavigationState.Search }
                        )
                    }
                }
            }
        }
    }
}
