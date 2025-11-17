package com.octopus.launcher.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import java.util.Locale

object TVKeyInjector {
    private const val TAG = "TVKeyInjector"

    /**
     * Sends key event via Activity dispatchKeyEvent (safe method, no root required)
     */
    private fun dispatchKeyEvent(context: Context, keyCode: Int): Boolean {
        return try {
            val activity = context as? Activity
            activity?.let {
                val keyEventDown = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
                val keyEventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)
                val handled = it.dispatchKeyEvent(keyEventDown) || it.dispatchKeyEvent(keyEventUp)
                if (handled) {
                    Log.d(TAG, "Key event $keyCode dispatched successfully")
                }
                handled
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch key event $keyCode", e)
            false
        }
    }

    /**
     * Try to start the provided intent safely, logging the reason for debugging.
     */
    private fun tryStartActivity(context: Context, intent: Intent, reason: String): Boolean {
        return try {
            val launchIntent = intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            Log.d(TAG, "Started $reason via ${intent.component ?: intent.action}")
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unable to start $reason via ${intent.component ?: intent.action}", e)
            false
        }
    }

    /**
     * Attempts to open the system input switcher or a vendor alternative.
     */
    fun sendTVInput(context: Context): Boolean {
        val pm: PackageManager = context.packageManager
        val manufacturer = Build.MANUFACTURER?.lowercase(Locale.US).orEmpty()
        val brand = Build.BRAND?.lowercase(Locale.US).orEmpty()

        val candidateIntents = buildList {
            add(Intent("com.android.tv.action.REQUEST_SWITCH_INPUT"))
            add(Intent("android.settings.TV_INPUT_SETTINGS"))
            add(Intent("android.media.tv.TvInputManager.ACTION_SETUP_INPUTS"))

            // Google TV quick panel
            add(Intent("com.google.android.tvlauncher.action.OPEN_INPUTS"))
            add(Intent("com.google.android.apps.tvlauncher.action.OPEN_INPUTS"))

            // TCL specific panels
            if ("tcl" in manufacturer || "tcl" in brand) {
                add(Intent().setClassName("com.tcl.tv", "com.tcl.tv.setting.ui.inputsource.InputSourceListActivity"))
                add(Intent().setClassName("com.tcl.tv", "com.tcl.tv.settings.ui.inputsource.InputSourceListActivity"))
                add(Intent().setClassName("com.tcl.tv", "com.tcl.tv.settings.InputSourceListActivity"))
                add(Intent().setClassName("com.tcl.settings", "com.tcl.settings.tv.InputSourceActivity"))
                add(Intent("com.tcl.tv.action.SWITCH_INPUT"))
                add(Intent("com.tcl.tv.action.INPUT_SOURCE"))
            }

            // Hisense / Vizio variations
            if ("hisense" in manufacturer) {
                add(Intent().setClassName("com.hisense.hismart", "com.hisense.hismart.settings.InputSourceActivity"))
                add(Intent("com.hisense.tv.action.SWITCH_INPUT"))
            }
            if ("vizio" in manufacturer || "vizio" in brand) {
                add(Intent("com.vizio.android.tv.action.SHOW_INPUTS"))
            }
        }.map { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

        candidateIntents.forEach { intent ->
            if (intent.resolveActivity(pm) != null || intent.component != null) {
                if (tryStartActivity(context, intent, "TV input panel")) {
                    return true
                }
            }
        }

        // Fallback: launch the first available TV input directly (HDMI / tuner etc.)
        if (launchFirstAvailableInput(context)) {
            return true
        }

        // Final fallback: attempt to emulate the hardware key
        return dispatchKeyEvent(context, KeyEvent.KEYCODE_TV_INPUT)
    }

    private fun launchFirstAvailableInput(context: Context): Boolean {
        val tim = context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager ?: return false
        val inputs = runCatching { tim.tvInputList ?: emptyList() }.getOrElse {
            Log.e(TAG, "Unable to fetch TV inputs", it)
            emptyList()
        }
        if (inputs.isEmpty()) return false

        inputs.sortedWith(compareBy<TvInputInfo> { it.type }
            .thenBy { it.loadLabel(context)?.toString().orEmpty() })
            .forEach { info ->
                if (!isInputConnected(tim, info)) return@forEach
                if (launchInput(context, info)) {
                    return true
                }
            }
        return false
    }

    private fun isInputConnected(tim: TvInputManager, info: TvInputInfo): Boolean {
        return runCatching { tim.getInputState(info.id) != TvInputManager.INPUT_STATE_DISCONNECTED }
            .getOrDefault(true)
    }

    private fun launchInput(context: Context, info: TvInputInfo): Boolean {
        val pm = context.packageManager
        val primaryIntent = buildViewIntent(info)
        if (primaryIntent != null &&
            (primaryIntent.resolveActivity(pm) != null || info.isPassthroughInput)
        ) {
            if (tryStartActivity(context, primaryIntent, "input ${info.id}")) {
                return true
            }
        }

        // Secondary option: OEM specific switch input broadcast/intent
        val switchIntent = Intent("com.android.tv.action.SWITCH_INPUT")
            .putExtra("inputId", info.id)
        if (switchIntent.resolveActivity(pm) != null &&
            tryStartActivity(context, switchIntent, "SWITCH_INPUT for ${info.id}")
        ) {
            return true
        }

        // As a last resort, try the setup intent (some OEMs reuse it for switching)
        val setupIntent = info.createSetupIntent()
        if (setupIntent != null &&
            tryStartActivity(context, setupIntent, "setup ${info.id}")
        ) {
            return true
        }

        return false
    }

    private fun buildViewIntent(info: TvInputInfo): Intent? {
        return try {
            val uri = if (info.isPassthroughInput) {
                TvContract.buildChannelUriForPassthroughInput(info.id)
            } else {
                TvContract.buildChannelsUriForInput(info.id)
            }
            Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to build view intent for ${info.id}", e)
            null
        }
    }

    /**
     * Sends TV Power Off key event
     */
    fun sendTVPowerOff(context: Context): Boolean {
        if (dispatchKeyEvent(context, KeyEvent.KEYCODE_TV_POWER)) {
            return true
        }
        return dispatchKeyEvent(context, KeyEvent.KEYCODE_POWER)
    }

    /**
     * Opens TV notifications or falls back to notification settings.
     */
    fun openNotifications(context: Context): Boolean {
        val manufacturer = Build.MANUFACTURER?.lowercase(Locale.US).orEmpty()
        val brand = Build.BRAND?.lowercase(Locale.US).orEmpty()
        val pm = context.packageManager

        // Try hardware key emulation first
        if (dispatchKeyEvent(context, KeyEvent.KEYCODE_NOTIFICATION)) return true

        val candidateIntents = buildList {
            // SystemUI notification panel (works without stock launcher)
            add(Intent(Intent.ACTION_MAIN).setClassName("com.android.systemui", "com.android.systemui.tv.notification.TvNotificationActivity"))
            
            // Standard Android TV notification intents
            add(Intent("com.android.tv.action.OPEN_NOTIFICATIONS"))
            add(Intent("com.android.tv.action.SHOW_NOTIFICATIONS"))
            add(Intent("com.google.android.systemui.notifications.PANEL"))
            
            // TCL-specific notification panels
            if ("tcl" in manufacturer || "tcl" in brand) {
                add(Intent().setClassName("com.tcl.settings", "com.tcl.settings.notification.NotificationActivity"))
                add(Intent("com.tcl.tv.action.OPEN_NOTIFICATION_CENTER"))
            }
            
            // Google TV launcher notification panel (only if launcher is enabled)
            // Note: This will be skipped if launcher is disabled
            val googleTvLauncherIntent = Intent("android.app.action.TOGGLE_NOTIFICATION_HANDLER_PANEL")
                .setClassName("com.google.android.apps.tv.launcherx", "com.google.android.apps.tv.launcherx.dashboard.DashboardHandler")
            if (googleTvLauncherIntent.resolveActivity(pm) != null) {
                add(googleTvLauncherIntent)
            }
        }.map { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

        candidateIntents.forEach { intent ->
            val descriptor = intent.component ?: intent.action
            if (intent.resolveActivity(pm) != null || intent.component != null) {
                Log.d(TAG, "Attempting notifications panel via $descriptor")
                if (tryStartActivity(context, intent, "notifications panel")) {
                    Log.d(TAG, "Notifications panel opened via $descriptor")
                    return true
                } else {
                    Log.w(TAG, "Failed to start notifications panel via $descriptor")
                }
            } else {
                Log.v(TAG, "Skipping notifications intent (no handler): $descriptor")
            }
        }

        if (!PermissionUtils.isNotificationListenerEnabled(context)) {
            Log.w(TAG, "Notification listener not enabled; redirecting to settings")
            val listenerSettings = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            if (listenerSettings.resolveActivity(pm) != null &&
                tryStartActivity(context, listenerSettings, "notification listener settings")
            ) {
                Log.d(TAG, "Opened notification listener settings")
                return true
            }

            val appSettings = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
            }
            if (appSettings.resolveActivity(pm) != null &&
                tryStartActivity(context, appSettings, "app notification settings")
            ) {
                Log.d(TAG, "Opened app notification settings for notifications")
                return true
            }
        }

        Log.w(TAG, "Unable to open notifications panel or related settings")
        return false
    }

    /**
     * Opens TV Settings
     */
    fun openTVSettings(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_SETTINGS)
        if (tryStartActivity(context, intent, "TV settings")) {
            return true
        }
        return dispatchKeyEvent(context, KeyEvent.KEYCODE_SETTINGS)
    }

