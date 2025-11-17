package com.octopus.launcher.ui.components

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.tv.TvInputManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent as AndroidKeyEvent
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import android.media.tv.TvInputInfo
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.octopus.launcher.utils.TVKeyInjector
import java.util.Locale

private const val TAG = "QuickActionsWidget"

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun QuickActionsWidget(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val inputs = remember { mutableStateListOf<TVKeyInjector.InputDescriptor>() }
    val tvInputManager = remember(context) {
        context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager
    }
    
    // Cache for inputs - use SharedPreferences to persist
    val inputsCache = remember(context) {
        context.getSharedPreferences("octopus_launcher_inputs_cache", Context.MODE_PRIVATE)
    }
    
    // Track inputs size to trigger recomposition when list changes
    val inputsSize = remember { mutableStateOf(0) }
    
    // Filter out IPTV inputs and app inputs - use derivedStateOf to track changes in mutableStateListOf
    // Read inputs.size to ensure derivedStateOf tracks changes properly
    val filteredInputs = remember {
        derivedStateOf {
            // Read size first to ensure we track changes
            val size = inputs.size
            val filtered = inputs.filter { input ->
                !(input.type == TvInputInfo.TYPE_TUNER && input.label.contains("IPTV", ignoreCase = true))
            }
            Log.d(TAG, "Filtered inputs: ${filtered.size} from $size total")
            filtered
        }
    }
    
    // Track changes in inputs list using snapshotFlow to trigger recomposition
    // This ensures derivedStateOf recomputes when inputs change
    LaunchedEffect(Unit) {
        snapshotFlow { inputs.size to inputs.toList() }.collect { (size, _) ->
            inputsSize.value = size
        }
    }
    
    // Only recreate focus requesters when filtered inputs count changes
    // Use inputsSize to trigger recomposition
    val inputFocusRequesters = remember(filteredInputs.value.size, inputsSize.value) {
        Log.d(TAG, "Creating ${filteredInputs.value.size} focus requesters")
        List(filteredInputs.value.size) { FocusRequester() }
    }

    val updateInputs = remember(context) {
        {
            Log.d(TAG, "=== Updating inputs list ===")
            val fresh = TVKeyInjector.getAvailableInputs(context)
            Log.d(TAG, "Got ${fresh.size} inputs from TVKeyInjector")

            val sorted = fresh.sortedBy { it.label.lowercase(Locale.getDefault()) }
            sorted.forEachIndexed { index, input ->
                Log.d(TAG, "  [$index] ${input.label} (type: ${input.type}, passthrough: ${input.isPassthrough})")
            }

            // Cache input IDs for quick comparison on next launch
            val inputIds = sorted.map { it.id }.joinToString(",")
            inputsCache.edit()
                .putString("cached_input_ids", inputIds)
                .putLong("cached_inputs_timestamp", System.currentTimeMillis())
                .apply()
            Log.d(TAG, "Cached ${sorted.size} input IDs")

            inputs.clear()
            inputs.addAll(sorted)
            Log.d(TAG, "Updated inputs list, total: ${inputs.size}")
            Log.d(TAG, "=== End updating inputs ===")
        }
    }
    
    // Scan inputs only once on launch
    var hasScannedOnce by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        if (!hasScannedOnce) {
            Log.d(TAG, "LaunchedEffect: Scanning inputs once on launch")
            updateInputs()
            hasScannedOnce = true
        }
    }

    val handler = remember { Handler(Looper.getMainLooper()) }

    DisposableEffect(tvInputManager, handler) {
        val manager = tvInputManager
        if (manager == null) {
            Log.w(TAG, "TvInputManager is null, skipping callback registration")
            onDispose { }
        } else {
            Log.d(TAG, "Registering TvInputManager callbacks")
            val callback = object : TvInputManager.TvInputCallback() {
                override fun onInputAdded(inputId: String) {
                    Log.d(TAG, "onInputAdded: $inputId - rescanning inputs")
                    handler.post { updateInputs() }
                }

                override fun onInputRemoved(inputId: String) {
                    Log.d(TAG, "onInputRemoved: $inputId - rescanning inputs")
                    handler.post { updateInputs() }
                }

                override fun onInputStateChanged(inputId: String, state: Int) {
                    // Don't rescan on state changes - only when inputs are added/removed
                    Log.v(TAG, "onInputStateChanged: $inputId, state: $state (ignored)")
                }
            }
            manager.registerCallback(callback, handler)
            onDispose {
                Log.d(TAG, "Unregistering TvInputManager callbacks")
                manager.unregisterCallback(callback)
                handler.removeCallbacksAndMessages(null)
            }
        }
    }

    Log.d(TAG, "Rendering widget with ${filteredInputs.value.size} filtered inputs (total: ${inputs.size})")
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Display input sources directly in the widget
        repeat(filteredInputs.value.size) { index ->
            val input = filteredInputs.value[index]
            Log.d(TAG, "Rendering InputSourceIcon at index=$index: ${input.label}")
            InputSourceIcon(
                input = input,
                focusRequester = inputFocusRequesters.getOrNull(index),
                onSelect = {
                    Log.d(TAG, "Input selected: ${input.label} (id: ${input.id})")
                    val success = TVKeyInjector.launchInputById(context, input.id)
                    if (!success) {
                        Log.e(TAG, "Failed to launch input: ${input.label}")
                        Toast.makeText(context, "Не удалось переключиться на ${input.label}", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.d(TAG, "Successfully launched input: ${input.label}")
                    }
                },
                onDismiss = {
                    // No-op for widget buttons
                }
            )
        }

        QuickActionButton(
            iconType = IconType.Wifi,
            contentDescription = "Переключить Wi-Fi",
            onClick = {
                Log.d(TAG, "WiFi button clicked")
                toggleWifi(context)
            }
        )

        QuickActionButton(
            iconType = IconType.Settings,
            contentDescription = "Настройки ТВ",
            onClick = {
                Log.d(TAG, "Settings button clicked")
                openTVSettings(context)
            }
        )
    }
}

