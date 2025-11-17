package com.octopus.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.octopus.launcher.data.MediaAppContent
import com.octopus.launcher.data.MediaContent
import com.octopus.launcher.data.MediaContentRepository

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VideoRecommendations(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mediaAppsContent by remember { mutableStateOf<List<MediaAppContent>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        val repository = MediaContentRepository(context)
        mediaAppsContent = repository.getMediaAppsWithContent()
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (mediaAppsContent.isEmpty()) {
            // Show generic recommendations if no media apps
            Text(
                text = "Рекомендации",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        } else {
            // Show content for each media app
            mediaAppsContent.forEach { appContent ->
                MediaAppRow(
                    appContent = appContent,
                    onContentClick = { content ->
                        val repository = MediaContentRepository(context)
                        repository.openMediaContent(content)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MediaAppRow(
    appContent: MediaAppContent,
    onContentClick: (MediaContent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = appContent.appName,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(appContent.contents) { content ->
                VideoRecommendationCard(
                    recommendation = content,
                    onClick = { onContentClick(content) }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VideoRecommendationCard(
    recommendation: MediaContent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    // Animation for focus - same as PopularAppIcon
    val animatedScale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "videoCardScale"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.8f,
        animationSpec = tween(durationMillis = 150),
        label = "videoCardAlpha"
    )
    
    // Generate color based on app package
    val thumbnailColor = remember(recommendation.appPackage) {
        when (recommendation.appPackage) {
            "com.google.android.youtube" -> Color(0xFFFF0000) // YouTube red
            "com.netflix.mediaclient" -> Color(0xFFE50914) // Netflix red
            "com.amazon.avod.thirdpartyclient" -> Color(0xFF00A8E1) // Prime Video blue
            "com.spotify.music" -> Color(0xFF1DB954) // Spotify green
            else -> Color(0xFF6C5CE7) // Default purple
        }
    }
    
    Card(
        onClick = onClick,
        modifier = modifier
            .width(320.dp)
            .height(180.dp)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                alpha = animatedAlpha
            }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .background(
                color = if (isFocused) {
                    Color.White.copy(alpha = 0.4f)
                } else {
                    Color.White.copy(alpha = 0.15f)
                }
            )
            .then(
                if (isFocused) {
                    Modifier.border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            ),
        scale = CardDefaults.scale(focusedScale = 1f) // Disable Card's built-in scale, use our own
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Thumbnail background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        color = thumbnailColor,
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(48.dp)
                )
            }
            
            // Info overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                    )
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = recommendation.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = recommendation.appName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

