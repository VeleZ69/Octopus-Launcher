package com.octopus.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.graphics.drawable.GradientDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.octopus.launcher.R
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.octopus.launcher.ui.components.BlurredBackground
import com.octopus.launcher.ui.components.ImageFileBrowser
import com.octopus.launcher.ui.viewmodel.SettingsViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Intent
import android.provider.Settings
import android.content.pm.PackageManager
import com.octopus.launcher.utils.LauncherManager

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onColorsChanged: () -> Unit = {},
    onImageChanged: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? com.octopus.launcher.MainActivity
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    
    // Adaptive padding based on screen size
    val horizontalPadding = when {
        screenWidth < 600.dp -> 16.dp
        screenWidth < 1200.dp -> 32.dp
        else -> 48.dp
    }
    val verticalPadding = when {
        screenHeight < 600.dp -> 16.dp
        screenHeight < 900.dp -> 24.dp
        else -> 32.dp
    }
    val titleTopPadding = when {
        screenHeight < 600.dp -> 4.dp
        screenHeight < 900.dp -> 8.dp
        else -> 16.dp
    }
    val color1 by viewModel.color1.collectAsState()
    val color2 by viewModel.color2.collectAsState()
    val color3 by viewModel.color3.collectAsState()
    val backgroundImagePath by viewModel.backgroundImagePath.collectAsState()

    // Default launcher status
    var isDefaultLauncher by remember { mutableStateOf(false) }
    val launcherManager = remember(context) { LauncherManager(context) }
    LaunchedEffect(Unit) {
        isDefaultLauncher = launcherManager.isDefaultLauncher()
    }
    
    // Show image file browser
    var showImageBrowser by remember { mutableStateOf(false) }
    var browserKey by remember { mutableStateOf(0) }
    
    // Force refresh backgroundImagePath from repository when screen is shown - optimized
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val settingsRepository = com.octopus.launcher.data.SettingsRepository(context)
            val currentPath = settingsRepository.getBackgroundImagePath()
            if (currentPath != backgroundImagePath) {
                withContext(Dispatchers.Main) {
                    viewModel.setBackgroundImagePath(currentPath)
                }
            }
        }
    }
    
    // Update window background in real-time when colors change - debounced for performance
    LaunchedEffect(color1, color2, color3) {
        delay(50) // Small delay to batch updates
        activity?.let { act ->
            val color1Int = android.graphics.Color.argb(
                (color1.alpha * 255).toInt(),
                (color1.red * 255).toInt(),
                (color1.green * 255).toInt(),
                (color1.blue * 255).toInt()
            )
            val color2Int = android.graphics.Color.argb(
                (color2.alpha * 255).toInt(),
                (color2.red * 255).toInt(),
                (color2.green * 255).toInt(),
                (color2.blue * 255).toInt()
            )
            val color3Int = android.graphics.Color.argb(
                (color3.alpha * 255).toInt(),
                (color3.red * 255).toInt(),
                (color3.green * 255).toInt(),
                (color3.blue * 255).toInt()
            )
            
            val gradientDrawable = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(color1Int, color2Int, color3Int)
            )
            act.window.setBackgroundDrawable(gradientDrawable)
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        BlurredBackground(color1, color2, color3, backgroundImagePath)
        
        if (showImageBrowser) {
            key(browserKey) {
                ImageFileBrowser(
                    onImageSelected = { imagePath ->
                        try {
                            // Copy file to app's internal storage for better access
                            val sourceFile = java.io.File(imagePath)
                            if (sourceFile.exists()) {
                                val destFile = java.io.File(context.filesDir, "background_image.jpg")
                                sourceFile.copyTo(destFile, overwrite = true)
                                viewModel.setBackgroundImagePath(destFile.absolutePath)
                                // Force refresh by updating the state immediately
                                android.util.Log.d("SettingsScreen", "Background image set to: ${destFile.absolutePath}")
                                // Notify parent to refresh LauncherViewModel
                                onImageChanged()
                            } else {
                                // If file doesn't exist, try to use the path directly
                                viewModel.setBackgroundImagePath(imagePath)
                                android.util.Log.d("SettingsScreen", "Background image set to: $imagePath")
                                // Notify parent to refresh LauncherViewModel
                                onImageChanged()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("SettingsScreen", "Error setting image path", e)
                        }
                        showImageBrowser = false
                    },
                    onBack = { showImageBrowser = false },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(
                when {
                    screenHeight < 600.dp -> 8.dp
                    screenHeight < 900.dp -> 10.dp
                    else -> 12.dp
                }
            )
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = titleTopPadding, bottom = 8.dp)
            )
            
            Text(
                text = stringResource(R.string.background_colors),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 16.dp)
            )
            
            // Gradient colors with labels
            ColorPicker(
                label = stringResource(R.string.gradient_1),
                color = color1,
                onColorChange = viewModel::setColor1
            )
            
            ColorPicker(
                label = stringResource(R.string.gradient_2),
                color = color2,
                onColorChange = viewModel::setColor2
            )
            
            ColorPicker(
                label = stringResource(R.string.gradient_3),
                color = color3,
                onColorChange = viewModel::setColor3
            )
            
            // Background image option
            Text(
                text = stringResource(R.string.background_image),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Select image button - limit width to allow delete button to fit
                var isImageButtonFocused by remember { mutableStateOf(false) }
                // Unified animations - same as PopularAppIcon
                val animatedScale by animateFloatAsState(
                    targetValue = if (isImageButtonFocused) 1.15f else 1f,
                    animationSpec = tween(durationMillis = 150),
                    label = "imageButtonScale"
                )
                val animatedAlpha by animateFloatAsState(
                    targetValue = if (isImageButtonFocused) 1f else 0.8f,
                    animationSpec = tween(durationMillis = 150),
                    label = "imageButtonAlpha"
                )
                    Box(
                        modifier = Modifier
                            // Do not expand to full width
                            .graphicsLayer {
                                scaleX = animatedScale
                                scaleY = animatedScale
                                alpha = animatedAlpha
                            }
                            .onFocusChanged { isImageButtonFocused = it.isFocused }
                            .focusable()
                            .clickable {
                                browserKey++ // Force rescan by changing key
                                showImageBrowser = true
                            }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                    // Light glow effect when focused
                    if (isImageButtonFocused) {
                        Box(
                            modifier = Modifier
                                // Match content size
                                .padding(4.dp)
                                .background(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .blur(8.dp)
                        )
                    }
                    
                    // Main button background - lighter when focused
                    Box(
                        modifier = Modifier
                            // Match content size
                            .defaultMinSize(minHeight = 56.dp) // increase height ~40%
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                color = if (isImageButtonFocused) {
                                    Color.White.copy(alpha = 0.35f) // Lighter when focused
                                } else {
                                    Color.White.copy(alpha = 0.15f)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (backgroundImagePath != null) stringResource(R.string.change_image) else stringResource(R.string.select_image),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
                
                // Clear image button (only show if image is set)
                if (backgroundImagePath != null) {
                    android.util.Log.d("SettingsScreen", "Rendering delete button, backgroundImagePath: $backgroundImagePath")
                    var isDeleteButtonFocused by remember(backgroundImagePath) { mutableStateOf(false) }
                    // Unified animations - same as PopularAppIcon
                    val animatedScale by animateFloatAsState(
                        targetValue = if (isDeleteButtonFocused) 1.15f else 1f,
                        animationSpec = tween(durationMillis = 150),
                        label = "deleteButtonScale"
                    )
                    val animatedAlpha by animateFloatAsState(
                        targetValue = if (isDeleteButtonFocused) 1f else 0.8f,
                        animationSpec = tween(durationMillis = 150),
                        label = "deleteButtonAlpha"
                    )
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = animatedScale
                                scaleY = animatedScale
                                alpha = animatedAlpha
                            }
                            .onFocusChanged { isDeleteButtonFocused = it.isFocused }
                            .focusable()
                            .defaultMinSize(minHeight = 28.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                color = Color.Red.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                android.util.Log.d("SettingsScreen", "Delete button clicked, clearing background image")
                                viewModel.clearBackgroundImage()
                                // Force state update immediately
                                viewModel.setBackgroundImagePath(null)
                                onImageChanged() // Notify parent to refresh LauncherViewModel
                            }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete_image),
                                tint = Color.Red,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = stringResource(R.string.delete),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color.Red.copy(alpha = 0.9f)
                            )
                        }
                    }
                } else {
                    android.util.Log.d("SettingsScreen", "backgroundImagePath is null, not rendering delete button")
                }
            }

            // Preset colors - closer to color pickers
            Text(
                text = stringResource(R.string.presets),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 8.dp) // Reduced spacing
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    PresetButton(
                        label = stringResource(R.string.preset_ocean),
                        colors = Triple(
                            Color(0xFF4A90A4),
                            Color(0xFF5BA68C),
                            Color(0xFF6B9E7A)
                        ),
                        onClick = {
                            viewModel.setColor1(Color(0xFF4A90A4))
                            viewModel.setColor2(Color(0xFF5BA68C))
                            viewModel.setColor3(Color(0xFF6B9E7A))
                        }
                    )
                }
                
                item {
                    PresetButton(
                        label = stringResource(R.string.preset_sunset),
                        colors = Triple(
                            Color(0xFFFF6B6B),
                            Color(0xFFFF8E53),
                            Color(0xFFFFA07A)
                        ),
                        onClick = {
                            viewModel.setColor1(Color(0xFFFF6B6B))
                            viewModel.setColor2(Color(0xFFFF8E53))
                            viewModel.setColor3(Color(0xFFFFA07A))
                        }
                    )
                }
                
                item {
                    PresetButton(
                        label = stringResource(R.string.preset_purple),
                        colors = Triple(
                            Color(0xFF6C5CE7),
                            Color(0xFFA29BFE),
                            Color(0xFFDDA0DD)
                        ),
                        onClick = {
                            viewModel.setColor1(Color(0xFF6C5CE7))
                            viewModel.setColor2(Color(0xFFA29BFE))
                            viewModel.setColor3(Color(0xFFDDA0DD))
                        }
                    )
                }
                
                item {
                    PresetButton(
                        label = stringResource(R.string.preset_dark),
                        colors = Triple(
                            Color(0xFF1A1A1A),
                            Color(0xFF2D2D2D),
                            Color(0xFF3A3A3A)
                        ),
                        onClick = {
                            viewModel.setColor1(Color(0xFF1A1A1A))
                            viewModel.setColor2(Color(0xFF2D2D2D))
                            viewModel.setColor3(Color(0xFF3A3A3A))
                        }
                    )
                }
                
                item {
                    PresetButton(
                        label = stringResource(R.string.preset_dark_blue),
                        colors = Triple(
                            Color(0xFF1E3A5F),
                            Color(0xFF2D4A6F),
                            Color(0xFF3A5A7F)
                        ),
                        onClick = {
                            viewModel.setColor1(Color(0xFF1E3A5F))
                            viewModel.setColor2(Color(0xFF2D4A6F))
                            viewModel.setColor3(Color(0xFF3A5A7F))
                        }
                    )
                }
                
                item {
                    PresetButton(
                        label = stringResource(R.string.preset_dark_green),
                        colors = Triple(
                            Color(0xFF1B3A2E),
                            Color(0xFF2D4A3E),
                            Color(0xFF3A5A4E)
                        ),
                        onClick = {
                            viewModel.setColor1(Color(0xFF1B3A2E))
                            viewModel.setColor2(Color(0xFF2D4A3E))
                            viewModel.setColor3(Color(0xFF3A5A4E))
                        }
                    )
                }
                
                item {
                    PresetButton(
                        label = stringResource(R.string.preset_black),
                        colors = Triple(
                            Color(0xFF000000),
                            Color(0xFF1A1A1A),
                            Color(0xFF2D2D2D)
                        ),
                        onClick = {
                            viewModel.setColor1(Color(0xFF000000))
                            viewModel.setColor2(Color(0xFF1A1A1A))
                            viewModel.setColor3(Color(0xFF2D2D2D))
                        }
                    )
                }
            }
            
            // Launcher actions - below presets
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                // Default launcher button (moved under presets) - same style as image button
                var isDefaultButtonFocused by remember { mutableStateOf(false) }
                val defaultScale by animateFloatAsState(
                    targetValue = if (isDefaultButtonFocused) 1.15f else 1f,
                    animationSpec = tween(durationMillis = 150),
                    label = "defaultLauncherButtonScale"
                )
                val defaultAlpha by animateFloatAsState(
                    targetValue = if (isDefaultButtonFocused) 1f else 0.8f,
                    animationSpec = tween(durationMillis = 150),
                    label = "defaultLauncherButtonAlpha"
                )
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = defaultScale
                            scaleY = defaultScale
                            alpha = defaultAlpha
                        }
                        .onFocusChanged { isDefaultButtonFocused = it.isFocused }
                        .focusable()
                        .then(
                            if (!isDefaultLauncher) {
                                Modifier.clickable { launcherManager.setAsDefaultLauncher() }
                            } else Modifier
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDefaultButtonFocused) {
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .background(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .blur(8.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                color = if (isDefaultButtonFocused) {
                                    Color.White.copy(alpha = 0.35f)
                                } else {
                                    Color.White.copy(alpha = 0.15f)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isDefaultLauncher) stringResource(R.string.already_default) else stringResource(R.string.set_as_default),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                // Helper to render action button with same style
                @Composable
                fun LauncherActionButton(
                    label: String,
                    enabled: Boolean = true,
                    onClick: () -> Unit
                ) {
                    var isFocused by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(
                        targetValue = if (isFocused) 1.15f else 1f,
                        animationSpec = tween(durationMillis = 150),
                        label = "${label}_scale"
                    )
                    val alpha by animateFloatAsState(
                        targetValue = if (isFocused) 1f else 0.8f,
                        animationSpec = tween(durationMillis = 150),
                        label = "${label}_alpha"
                    )
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                            }
                            .onFocusChanged { isFocused = it.isFocused }
                            .focusable()
                            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isFocused) {
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .background(
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .blur(8.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    color = if (isFocused) {
                                        Color.White.copy(alpha = 0.35f)
                                    } else {
                                        Color.White.copy(alpha = 0.15f)
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // 1) Принудительная установка
                LauncherActionButton(label = stringResource(R.string.force_install)) {
                    val result = launcherManager.forceSetLauncher()
                    android.util.Log.d("SettingsScreen", "forceSetLauncher: $result")
                    try {
                        android.widget.Toast.makeText(context, result.message, android.widget.Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {}
                }

                // 2) Перезапустить лаунчер
                val restartLauncherLabel = stringResource(R.string.restart_launcher)
                val restartingLauncherText = stringResource(R.string.restarting_launcher)
                LauncherActionButton(label = restartLauncherLabel) {
                    launcherManager.startOurLauncher()
                    try {
                        android.widget.Toast.makeText(context, restartingLauncherText, android.widget.Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {}
                }

                // 3) Детальный статус
                val detailedStatusLabel = stringResource(R.string.detailed_status)
                val ourLauncherActiveText = stringResource(R.string.our_launcher_active)
                val stockLauncherActiveText = stringResource(R.string.stock_launcher_active)
                LauncherActionButton(label = detailedStatusLabel) {
                    val status = launcherManager.getLauncherStatus()
                    android.util.Log.d("SettingsScreen", "LauncherStatus: $status")
                    val brief = if (status.isDefault) ourLauncherActiveText else stockLauncherActiveText
                    try {
                        android.widget.Toast.makeText(context, brief, android.widget.Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {}
                }

                // 4) Включить стоковые лаунчеры
                LauncherActionButton(label = stringResource(R.string.enable_stock_launchers)) {
                    val result = launcherManager.enableStockLaunchers()
                    android.util.Log.d("SettingsScreen", "enableStockLaunchers: $result")
                    try {
                        android.widget.Toast.makeText(context, result.message, android.widget.Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {}
                }
            }

            // Removed default launcher option per requirements
        }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ColorPicker(
    label: String,
    color: Color,
    onColorChange: (Color) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.9f)
        )
        
        // Color presets - scrollable
        val colorList = listOf(
            Color(0xFF4A90A4), Color(0xFF5BA68C), Color(0xFF6B9E7A), // Ocean
            Color(0xFFFF6B6B), Color(0xFFFF8E53), Color(0xFFFFA07A), // Sunset
            Color(0xFF6C5CE7), Color(0xFFA29BFE), Color(0xFFDDA0DD), // Purple
            Color(0xFF2ECC71), Color(0xFF52BE80), Color(0xFF7FB069), // Green
            Color(0xFF3498DB), Color(0xFF5DADE2), Color(0xFF85C1E9), // Blue
            Color(0xFF1A1A1A), Color(0xFF2D2D2D), Color(0xFF3A3A3A), // Dark
            Color(0xFF1E3A5F), Color(0xFF2D4A6F), Color(0xFF3A5A7F), // Dark Blue
            Color(0xFF1B3A2E), Color(0xFF2D4A3E), Color(0xFF3A5A4E), // Dark Green
            Color(0xFF000000), Color(0xFF1A1A1A)  // Black and light black
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup()
        ) {
            itemsIndexed(
                items = colorList,
                key = { index, _ -> index }
            ) { _, presetColor ->
                ColorCircle(
                    color = presetColor,
                    isSelected = color == presetColor,
                    onClick = { onColorChange(presetColor) }
                )
            }
        }
    }
}

@Composable
fun ColorCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    // Unified animations - same as buttons in settings
    val animatedScale by animateFloatAsState(
        targetValue = if (isFocused) 1.4f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "colorCircleScale"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.7f,
        animationSpec = tween(durationMillis = 200),
        label = "colorCircleAlpha"
    )
    
    Box(
        modifier = Modifier
            .size(36.dp)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                alpha = animatedAlpha
            }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
    ) {
        // Glow effect when focused
        if (isFocused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(
                        color = Color.White.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                    .blur(8.dp)
            )
        }
        
        // Main color circle
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(color)
                .then(
                    if (isSelected) {
                        Modifier.border(
                            width = 2.dp,
                            color = Color.White.copy(alpha = 0.9f),
                            shape = CircleShape
                        )
                    } else {
                        Modifier
                    }
                )
        )
        
        // Highlight overlay when focused
        if (isFocused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PresetButton(
    label: String,
    colors: Triple<Color, Color, Color>,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    // Unified animations - same as PopularAppIcon
    val animatedScale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "presetButtonScale"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.8f,
        animationSpec = tween(durationMillis = 150),
        label = "presetButtonAlpha"
    )
    
    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                alpha = animatedAlpha
            }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Light glow effect when focused - soft transparent glow around shape
        if (isFocused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
                    .align(Alignment.Center)
                    .background(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .blur(8.dp)
            )
        }
        
        // Main background - lighter when focused
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    color = if (isFocused) {
                        Color.White.copy(alpha = 0.35f) // Lighter when focused
                    } else {
                        Color.White.copy(alpha = 0.15f)
                    }
                )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    colors.toList().forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color)
                        )
                    }
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

