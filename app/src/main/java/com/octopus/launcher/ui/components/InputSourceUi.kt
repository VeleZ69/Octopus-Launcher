package com.octopus.launcher.ui.components

import android.media.tv.TvInputInfo
import android.util.Log
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.octopus.launcher.utils.TVKeyInjector
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.content.ContextCompat

private const val TAG_INPUT_UI = "InputSourceUi"

@Composable
private fun IconWithDrawable(
    drawable: android.graphics.drawable.Drawable?,
    contentDescription: String,
    iconSize: androidx.compose.ui.unit.Dp,
    tint: Color,
    fallbackIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val density = LocalDensity.current
    val iconSizePx = remember(iconSize, density) { 
        with(density) { iconSize.roundToPx() }
    }
    var iconBitmap by remember(drawable, iconSizePx) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    
    LaunchedEffect(drawable, iconSizePx) {
        iconBitmap = if (drawable != null) {
            withContext(Dispatchers.Default) {
                try {
                    drawable.toBitmap(iconSizePx, iconSizePx)?.asImageBitmap()
                } catch (e: Exception) {
                    Log.w(TAG_INPUT_UI, "Failed to convert drawable to bitmap", e)
                    null
                }
            }
        } else {
            null
        }
    }
    
    iconBitmap?.let { bitmap ->
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            contentScale = ContentScale.Fit
        )
    } ?: Icon(
        imageVector = fallbackIcon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(iconSize)
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun InputSourceIcon(
    input: TVKeyInjector.InputDescriptor,
    focusRequester: FocusRequester?,
    onSelect: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    var selectionHandled by remember { mutableStateOf(false) }
    
    // Same sizes as QuickActionButton
    val iconSize = 36.dp
    
    val context = LocalContext.current
    
    // Get custom icon drawable resource based on input label
    val customIconDrawable = remember(input.label) {
        val labelLower = input.label.lowercase().trim()
        val resId = when {
            labelLower == "tv" || (labelLower.contains("tv") && !labelLower.contains("iptv")) -> {
                context.resources.getIdentifier("tv", "drawable", context.packageName)
            }
            labelLower == "av" || labelLower.contains("av") -> {
                context.resources.getIdentifier("av", "drawable", context.packageName)
            }
            labelLower.contains("hdmi") -> {
                context.resources.getIdentifier("hdmi", "drawable", context.packageName)
            }
            else -> 0
        }
        resId.takeIf { it != 0 }?.let { ContextCompat.getDrawable(context, it) } ?: input.icon
    }
    
    // Use custom icon if available, otherwise use system icon
    val iconDrawable = customIconDrawable ?: input.icon
    
    // Animation for focus - same as QuickActionButton
    val animatedScale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "inputIconScale"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.8f,
        animationSpec = tween(durationMillis = 150),
        label = "inputIconAlpha"
    )
    
    val contentColor = Color.White.copy(alpha = if (isFocused) 0.95f else 0.75f)

    // Determine input type for fallback icon
    val isTuner = input.type == TvInputInfo.TYPE_TUNER
    
    // Handle selection action
    val handleSelection = {
        if (!selectionHandled) {
            selectionHandled = true
            onSelect()
        }
    }
    
    // Label text style - MaterialTheme is already cached by the system
    val labelTextStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White
    )
    
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                alpha = animatedAlpha
            }
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (!focusState.isFocused) {
                    selectionHandled = false
                }
            }
            .focusable()
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
            .onPreviewKeyEvent { event ->
                if (!isFocused) return@onPreviewKeyEvent false
                
                val keyCode = event.nativeKeyEvent?.keyCode
                val isSelectKey = keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
                        keyCode == AndroidKeyEvent.KEYCODE_ENTER ||
                        keyCode == AndroidKeyEvent.KEYCODE_BUTTON_A
                
                if (isSelectKey && event.type == KeyEventType.KeyUp) {
                    handleSelection()
                    true
                } else {
                    isSelectKey
                }
            }
            .clickable(onClick = handleSelection)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Popup with input name when focused
        if (isFocused) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-40).dp)
                    .zIndex(1f)
                    .wrapContentSize()
                    .widthIn(max = 200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = input.label,
                    style = labelTextStyle,
                    maxLines = 1 // Single line only
                )
            }
        }
        
        // Background box - same style as QuickActionButton
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    color = if (isFocused) {
                        Color.White.copy(alpha = 0.3f)
                    } else {
                        Color.White.copy(alpha = 0.1f)
                    }
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            // Icon - use drawable or fallback to Material icon
            IconWithDrawable(
                drawable = iconDrawable,
                contentDescription = input.label,
                iconSize = iconSize,
                tint = contentColor,
                fallbackIcon = if (isTuner) Icons.Filled.Tv else Icons.Filled.Input
            )
        }
    }
}