    fun openTvTunerSettings(context: Context): Boolean {
        val intent = Intent("android.settings.TV_INPUT_SETTINGS")
        return tryStartActivity(context, intent, "TV tuner settings")
    }

    fun isTvTunerAvailable(context: Context): Boolean {
        val pm = context.packageManager
        val intent = Intent("android.settings.TV_INPUT_SETTINGS")
        return intent.resolveActivity(pm) != null
    }

    fun isRequestSwitchAvailable(context: Context): Boolean {
        val pm = context.packageManager
        val intent = Intent("com.android.tv.action.REQUEST_SWITCH_INPUT")
        return intent.resolveActivity(pm) != null
    }

    fun hasHdmiInput(context: Context, index: Int): Boolean {
        val tim = context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager ?: return false
        val inputs = runCatching {
            tim.tvInputList?.filter { it.type == TvInputInfo.TYPE_HDMI && isInputConnected(tim, it) }
        }.getOrElse {
            Log.e(TAG, "Unable to inspect HDMI inputs", it)
            emptyList()
        } ?: emptyList()
        return index in inputs.indices
    }

    fun launchHdmiInput(context: Context, index: Int): Boolean {
        val tim = context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager ?: return sendTVInput(context)
        val inputs = runCatching {
            tim.tvInputList?.filter { it.type == TvInputInfo.TYPE_HDMI }
        }.getOrElse {
            Log.e(TAG, "Unable to enumerate HDMI inputs", it)
            emptyList()
        } ?: emptyList()
        if (index !in inputs.indices) {
            return sendTVInput(context)
        }
        val info = inputs[index]
        if (launchInput(context, info)) {
            Log.d(TAG, "Launched HDMI input index=$index (${info.id})")
            return true
        }
        return sendTVInput(context)
    }