enum class IconType {
    Wifi, Settings
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun QuickActionButton(
    iconType: IconType,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val iconVector = remember(iconType) {
        when (iconType) {
            IconType.Wifi -> Icons.Filled.Wifi
            IconType.Settings -> Icons.Filled.Settings
        }
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "quickActionScale"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.8f,
        animationSpec = tween(durationMillis = 150),
        label = "quickActionAlpha"
    )

    val contentColor = Color.White.copy(alpha = if (isFocused) 0.95f else 0.75f)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                alpha = animatedAlpha
            }
            .onFocusChanged {
                val wasFocused = isFocused
                isFocused = it.isFocused
                if (isFocused != wasFocused) {
                    Log.v(TAG, "QuickActionButton ($iconType) focus changed: $isFocused")
                }
            }
            .focusable()
            .onKeyEvent { keyEvent ->
                val keyCode = keyEvent.nativeKeyEvent?.keyCode
                when (keyEvent.type) {
                    KeyEventType.KeyDown -> {
                        when (keyCode) {
                            AndroidKeyEvent.KEYCODE_DPAD_CENTER,
                            AndroidKeyEvent.KEYCODE_ENTER -> {
                                Log.d(TAG, "QuickActionButton ($iconType) activated via key")
                                onClick()
                                true
                            }
                            else -> false
                        }
                    }
                    else -> false
                }
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
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
            Icon(
                imageVector = iconVector,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

private fun toggleWifi(context: Context) {
    try {
        Log.d(TAG, "toggleWifi: Starting WiFi toggle")
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        wifiManager?.let {
            val isWifiEnabled = it.isWifiEnabled
            Log.d(TAG, "toggleWifi: Current state: $isWifiEnabled")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Log.d(TAG, "toggleWifi: Using Settings panel (Android 10+)")
                val intent = Intent(android.provider.Settings.Panel.ACTION_WIFI)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                Log.d(TAG, "toggleWifi: Toggling directly (Android < 10)")
                it.isWifiEnabled = !isWifiEnabled
                Log.d(TAG, "toggleWifi: New state: ${it.isWifiEnabled}")
            }
        } ?: Log.e(TAG, "toggleWifi: WifiManager is null")
    } catch (e: Exception) {
        Log.e(TAG, "toggleWifi: Failed to toggle WiFi", e)
    }
}

private fun openTVSettings(context: Context) {
    Log.d(TAG, "openTVSettings: Opening TV settings")
    TVKeyInjector.openTVSettings(context)
}

