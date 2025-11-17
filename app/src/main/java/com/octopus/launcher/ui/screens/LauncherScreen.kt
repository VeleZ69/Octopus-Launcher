package com.octopus.launcher.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.octopus.launcher.ui.components.BlurredBackground
import com.octopus.launcher.ui.components.QuickActionsWidget
import com.octopus.launcher.ui.components.SearchBar
import com.octopus.launcher.ui.components.TimeWeatherWidget
import com.octopus.launcher.ui.components.TopNavigationBar
import com.octopus.launcher.ui.viewmodel.LauncherViewModel
import com.octopus.launcher.ui.viewmodel.WeatherViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LauncherScreen(
    viewModel: LauncherViewModel = viewModel(),
    weatherViewModel: WeatherViewModel = viewModel(),
    onNavigateToSettings: () -> Unit = {},
    onShowMoreMenu: () -> Unit = {}
) {
    val apps by viewModel.apps.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val backgroundColors by viewModel.backgroundColors.collectAsState()
    
    var selectedMenu by remember { mutableStateOf("Главная") }
    var showSearch by remember { mutableStateOf(false) }
    
    // Get popular apps from popularApps repository (apps added to top bar)
    val popularAppsList by viewModel.popularApps.collectAsState()
    val popularAppsSet by viewModel.popularAppsSet.collectAsState()
    // Recalculate popular apps maintaining order
    val popularApps = popularAppsList.mapNotNull { packageName ->
        apps.find { it.packageName == packageName }
    }
    
    // Get background colors and image path
    val (color1, color2, color3) = backgroundColors
    val backgroundImagePath by viewModel.backgroundImagePath.collectAsState()
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Blurred background with custom colors or image
        BlurredBackground(color1, color2, color3, backgroundImagePath)
        
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top navigation bar
            TopNavigationBar(
                popularApps = popularApps,
                popularAppsSet = popularAppsSet,
                allApps = apps,
                onAppClick = viewModel::launchApp,
                onAppAdd = viewModel::addPopularApp,
                onAppRemove = viewModel::removePopularApp,
                onAppMove = viewModel::movePopularApp,
                onSearchClick = { showSearch = !showSearch },
                onMenuClick = { menu -> selectedMenu = menu },
                onSettingsClick = onNavigateToSettings,
                selectedMenu = selectedMenu,
                weatherViewModel = weatherViewModel
            )
            
            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
            ) {
                // Animated SearchBar with fade and slide down animation - optimized
                AnimatedVisibility(
                    visible = showSearch,
                    enter = fadeIn(
                        animationSpec = tween(200, easing = FastOutLinearInEasing)
                    ) + slideInVertically(
                        initialOffsetY = { -it / 3 },
                        animationSpec = tween(200, easing = FastOutLinearInEasing)
                    ),
                    exit = fadeOut(
                        animationSpec = tween(150, easing = FastOutLinearInEasing)
                    ) + slideOutVertically(
                        targetOffsetY = { -it / 3 },
                        animationSpec = tween(150, easing = FastOutLinearInEasing)
                    )
                ) {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = viewModel::setSearchQuery
                    )
                }
                
                // Animated content for menu switching - smooth animations
                AnimatedContent(
                    targetState = selectedMenu,
                    transitionSpec = {
                        if (targetState == "Приложения") {
                            // Entering apps list - smooth animation
                            fadeIn(
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + slideInHorizontally(
                                initialOffsetX = { it / 4 },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) togetherWith fadeOut(
                                animationSpec = tween(250, easing = FastOutSlowInEasing)
                            ) + slideOutHorizontally(
                                targetOffsetX = { -it / 4 },
                                animationSpec = tween(250, easing = FastOutSlowInEasing)
                            )
                        } else {
                            // Entering home - smooth animation
                            fadeIn(
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + slideInHorizontally(
                                initialOffsetX = { -it / 4 },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) togetherWith fadeOut(
                                animationSpec = tween(250, easing = FastOutSlowInEasing)
                            ) + slideOutHorizontally(
                                targetOffsetX = { it / 4 },
                                animationSpec = tween(250, easing = FastOutSlowInEasing)
                            )
                        }
                    },
                    label = "menuContent"
                ) { menu ->
                    when (menu) {
                        "Главная" -> {
                            AnimatedContent(
                                targetState = showSearch && searchQuery.isNotBlank(),
                                transitionSpec = {
                                    if (targetState) {
                                        // Entering search results - faster
                                        fadeIn(
                                            animationSpec = tween(200, easing = FastOutLinearInEasing)
                                        ) + scaleIn(
                                            initialScale = 0.95f,
                                            animationSpec = tween(200, easing = FastOutLinearInEasing)
                                        ) togetherWith fadeOut(
                                            animationSpec = tween(150, easing = LinearOutSlowInEasing)
                                        )
                                    } else {
                                        // Exiting search results - faster
                                        fadeIn(
                                            animationSpec = tween(150, easing = LinearOutSlowInEasing)
                                        ) togetherWith fadeOut(
                                            animationSpec = tween(200, easing = FastOutLinearInEasing)
                                        ) + scaleOut(
                                            targetScale = 0.95f,
                                            animationSpec = tween(200, easing = FastOutLinearInEasing)
                                        )
                                    }
                                },
                                label = "searchContent"
                            ) { isSearching ->
                                if (isSearching) {
                                    // Show search results
                                    SearchResultsScreen(
                                        viewModel = viewModel
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Time and Weather Widget - use shared ViewModel
                                        TimeWeatherWidget(viewModel = weatherViewModel)
                                        
                                        // Quick actions widget at the bottom
                                        QuickActionsWidget(
                                            modifier = Modifier.padding(bottom = 32.dp)
                                        )
                                    }
                                }
                            }
                        }
                        "Приложения" -> {
                            // Apps list
                            AppsListScreen(
                                viewModel = viewModel
                            )
                        }
                        else -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                TimeWeatherWidget(viewModel = weatherViewModel)
                                QuickActionsWidget(
                                    modifier = Modifier.padding(bottom = 32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