    data class InputDescriptor(
        val id: String,
        val label: String,
        val type: Int,
        val isPassthrough: Boolean,
        val icon: Drawable?
    )

    fun getAvailableInputs(context: Context): List<InputDescriptor> {
        val tim = context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager ?: return emptyList()
        val inputs = runCatching { tim.tvInputList ?: emptyList() }.getOrElse {
            Log.e(TAG, "Unable to fetch TV input list", it)
            emptyList()
        }
        
        Log.d(TAG, "=== TV INPUTS DEBUG ===")
        Log.d(TAG, "Total TV inputs found: ${inputs.size}")
        
        inputs.forEachIndexed { index, input ->
            val label = input.loadLabel(context)?.toString() ?: "Unknown"
            Log.d(TAG, "Input #$index: '$label'")
            Log.d(TAG, "  - ID: ${input.id}")
            Log.d(TAG, "  - Type: ${input.type} (${getTypeName(input.type)})")
            Log.d(TAG, "  - Passthrough: ${input.isPassthroughInput}")
            Log.d(TAG, "  - CanRecord: ${input.canRecord()}")
            
            // Проверим состояние подключения
            val state = runCatching { tim.getInputState(input.id) }.getOrNull()
            Log.d(TAG, "  - State: $state (${getStateName(state)})")
            
            // Проверим сервис
            val service = input.serviceInfo
            Log.d(TAG, "  - Service: ${service?.packageName}/${service?.name}")
        }

        if (inputs.isEmpty()) return emptyList()

        // Фильтрация по логике LeanbackLauncher:
        // 1. Passthrough входы показываем (но исключаем приложения из черного списка)
        // 2. Физические входы (HDMI, Component, Composite и т.д.) показываем
        // 3. Тюнеры показываем
        val filteredInputs = inputs.mapNotNull { input ->
            val label = deriveLabel(context, input)
            val icon = runCatching { input.loadIcon(context) }.getOrNull()
            
            // Исключаем приложения из черного списка
            if (isAppInput(input)) {
                Log.d(TAG, "Filtering out app input: $label (package: ${input.serviceInfo?.packageName})")
                return@mapNotNull null
            }
            
            // Показываем passthrough входы (физические входы через приложения)
            if (input.isPassthroughInput) {
                val descriptor = InputDescriptor(
                    id = input.id,
                    label = label,
                    type = input.type,
                    isPassthrough = input.isPassthroughInput,
                    icon = icon
                )
                Log.d(TAG, "Including passthrough input: $label (type: ${input.type})")
                return@mapNotNull descriptor
            }
            
            // Показываем физические входы определенных типов
            when (input.type) {
                TvInputInfo.TYPE_HDMI,
                TvInputInfo.TYPE_TUNER,
                TvInputInfo.TYPE_COMPONENT,
                TvInputInfo.TYPE_COMPOSITE,
                TvInputInfo.TYPE_DISPLAY_PORT,
                TvInputInfo.TYPE_DVI,
                TvInputInfo.TYPE_SVIDEO,
                TvInputInfo.TYPE_SCART,
                TvInputInfo.TYPE_OTHER -> {
                    val descriptor = InputDescriptor(
                        id = input.id,
                        label = label,
                        type = input.type,
                        isPassthrough = input.isPassthroughInput,
                        icon = icon
                    )
                    Log.d(TAG, "Including input: $label (type: ${input.type})")
                    descriptor
                }
                else -> {
                    Log.d(TAG, "Filtering out unknown type: $label (type: ${input.type})")
                    null
                }
            }
        }

        Log.d(TAG, "Filtered inputs: ${filteredInputs.size}")
        Log.d(TAG, "=== END DEBUG ===")
        
        return filteredInputs
    }
    
