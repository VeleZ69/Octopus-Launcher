package com.octopus.launcher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.octopus.launcher.data.AppInfo

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FavoritesRow(
    apps: List<AppInfo>,
    favorites: Set<String>,
    onAppClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (apps.isEmpty()) return
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "Favorites",
            style = MaterialTheme.typography.headlineMedium,
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(
                items = apps,
                key = { app -> app.packageName } // Add key for better performance
            ) { app ->
                AppCard(
                    app = app,
                    isFavorite = favorites.contains(app.packageName),
                    onFavoriteClick = { onFavoriteClick(app.packageName) },
                    onClick = { onAppClick(app.packageName) }
                )
            }
        }
    }
}

