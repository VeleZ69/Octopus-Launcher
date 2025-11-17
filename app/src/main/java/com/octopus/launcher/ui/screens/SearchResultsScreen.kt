package com.octopus.launcher.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.octopus.launcher.ui.components.AppCard
import com.octopus.launcher.ui.viewmodel.LauncherViewModel

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SearchResultsScreen(
    viewModel: LauncherViewModel = viewModel()
) {
    val filteredApps by viewModel.filteredApps.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    // Track animation trigger for staggered animations
    var animationTrigger by remember(searchQuery) { mutableStateOf(0) }
    LaunchedEffect(searchQuery) {
        animationTrigger++
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Результаты поиска: \"$searchQuery\"",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ничего не найдено",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 180.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = filteredApps,
                    key = { _, app -> "${app.packageName}_$animationTrigger" }
                ) { index, app ->
                    // Staggered animation for each card
                    val delay = index * 30L
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(
                            animationSpec = tween(
                                durationMillis = 400,
                                delayMillis = delay.toInt(),
                                easing = FastOutSlowInEasing
                            )
                        ) + scaleIn(
                            initialScale = 0.8f,
                            animationSpec = tween(
                                durationMillis = 400,
                                delayMillis = delay.toInt(),
                                easing = FastOutSlowInEasing
                            )
                        ),
                        modifier = Modifier.animateItemPlacement()
                    ) {
                        AppCard(
                            app = app,
                            isFavorite = favorites.contains(app.packageName),
                            onFavoriteClick = { viewModel.toggleFavorite(app.packageName) },
                            onLongPress = { viewModel.addPopularApp(app.packageName) },
                            onClick = { viewModel.launchApp(app.packageName) }
                        )
                    }
                }
            }
        }
    }
}

