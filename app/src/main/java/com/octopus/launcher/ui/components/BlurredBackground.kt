package com.octopus.launcher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BlurredBackground(
    color1: Color = Color(0xFF4A90A4),
    color2: Color = Color(0xFF5BA68C),
    color3: Color = Color(0xFF6B9E7A),
    backgroundImagePath: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val displayMetrics = context.resources.displayMetrics
    val screenWidth = displayMetrics.widthPixels
    val screenHeight = displayMetrics.heightPixels
    
    // Use global cache - try to get from cache synchronously first to avoid showing gradient
    // This is critical - must be synchronous to prevent gradient flash
    var blurredImageBitmap by remember(backgroundImagePath) { 
        // Try to get from cache immediately if available - this is synchronous
        mutableStateOf<ImageBitmap?>(
            BackgroundImageCache.getCachedBitmap(backgroundImagePath)
        )
    }
    
    // Load image from cache or file asynchronously (only if not in cache)
    LaunchedEffect(backgroundImagePath) {
        if (backgroundImagePath != null && blurredImageBitmap == null) {
            try {
                // Load in background thread to not block UI
                val loadedBitmap = withContext(Dispatchers.Default) {
                    BackgroundImageCache.getBitmap(
                        backgroundImagePath,
                        screenWidth,
                        screenHeight
                    )
                }
                
                blurredImageBitmap = loadedBitmap
            } catch (e: CancellationException) {
                // Silently ignore cancellation
                blurredImageBitmap = null
            } catch (e: Exception) {
                android.util.Log.e("BlurredBackground", "Error loading background image", e)
                blurredImageBitmap = null
            }
        } else if (backgroundImagePath == null) {
            // Clear bitmap when no image is set
            blurredImageBitmap = null
        }
    }
    
    // Animate transition between gradient and image for smooth appearance
    val imageAlpha by animateFloatAsState(
        targetValue = if (blurredImageBitmap != null) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "backgroundImageAlpha"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(-1f)
    ) {
        // Always show gradient as base layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(color1, color2, color3)
                    )
                )
        )
        
        // Show image on top with fade animation if available
        blurredImageBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = "Background",
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(imageAlpha),
                contentScale = ContentScale.Crop
            )
            // Add dark overlay for better text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(imageAlpha)
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        }
    }
}