    // Вспомогательные функции для отладки
    private fun getTypeName(type: Int): String {
        return when (type) {
            TvInputInfo.TYPE_HDMI -> "HDMI"
            TvInputInfo.TYPE_TUNER -> "TUNER"
            TvInputInfo.TYPE_COMPONENT -> "COMPONENT"
            TvInputInfo.TYPE_COMPOSITE -> "COMPOSITE"
            TvInputInfo.TYPE_DISPLAY_PORT -> "DISPLAY_PORT"
            TvInputInfo.TYPE_DVI -> "DVI"
            TvInputInfo.TYPE_SVIDEO -> "SVIDEO"
            TvInputInfo.TYPE_SCART -> "SCART"
            TvInputInfo.TYPE_OTHER -> "OTHER"
            else -> "UNKNOWN($type)"
        }
    }
    
    private fun getStateName(state: Int?): String {
        return when (state) {
            TvInputManager.INPUT_STATE_CONNECTED -> "CONNECTED"
            TvInputManager.INPUT_STATE_CONNECTED_STANDBY -> "CONNECTED_STANDBY"
            TvInputManager.INPUT_STATE_DISCONNECTED -> "DISCONNECTED"
            null -> "UNKNOWN"
            else -> "UNKNOWN($state)"
        }
    }
    
