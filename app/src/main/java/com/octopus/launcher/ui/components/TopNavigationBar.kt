package com.octopus.launcher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Apps
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.octopus.launcher.R
import androidx.core.graphics.drawable.toBitmap
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.octopus.launcher.data.AppInfo
import com.octopus.launcher.data.WeatherInfo
import com.octopus.launcher.data.WeatherRepository
import com.octopus.launcher.ui.viewmodel.WeatherViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.content.Context

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TopNavigationBar(
    popularApps: List<AppInfo>,
    popularAppsSet: Set<String>,
    allApps: List<AppInfo>,
    onAppClick: (String) -> Unit,
    onAppAdd: (String) -> Unit,
    onAppRemove: (String) -> Unit,
    onAppMove: (Int, Int) -> Unit,
    onSearchClick: () -> Unit,
    onMenuClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedMenu: String,
    weatherViewModel: WeatherViewModel = viewModel()
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showMenuForApp by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val weatherInfo by weatherViewModel.weatherInfo.collectAsState()
    
    // Get localized strings
    val homeMenu = stringResource(R.string.menu_home)
    val appsMenu = stringResource(R.string.menu_apps)
    val settingsMenu = stringResource(R.string.menu_settings)
    var networkStatus by remember { mutableStateOf<NetworkStatus>(NetworkStatus.Unknown) }
    
    // Monitor network status - cache managers to avoid recreating
    val connectivityManager = remember(context) { 
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager 
    }
    val wifiManager = remember(context) { 
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager 
    }
    
    // Monitor network status - only update if changed
    LaunchedEffect(Unit) {
        try {
            while (true) {
                val newStatus = getNetworkStatus(context, connectivityManager, wifiManager)
                // Only update if status changed to avoid unnecessary recompositions
                if (networkStatus != newStatus) {
                    networkStatus = newStatus
                }
                delay(5000) // Check every 5 seconds instead of 2
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Expected when composable is removed - just exit
        }
    }
    
    // Periodically refresh weather - removed duplicate, handled in TimeWeatherWidget

    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
            )
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
            // Left side: Search icon and popular apps
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Search button with focus animations
                SearchIconButton(
                    onClick = onSearchClick
                )
                
                // Popular apps row - show max 7 icons with scrolling - optimized for performance
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .height(56.dp)
                        .fillMaxWidth(0.55f) // Limit width to allow scrolling when more than 7 icons
                ) {
                    itemsIndexed(
                        items = popularApps,
                        key = { _, app -> app.packageName } // Add key for better performance
                    ) { index, app ->
                        PopularAppIcon(
                            app = app,
                            index = index,
                            onAppClick = { onAppClick(app.packageName) },
                            onLongPress = { }, // Not used anymore
                            onMenuPress = { showMenuForApp = app.packageName }
                        )
                    }
                    
                    // Add button
                    item {
                        AddAppButton(
                            onClick = { showAddDialog = true }
                        )
                    }
                }
            }
            
            // Right side: Navigation menu and icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavigationMenu(
                    items = listOf(homeMenu, appsMenu, settingsMenu),
                    selectedItem = selectedMenu,
                    onItemClick = { item ->
                        if (item == settingsMenu) {
                            onSettingsClick()
                        } else {
                            onMenuClick(item)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${weatherInfo?.temperature ?: 22}°",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Network status icon
                NetworkStatusIcon(networkStatus = networkStatus)
            }
        }
    }
    
    // Add app dialog
    if (showAddDialog) {
        AddAppDialog(
            availableApps = allApps.filter { app -> 
                !popularAppsSet.contains(app.packageName)
            },
            onAppSelected = { packageName ->
                onAppAdd(packageName)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
    
    // Menu for app management
    showMenuForApp?.let { packageName ->
        val app = popularApps.find { it.packageName == packageName }
        if (app != null) {
            val index = popularApps.indexOf(app)
            AppManagementMenu(
                app = app,
                appIndex = index,
                canMoveLeft = index > 0,
                canMoveRight = index < popularApps.size - 1,
                onMoveLeft = {
                    if (index > 0) {
                        onAppMove(index, index - 1)
                    }
                    showMenuForApp = null
                },
                onMoveRight = {
                    if (index < popularApps.size - 1) {
                        onAppMove(index, index + 1)
                    }
                    showMenuForApp = null
                },
                onRemove = {
                    onAppRemove(packageName)
                    showMenuForApp = null
                },
                onDismiss = { showMenuForApp = null }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PopularAppIcon(
    app: AppInfo,
    index: Int,
    onAppClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    onMenuPress: () -> Unit = {}
) {
    var isFocused by remember { mutableStateOf(false) }
    
    val iconSize = 60.dp
    val contentIconSize = 52.dp
    val density = LocalDensity.current
    val iconBitmapSizePx = remember(contentIconSize, density) { 
        with(density) { contentIconSize.roundToPx() }
    }
    
    // Optimize icon bitmap creation - async loading on background thread using all CPU cores
    var iconBitmap by remember(app.packageName, iconBitmapSizePx) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    
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
                android.util.Log.e("PopularAppIcon", "Error loading icon for ${app.packageName}", e)
                iconBitmap = null
            }
        }
    }
    
    // Animation for focus - optimized for better performance
    val animatedScale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1f,
        animationSpec = tween(durationMillis = 150), // Faster animation
        label = "iconScale"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.8f,
        animationSpec = tween(durationMillis = 150), // Faster animation
        label = "iconAlpha"
    )

    Box(
        modifier = modifier
            .size(iconSize)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                alpha = animatedAlpha
            }
            .onFocusChanged { 
                isFocused = it.isFocused
            }
            .focusable()
            .onKeyEvent { keyEvent ->
                if (!isFocused) return@onKeyEvent false
                
                val keyCode = keyEvent.nativeKeyEvent?.keyCode
                when (keyEvent.type) {
                    KeyEventType.KeyDown -> {
                        when (keyCode) {
                            AndroidKeyEvent.KEYCODE_DPAD_CENTER,
                            AndroidKeyEvent.KEYCODE_ENTER -> {
                                // Only handle click, no long press
                                onAppClick()
                                true
                            }
                            AndroidKeyEvent.KEYCODE_MENU -> {
                                // Open menu on MENU button press
                                onMenuPress()
                                true
                            }
                            else -> false
                        }
                    }
                    KeyEventType.KeyUp -> {
                        when (keyCode) {
                            AndroidKeyEvent.KEYCODE_MENU -> {
                                // Open menu on MENU button release
                                onMenuPress()
                                true
                            }
                            else -> false
                        }
                    }
                    else -> false
                }
            }
            .clickable(onClick = onAppClick),
        contentAlignment = Alignment.Center
    ) {
        // Main background - brighter when focused
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
        
        // Icon - with rounded corners for square icons
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
    }
}

@Composable
fun AddAppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    // Animation for focus - same as PopularAppIcon
    val animatedScale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "addAppButtonScale"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.8f,
        animationSpec = tween(durationMillis = 150),
        label = "addAppButtonAlpha"
    )
    
    Box(
        modifier = modifier
            .size(60.dp) // Same size as PopularAppIcon
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                alpha = animatedAlpha
            }
            .onFocusChanged { 
                isFocused = it.isFocused
            }
            .focusable()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Main background - brighter when focused (same as PopularAppIcon)
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
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Добавить приложение",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AddAppDialog(
    availableApps: List<AppInfo>,
    onAppSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectAppText = stringResource(R.string.select_app)
    val overlayFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    // Same sizes as PopularAppIcon
    val iconSize = 60.dp
    val contentIconSize = 52.dp
    val density = LocalDensity.current
    val iconBitmapSizePx = remember(contentIconSize, density) { 
        with(density) { contentIconSize.roundToPx() }
    }
    val dialogApps = remember(availableApps) { availableApps.take(24) }
    val itemFocusRequesters = remember(dialogApps) { dialogApps.map { FocusRequester() } }

    LaunchedEffect(Unit) {
        overlayFocusRequester.requestFocus()
    }
    LaunchedEffect(dialogApps) {
        if (dialogApps.isNotEmpty()) {
            focusManager.clearFocus(force = true)
            delay(60)
            itemFocusRequesters.firstOrNull()?.requestFocus()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent?.keyCode == AndroidKeyEvent.KEYCODE_BACK) {
                    when (event.type) {
                        KeyEventType.KeyDown -> {
                            onDismiss()
                            return@onPreviewKeyEvent true
                        }
                        else -> return@onPreviewKeyEvent true
                    }
                }
                false
            }
            .focusable()
            .focusRequester(overlayFocusRequester)
            .focusGroup()
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 72.dp, vertical = 56.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White.copy(alpha = 0.18f))
                .padding(32.dp)
                .focusGroup()
                .clickable(enabled = false, onClick = {})
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                Text(
                    text = selectAppText,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White.copy(alpha = 0.95f)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier
                        .focusGroup()
                        .onPreviewKeyEvent { event ->
                            val keyCode = event.nativeKeyEvent?.keyCode
                            when (keyCode) {
                                AndroidKeyEvent.KEYCODE_DPAD_UP,
                                AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                                    // Block up/down navigation at LazyRow level
                                    return@onPreviewKeyEvent true
                                }
                                else -> false
                            }
                        }
                ) {
                    val lastIndex = dialogApps.lastIndex
                    itemsIndexed(dialogApps) { index, app ->
                        var isFocused by remember { mutableStateOf(false) }
                        var selectionHandled by remember { mutableStateOf(false) }
                        
                        // Animation for focus - same as PopularAppIcon
                        val animatedScale by animateFloatAsState(
                            targetValue = if (isFocused) 1.15f else 1f,
                            animationSpec = tween(durationMillis = 150),
                            label = "addAppDialogScale"
                        )
                        val animatedAlpha by animateFloatAsState(
                            targetValue = if (isFocused) 1f else 0.8f,
                            animationSpec = tween(durationMillis = 150),
                            label = "addAppDialogAlpha"
                        )
                        
                        // Optimize icon bitmap creation - async loading on background thread using all CPU cores
                        var iconBitmap by remember(app.packageName, iconBitmapSizePx) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
                        
                        LaunchedEffect(app.packageName, iconBitmapSizePx) {
                            iconBitmap = null // Reset while loading
                            if (app.icon != null) {
                                try {
                                    iconBitmap = withContext(Dispatchers.Default) {
                                        app.icon?.toBitmap(iconBitmapSizePx, iconBitmapSizePx)?.asImageBitmap()
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("AddAppDialog", "Error loading icon for ${app.packageName}", e)
                                    iconBitmap = null
                                }
                            }
                        }
                        
                        val itemFocusRequester = itemFocusRequesters.getOrNull(index)

                        Box(
                            modifier = Modifier
                                .size(iconSize)
                                .graphicsLayer {
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                    alpha = animatedAlpha
                                }
                                .onFocusChanged {
                                    isFocused = it.isFocused
                                    if (!it.isFocused) {
                                        selectionHandled = false
                                    }
                                }
                                .focusable()
                                .then(
                                    if (itemFocusRequester != null) {
                                        Modifier.focusRequester(itemFocusRequester)
                                    } else {
                                        Modifier
                                    }
                                )
                                .onPreviewKeyEvent { event ->
                                    if (!isFocused) return@onPreviewKeyEvent false
                                    
                                    val keyCode = event.nativeKeyEvent?.keyCode
                                    when {
                                        (keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
                                                keyCode == AndroidKeyEvent.KEYCODE_ENTER ||
                                                keyCode == AndroidKeyEvent.KEYCODE_BUTTON_A) -> {
                                            when (event.type) {
                                                KeyEventType.KeyUp -> {
                                                    if (!selectionHandled) {
                                                        selectionHandled = true
                                                        onAppSelected(app.packageName)
                                                        return@onPreviewKeyEvent true
                                                    }
                                                }
                                                else -> {
                                                    return@onPreviewKeyEvent true
                                                }
                                            }
                                            false
                                        }
                                        keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP ||
                                                keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                                            return@onPreviewKeyEvent true
                                        }
                                        keyCode == AndroidKeyEvent.KEYCODE_DPAD_LEFT && index == 0 -> {
                                            when (event.type) {
                                                KeyEventType.KeyDown -> {
                                                    onDismiss()
                                                    return@onPreviewKeyEvent true
                                                }
                                                else -> return@onPreviewKeyEvent true
                                            }
                                        }
                                        keyCode == AndroidKeyEvent.KEYCODE_DPAD_RIGHT && index == lastIndex -> {
                                            return@onPreviewKeyEvent true
                                        }
                                        else -> false
                                    }
                                }
                                .clickable(onClick = {
                                    if (!selectionHandled) {
                                        selectionHandled = true
                                        onAppSelected(app.packageName)
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
                            ) {
                                iconBitmap?.let { bitmap ->
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = app.name,
                                        modifier = Modifier
                                            .size(contentIconSize)
                                            .align(Alignment.Center)
                                            .clip(RoundedCornerShape(16.dp)), // Rounded corners like Google Play
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AppManagementMenu(
    app: AppInfo,
    appIndex: Int,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true, onBack = onDismiss)
    
    val overlayFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    
    // Always create focus requesters for all possible menu items
    // Order: moveLeft (if canMoveLeft), moveRight (if canMoveRight), remove (always)
    val moveLeftRequester = remember { FocusRequester() }
    val moveRightRequester = remember { FocusRequester() }
    val removeRequester = remember { FocusRequester() }
    
    // Flag to ignore the first OK KeyUp event (when user releases the button that opened the menu)
    var shouldIgnoreNextOkKeyUp by remember { mutableStateOf(true) }
    
    // Auto-focus on first menu item when menu opens - using same logic as AddAppDialog
    LaunchedEffect(Unit) {
        android.util.Log.d("AppManagementMenu", "Menu opened for app: ${app.name}, index: $appIndex, canMoveLeft: $canMoveLeft, canMoveRight: $canMoveRight")
        // When menu opens, we expect the user to release the OK button that opened it
        // So we ignore the first OK KeyUp event
        shouldIgnoreNextOkKeyUp = true
        focusManager.clearFocus(force = true)
        delay(60)
        overlayFocusRequester.requestFocus()
        delay(60)
        // Focus on first available menu item
        when {
            canMoveLeft -> moveLeftRequester.requestFocus()
            canMoveRight -> moveRightRequester.requestFocus()
            else -> removeRequester.requestFocus()
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .onKeyEvent { event ->
                val keyCode = event.nativeKeyEvent?.keyCode
                if (keyCode == AndroidKeyEvent.KEYCODE_BACK || keyCode == AndroidKeyEvent.KEYCODE_ESCAPE) {
                    when (event.type) {
                        KeyEventType.KeyDown -> return@onKeyEvent true
                        KeyEventType.KeyUp -> {
                            onDismiss()
                            return@onKeyEvent true
                        }
                        else -> return@onKeyEvent true
                    }
                }
                false
            }
            .focusable()
            .focusRequester(overlayFocusRequester)
            .focusGroup()
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 48.dp, vertical = 24.dp)
                .background(Color.White.copy(alpha = 0.15f))
                .padding(16.dp)
                .clickable(enabled = false, onClick = {})
                .focusGroup()
                .onPreviewKeyEvent { event ->
                    val keyCode = event.nativeKeyEvent?.keyCode
                    when (keyCode) {
                        AndroidKeyEvent.KEYCODE_DPAD_LEFT,
                        AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                            // Block left/right navigation to prevent leaving menu
                            return@onPreviewKeyEvent true
                        }
                        else -> false
                    }
                },
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = app.name,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            // Build menu items - remove is always available
            if (canMoveLeft) {
                MenuItem(
                    text = "Переместить влево",
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = onMoveLeft,
                    focusRequester = moveLeftRequester,
                    isFirstItem = true,
                    isLastItem = !canMoveRight,
                    shouldIgnoreNextOkKeyUp = shouldIgnoreNextOkKeyUp,
                    onOkKeyUp = { 
                        // After first OK KeyUp (release of button that opened menu), reset flag
                        shouldIgnoreNextOkKeyUp = false
                    }
                )
            }
            
            if (canMoveRight) {
                MenuItem(
                    text = "Переместить вправо",
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    onClick = onMoveRight,
                    focusRequester = moveRightRequester,
                    isFirstItem = !canMoveLeft,
                    isLastItem = false,
                    shouldIgnoreNextOkKeyUp = shouldIgnoreNextOkKeyUp,
                    onOkKeyUp = { 
                        // After first OK KeyUp (release of button that opened menu), reset flag
                        shouldIgnoreNextOkKeyUp = false
                    }
                )
            }
            
            // Remove option is ALWAYS available for ALL apps - MUST be visible
            // This is rendered unconditionally for all apps
            // Log to verify this is being rendered
            LaunchedEffect(app.name) {
                android.util.Log.d("AppManagementMenu", "Rendering remove option for: ${app.name}, index: $appIndex")
            }
            MenuItem(
                text = "Удалить из списка",
                icon = Icons.Default.Delete,
                onClick = {
                    android.util.Log.d("AppManagementMenu", "Remove clicked for: ${app.name}")
                    onRemove()
                },
                isDestructive = true,
                focusRequester = removeRequester,
                isFirstItem = !canMoveLeft && !canMoveRight,
                isLastItem = true,
                shouldIgnoreNextOkKeyUp = shouldIgnoreNextOkKeyUp,
                onOkKeyUp = { 
                    // After first OK KeyUp (release of button that opened menu), reset flag
                    shouldIgnoreNextOkKeyUp = false
                }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MenuItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
    focusRequester: FocusRequester? = null,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
    shouldIgnoreNextOkKeyUp: Boolean = false,
    onOkKeyUp: () -> Unit = {}
) {
    var isFocused by remember { mutableStateOf(false) }
    // Unified animations - same as PopularAppIcon
    val animatedScale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "menuItemScale"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.8f,
        animationSpec = tween(durationMillis = 150),
        label = "menuItemAlpha"
    )
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                alpha = animatedAlpha
            }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                }
            )
            .onKeyEvent { keyEvent ->
                if (!isFocused) return@onKeyEvent false
                
                when (keyEvent.type) {
                    KeyEventType.KeyDown -> {
                        when (keyEvent.nativeKeyEvent?.keyCode) {
                            AndroidKeyEvent.KEYCODE_DPAD_CENTER,
                            AndroidKeyEvent.KEYCODE_ENTER -> {
                                // Handle OK press on KeyDown - this will work immediately
                                // Reset the ignore flag if it was set
                                if (shouldIgnoreNextOkKeyUp) {
                                    onOkKeyUp()
                                }
                                onClick()
                                true
                            }
                            AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                                if (isFirstItem) {
                                    // Block up navigation on first item
                                    return@onKeyEvent true
                                }
                                false
                            }
                            AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                                if (isLastItem) {
                                    // Block down navigation on last item
                                    return@onKeyEvent true
                                }
                                false
                            }
                            AndroidKeyEvent.KEYCODE_DPAD_LEFT,
                            AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                                // Block left/right navigation to prevent leaving menu
                                return@onKeyEvent true
                            }
                            else -> false
                        }
                    }
                    KeyEventType.KeyUp -> {
                        when (keyEvent.nativeKeyEvent?.keyCode) {
                            AndroidKeyEvent.KEYCODE_DPAD_CENTER,
                            AndroidKeyEvent.KEYCODE_ENTER -> {
                                // Reset ignore flag on KeyUp if it was still set
                                if (shouldIgnoreNextOkKeyUp) {
                                    onOkKeyUp()
                                    return@onKeyEvent true
                                }
                                false
                            }
                            AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                                if (isFirstItem) {
                                    return@onKeyEvent true
                                }
                                false
                            }
                            AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                                if (isLastItem) {
                                    return@onKeyEvent true
                                }
                                false
                            }
                            AndroidKeyEvent.KEYCODE_DPAD_LEFT,
                            AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                                // Block left/right navigation to prevent leaving menu
                                return@onKeyEvent true
                            }
                            else -> false
                        }
                    }
                    else -> false
                }
            }
            .onPreviewKeyEvent { event ->
                if (!isFocused) return@onPreviewKeyEvent false
                val keyCode = event.nativeKeyEvent?.keyCode ?: return@onPreviewKeyEvent false
                when (keyCode) {
                    AndroidKeyEvent.KEYCODE_DPAD_LEFT,
                    AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                        // Block left/right navigation at preview level
                        return@onPreviewKeyEvent true
                    }
                    AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                        if (isFirstItem) {
                            return@onPreviewKeyEvent true
                        }
                        false
                    }
                    AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (isLastItem) {
                            return@onPreviewKeyEvent true
                        }
                        false
                    }
                    else -> false
                }
            }
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = if (isDestructive) Color.Red else Color.White.copy(alpha = animatedAlpha),
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDestructive) Color.Red else Color.White.copy(alpha = animatedAlpha)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NavigationMenu(
    items: List<String>,
    selectedItem: String,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Get localized strings
    val homeMenu = stringResource(R.string.menu_home)
    val appsMenu = stringResource(R.string.menu_apps)
    val settingsMenu = stringResource(R.string.menu_settings)
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            var isFocused by remember { mutableStateOf(false) }
            val isSelected = item == selectedItem
            val isHomeItem = item == homeMenu
            val isSettingsItem = item == settingsMenu
            val isAppsItem = item == appsMenu

            val backgroundColor = when {
                isSelected -> Color.White.copy(alpha = 0.1f) // Very light background for selected
                isFocused -> Color.White.copy(alpha = 0.08f)
                isSettingsItem -> Color.White.copy(alpha = 0.12f)
                else -> Color.White.copy(alpha = 0.05f) // Small background to prevent gradient showing through
            }

            val contentColor = when {
                isSelected -> Color.White.copy(alpha = 0.95f) // White text for selected
                isSettingsItem -> Color.White.copy(alpha = if (isFocused) 0.95f else 0.85f)
                else -> Color.White.copy(alpha = if (isFocused) 0.95f else 0.75f)
            }

            // Animation for focus - same as PopularAppIcon
            val animatedScale by animateFloatAsState(
                targetValue = if (isFocused) 1.15f else 1f,
                animationSpec = tween(durationMillis = 150),
                label = "navigationMenuScale"
            )
            val animatedAlpha by animateFloatAsState(
                targetValue = if (isFocused) 1f else 0.8f,
                animationSpec = tween(durationMillis = 150),
                label = "navigationMenuAlpha"
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
                    .clickable(onClick = { onItemClick(item) })
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Main background - same style as PopularAppIcon
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            color = if (isFocused || isSelected) {
                                Color.White.copy(alpha = 0.3f) // Brighter when focused
                            } else {
                                Color.White.copy(alpha = 0.1f)
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Light glow effect when focused or selected - soft transparent glow around shape (circular for icons)
                    if (isFocused || isSelected) {
                        val glowShape = if (isHomeItem || isSettingsItem || isAppsItem) {
                            CircleShape // Circular glow for icon items
                        } else {
                            RoundedCornerShape(24.dp) // Rounded for text items
                        }
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .padding(6.dp)
                                .background(
                                    color = Color.White.copy(alpha = if (isSelected) 0.3f else 0.25f),
                                    shape = glowShape
                                )
                                .blur(8.dp)
                        )
                    }
                    
                    // Content on top
                    if (isHomeItem) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = homeMenu,
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    } else if (isSettingsItem) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = settingsMenu,
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    } else if (isAppsItem) {
                        // Use Material Apps icon
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = appsMenu,
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

enum class NetworkStatus {
    WifiActive,
    WifiDisabled,
    Ethernet,
    Unknown
}

@Composable
fun SearchIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    // Animation for focus - same as PopularAppIcon
    val animatedScale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "searchIconScale"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.8f,
        animationSpec = tween(durationMillis = 150),
        label = "searchIconAlpha"
    )
    
    Box(
        modifier = modifier
            .size(60.dp) // Same size as PopularAppIcon
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                alpha = animatedAlpha
            }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { keyEvent ->
                val keyCode = keyEvent.nativeKeyEvent?.keyCode
                when (keyEvent.type) {
                    KeyEventType.KeyDown -> {
                        when (keyCode) {
                            AndroidKeyEvent.KEYCODE_DPAD_CENTER,
                            AndroidKeyEvent.KEYCODE_ENTER -> {
                                onClick()
                                true
                            }
                            else -> false
                        }
                    }
                    else -> false
                }
            }
            .clip(RoundedCornerShape(12.dp))
            .background(
                color = if (isFocused) {
                    Color.White.copy(alpha = 0.3f) // Brighter when focused
                } else {
                    Color.White.copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun NetworkStatusIcon(
    networkStatus: NetworkStatus,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val iconResId = remember(networkStatus) {
        when (networkStatus) {
            NetworkStatus.WifiActive -> context.resources.getIdentifier("ic_wifi_material", "drawable", context.packageName)
            NetworkStatus.WifiDisabled -> context.resources.getIdentifier("ic_wifi_off_material", "drawable", context.packageName)
            NetworkStatus.Ethernet -> context.resources.getIdentifier("ic_ethernet_material", "drawable", context.packageName)
            NetworkStatus.Unknown -> context.resources.getIdentifier("ic_wifi_material", "drawable", context.packageName)
        }
    }
    
    val description = when (networkStatus) {
        NetworkStatus.WifiActive -> stringResource(R.string.wifi_active)
        NetworkStatus.WifiDisabled -> stringResource(R.string.wifi_disabled)
        NetworkStatus.Ethernet -> stringResource(R.string.ethernet_connected)
        NetworkStatus.Unknown -> stringResource(R.string.network_status_unknown)
    }
    
    if (iconResId != 0) {
        Image(
            painter = painterResource(id = iconResId),
            contentDescription = description,
            modifier = modifier.size(18.dp),
            contentScale = ContentScale.Fit
        )
    } else {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = description,
            tint = Color.White,
            modifier = modifier.size(18.dp)
        )
    }
}

private fun getNetworkStatus(
    context: Context,
    connectivityManager: ConnectivityManager?,
    wifiManager: WifiManager?
): NetworkStatus {
    try {
        val isWifiEnabled = wifiManager?.isWifiEnabled ?: false
        
        connectivityManager?.let { cm ->
            val network = cm.activeNetwork
            if (network != null) {
                val capabilities = cm.getNetworkCapabilities(network)
                capabilities?.let { caps ->
                    // Check for Ethernet first
                    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                        return NetworkStatus.Ethernet
                    }
                    
                    // Check for WiFi
                    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        return if (isWifiEnabled) {
                            NetworkStatus.WifiActive
                        } else {
                            NetworkStatus.WifiDisabled
                        }
                    }
                }
            }
        }
        
        // If no active network, check WiFi state
        val result = if (isWifiEnabled) {
            NetworkStatus.WifiActive // WiFi enabled (may or may not be connected, but enabled)
        } else {
            NetworkStatus.WifiDisabled // WiFi disabled
        }
        android.util.Log.d("TopNavigationBar", "Network status determined: $result (wifiEnabled: $isWifiEnabled)")
        return result
    } catch (e: Exception) {
        android.util.Log.e("TopNavigationBar", "Error getting network status", e)
        return NetworkStatus.Unknown
    }
}

