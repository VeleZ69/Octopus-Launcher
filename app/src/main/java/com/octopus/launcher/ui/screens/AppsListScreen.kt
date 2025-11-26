package com.octopus.launcher.ui.screens

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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.octopus.launcher.ui.components.AppCard
import com.octopus.launcher.ui.viewmodel.LauncherViewModel
import com.octopus.launcher.R

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppsListScreen(
    viewModel: LauncherViewModel = viewModel()
) {
    val apps by viewModel.apps.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    
    // Memoize apps list to avoid unnecessary recomposition
    val userApps = remember(apps) { apps }
    val gridState = rememberLazyGridState()
    
    // Optimize grid performance - use fixed cell size for better performance
    val density = LocalDensity.current
    val cellSize = remember(density) { with(density) { 104.dp } }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        val appsTitle = stringResource(R.string.apps_title)
        val appsNotFound = stringResource(R.string.apps_not_found)
        
        Text(
            text = appsTitle,
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (userApps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = appsNotFound,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = cellSize),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = userApps,
                    key = { _, app -> app.packageName }
                ) { _, app ->
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