    private fun isAppInput(input: TvInputInfo): Boolean {
        // Проверяем, является ли вход приложением (например, VK Video, YouTube и т.д.)
        // Используем логику из LeanbackLauncher - черный список пакетов
        val packageName = input.serviceInfo?.packageName ?: return false
        
        // Черный список пакетов из LeanbackLauncher (приложения, которые не являются физическими входами)
        val blackList = listOf(
            "com.google.android.videos",
            "com.google.android.youtube.tv",
            "com.amazon.avod",
            "com.amazon.hedwig",
            "com.vk.video",
            "com.vk.tv", // VK Video TV версия
            "com.netflix",
            "com.octopus.launcher" // наше приложение
        )
        
        // Проверяем точное совпадение с черным списком
        if (blackList.contains(packageName)) {
            return true
        }
        
        // Дополнительная проверка: исключаем VK приложения по части названия
        if (packageName.startsWith("com.vk.") && 
            (packageName.contains("video", ignoreCase = true) || 
             packageName.contains("tv", ignoreCase = true))) {
            return true
        }
        
        // Дополнительная проверка: если passthrough вход не имеет setupIntent, 
        // это скорее всего приложение, а не физический вход
        if (input.isPassthroughInput) {
            val setupIntent = input.createSetupIntent()
            if (setupIntent == null) {
                // Нет setupIntent - вероятно приложение
                return true
            }
        }
        
        return false
    }
    
    // Добавьте в TVKeyInjector.kt
    fun debugAllInputs(context: Context) {
        val tim = context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager ?: return
        val inputs = runCatching { tim.tvInputList ?: emptyList() }.getOrElse {
            Log.e(TAG, "Unable to fetch TV input list", it)
            emptyList()
        }
        
        Log.d(TAG, "=== ALL INPUTS FOR DEBUG ===")
        inputs.forEachIndexed { index, input ->
            val label = input.loadLabel(context)?.toString() ?: "Unknown"
            Log.d(TAG, "DEBUG Input #$index: '$label'")
            Log.d(TAG, "DEBUG   Type: ${input.type} (${getTypeName(input.type)})")
            Log.d(TAG, "DEBUG   ID: ${input.id}")
            Log.d(TAG, "DEBUG   Package: ${input.serviceInfo?.packageName}")
        }
        Log.d(TAG, "=== END ALL INPUTS ===")
    }

    fun launchInputById(context: Context, inputId: String): Boolean {
        val tim = context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager ?: return false
        val info = runCatching { tim.tvInputList?.firstOrNull { it.id == inputId } }.getOrNull()
        return info?.let { launchInput(context, it) } ?: false
    }

    private fun deriveLabel(context: Context, info: TvInputInfo): String {
        val label = runCatching { info.loadLabel(context)?.toString() }.getOrNull()
        if (!label.isNullOrBlank()) return label.trim()
        return when (info.type) {
            TvInputInfo.TYPE_HDMI -> "HDMI"
            TvInputInfo.TYPE_TUNER -> "Тюнер"
            TvInputInfo.TYPE_COMPONENT -> "Component"
            TvInputInfo.TYPE_COMPOSITE -> "Composite"
            TvInputInfo.TYPE_DISPLAY_PORT -> "DisplayPort"
            TvInputInfo.TYPE_DVI -> "DVI"
            TvInputInfo.TYPE_SVIDEO -> "S-Video"
            else -> "Input"
        }
    }
}

