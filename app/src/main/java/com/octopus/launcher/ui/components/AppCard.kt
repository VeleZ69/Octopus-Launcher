package com.octopus.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.octopus.launcher.data.AppInfo
import com.octopus.launcher.ui.theme.OctopusAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AppCard(
    app: AppInfo,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onLongPress: () -> Unit = {},
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    var isLongPressing by remember { mutableStateOf(false) }
    
    // Handle long press when focused
    LaunchedEffect(isFocused) {
        if (isFocused) {
            isLongPressing = false
            delay(2000) // 2 seconds for long press
            if (isFocused) {
                isLongPressing = true
                onLongPress()
                delay(500)
                isLongPressing = false
            }
        } else {
            isLongPressing = false
        }
    }
    
    // Same style as PopularAppIcon
    val iconSize = 60.dp
    val contentIconSize = 52.dp
    val density = LocalDensity.current
    val iconBitmapSizePx = remember(contentIconSize, density) { 
        (contentIconSize.value * density.density).toInt()
    }
    
    // Animation for focus - same as PopularAppIcon
    val animatedScale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "appCardScale"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.8f,
        animationSpec = tween(durationMillis = 150),
        label = "appCardAlpha"
    )
    
    // Optimize icon bitmap creation - async loading on background thread
    var iconBitmap by remember(app.packageName, iconBitmapSizePx) { mutableStateOf<ImageBitmap?>(null) }
    
    LaunchedEffect(app.packageName, iconBitmapSizePx) {
        iconBitmap = null // Reset while loading
        val currentPackageName = app.packageName
        if (app.icon != null) {
            try {
                iconBitmap = withContext(Dispatchers.Default) {
                    // Check if package name changed during loading
                    if (currentPackageName != app.packageName) return@withContext null
                    app.icon?.toBitmap(iconBitmapSizePx, iconBitmapSizePx)?.asImageBitmap()
                }
            } catch (e: CancellationException) {
                // Expected when composition leaves - just exit
                iconBitmap = null
            } catch (e: Exception) {
                android.util.Log.e("AppCard", "Error loading icon for ${app.packageName}", e)
                iconBitmap = null
            }
        }
    }
    
    Box(
        modifier = modifier
            .size(iconSize)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                alpha = animatedAlpha
            }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = {
                if (!isLongPressing) {
                    onClick()
                }
            }),
        contentAlignment = Alignment.Center
    ) {
        // Main background - same style as PopularAppIcon
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    color = if (isFocused) {
                        Color.White.copy(alpha = 0.3f) // Brighter when focused
                    } else {
                        Color.White.copy(alpha = 0.1f)
                    },
                    shape = RoundedCornerShape(12.dp)
                )
        )
        
        // Icon - with rounded corners for square icons (same as PopularAppIcon)
        iconBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = app.name,
                modifier = Modifier
                    .size(contentIconSize)
                    .clip(RoundedCornerShape(16.dp)), // Rounded corners like Google Play
                contentScale = ContentScale.Fit
            )
        }
        
        // Star icon overlay
        if (isFavorite) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Favorite",
                    tint = OctopusAccent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

