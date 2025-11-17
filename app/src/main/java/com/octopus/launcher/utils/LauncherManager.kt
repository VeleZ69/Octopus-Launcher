package com.octopus.launcher.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log

class LauncherManager(private val context: Context) {

    companion object {
        private const val TAG = "LauncherManager"
        private const val MY_LAUNCHER_PACKAGE = "com.octopus.launcher"
        private const val MY_LAUNCHER_ACTIVITY = "com.octopus.launcher.MainActivity"
        private const val GOOGLE_TV_LAUNCHER = "com.google.android.tvlauncher"
        private const val GOOGLE_TV_LAUNCHER_X = "com.google.android.apps.tv.launcherx"

        // Popular stock launchers on Google TV / Android TV
        private val STOCK_LAUNCHERS = listOf(
            GOOGLE_TV_LAUNCHER,                         // Google TV standard
            GOOGLE_TV_LAUNCHER_X,                       // Google TV Launcher X
            "com.google.android.tungsten.setupwraith",  // Setup Wraith
            "com.google.android.leanbacklauncher",      // Android TV Leanback
            "com.android.tvlauncher",                   // Base Android TV Launcher
            "com.google.android.tv",                    // Google TV
            "com.google.android.apps.tv.launcher",      // Google Apps TV Launcher
            "com.google.android.tvhome",                // Google TV Home
            "com.google.android.launcher",              // Google Launcher (generic)
            "com.google.android.apps.nexuslauncher"     // Nexus Launcher
        )

        // Additional system components that may interfere
        private val SYSTEM_COMPONENTS = listOf(
            "com.google.android.tungsten.setupwraith",
            "com.google.android.apps.tv.initialsetup",
            "com.google.android.tv.frameworkpackages",
            "com.google.android.providers.tv"
        )
    }

    // Get installed stock launchers
    fun getInstalledStockLaunchers(): List<String> {
        return STOCK_LAUNCHERS.filter { isPackageInstalled(it) }
    }

    // Get current default launcher package
    fun getCurrentLauncher(): String? {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }

    // Are we the default launcher?
    fun isDefaultLauncher(): Boolean = getCurrentLauncher() == MY_LAUNCHER_PACKAGE

    // Set our launcher as default
    fun setAsDefaultLauncher() {
        if (isDefaultLauncher()) {
            Log.d(TAG, "Already default launcher")
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            showSetLauncherDialog()
        } else {
            setDefaultLauncherLegacy()
        }
    }

    private fun showSetLauncherDialog() {
        try {
            val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show home settings", e)
            setDefaultLauncherLegacy()
        }
    }

    private fun setDefaultLauncherLegacy() {
        try {
            val selectorIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(selectorIntent, "Choose launcher"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set default launcher (legacy)", e)
        }
    }

    // Disable installed stock launchers and interfering components (requires ADB/root)
    fun disableStockLaunchers(): DisableResult {
        return try {
            val installedLaunchers = getInstalledStockLaunchers()
            Log.d(TAG, "Found installed stock launchers: $installedLaunchers")

            val results = mutableListOf<CommandResult>()

            installedLaunchers.forEach { packageName ->
                val ok = executeShellCommand("pm disable-user --user 0 $packageName")
                results.add(CommandResult(packageName, ok))
                Log.d(TAG, "Disable $packageName: $ok")
            }

            SYSTEM_COMPONENTS.forEach { component ->
                if (isPackageInstalled(component)) {
                    val ok = executeShellCommand("pm disable-user --user 0 $component")
                    results.add(CommandResult(component, ok))
                    Log.d(TAG, "Disable component $component: $ok")
                }
            }

            // Force-stop processes
            installedLaunchers.forEach { packageName ->
                executeShellCommand("am force-stop $packageName")
            }

            // Start our launcher
            startOurLauncher()

            DisableResult(
                success = true,
                results = results,
                message = "Отключено ${results.count { it.success }} из ${results.size} компонентов"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling stock launchers", e)
            DisableResult(false, emptyList(), "Ошибка: ${e.message}")
        }
    }

    // Re-enable stock launchers/components
    fun enableStockLaunchers(): EnableResult {
        return try {
            val allPackages = STOCK_LAUNCHERS + SYSTEM_COMPONENTS
            val results = mutableListOf<CommandResult>()

            allPackages.forEach { packageName ->
                if (isPackageInstalled(packageName)) {
                    val ok = executeShellCommand("pm enable $packageName")
                    results.add(CommandResult(packageName, ok))
                    Log.d(TAG, "Enable $packageName: $ok")
                }
            }

            EnableResult(
                success = true,
                results = results,
                message = "Включено ${results.count { it.success }} компонентов"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling stock launchers", e)
            EnableResult(false, emptyList(), "Ошибка: ${e.message}")
        }
    }

    // Status for stock launchers
    fun getStockLaunchersStatus(): List<StockLauncherStatus> {
        val current = getCurrentLauncher()
        return STOCK_LAUNCHERS.map { pkg ->
            StockLauncherStatus(
                packageName = pkg,
                installed = isPackageInstalled(pkg),
                enabled = if (isPackageInstalled(pkg)) isPackageEnabled(pkg) else false,
                isCurrent = pkg == current
            )
        }
    }

    // Explicitly start our launcher
    fun startOurLauncher() {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                component = ComponentName(MY_LAUNCHER_PACKAGE, MY_LAUNCHER_ACTIVITY)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start our launcher", e)
        }
    }

    // Forceful set: disable stock, clear caches, open settings, and start our launcher
    fun forceSetLauncher(): ForceSetResult {
        return try {
            val results = mutableListOf<CommandResult>()

            // 1) Disable installed stock launchers and force-stop
            getInstalledStockLaunchers().forEach { pkg ->
                val disabled = executeShellCommand("pm disable-user --user 0 $pkg")
                results.add(CommandResult("Disable $pkg", disabled))
                executeShellCommand("am force-stop $pkg")
            }

            // 2) Clear known launcher caches
            executeShellCommand("pm clear $GOOGLE_TV_LAUNCHER")
            executeShellCommand("pm clear $GOOGLE_TV_LAUNCHER_X")

            // 3) Open settings to pick default
            setDefaultLauncherViaSettings()

            // 4) Start our launcher with slight delay
            handler.postDelayed({ startOurLauncherForcibly() }, 500)

            val successCount = results.count { it.success }
            ForceSetResult(
                success = successCount > 0,
                results = results,
                message = "Принудительно установлен лаунчер. Отключено $successCount стоковых лаунчеров."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in forceSetLauncher", e)
            ForceSetResult(false, emptyList(), "Ошибка: ${e.message}")
        }
    }

    private fun startOurLauncherForcibly() {
        try {
            // Way 1: HOME intent
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
            }
            context.startActivity(homeIntent)

            // Way 2: Direct component
            handler.postDelayed({
                try {
                    val directIntent = Intent().apply {
                        component = ComponentName(MY_LAUNCHER_PACKAGE, MY_LAUNCHER_ACTIVITY)
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                        )
                    }
                    context.startActivity(directIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Direct start failed", e)
                }
            }, 1000)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start launcher forcibly", e)
        }
    }

    private fun setDefaultLauncherViaSettings() {
        try {
            // Way 1: System home settings
            val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            // Way 2: launcher chooser after delay
            handler.postDelayed({
                try {
                    val selector = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(selector)
                } catch (_: Exception) { }
            }, 2000)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set via settings", e)
        }
    }

    // Aggregate status across known stock launchers
    fun getLauncherStatus(): StatusSummary {
        val current = getCurrentLauncher()
        val stockStatuses = STOCK_LAUNCHERS.map { pkg ->
            StockLauncherStatus(
                packageName = pkg,
                installed = isPackageInstalled(pkg),
                enabled = if (isPackageInstalled(pkg)) isPackageEnabled(pkg) else false,
                isCurrent = pkg == current
            )
        }
        val enabledStockCount = stockStatuses.count { it.installed && it.enabled }
        return StatusSummary(
            isDefault = current == MY_LAUNCHER_PACKAGE,
            currentLauncher = current ?: "Unknown",
            stockLaunchers = stockStatuses,
            enabledStockCount = enabledStockCount
        )
    }

    // Helpers
    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking package installation: $packageName", e)
            false
        }
    }

    private fun isPackageEnabled(packageName: String): Boolean {
        return try {
            @Suppress("DEPRECATION")
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            packageInfo.applicationInfo?.enabled == true
        } catch (_: Exception) {
            false
        }
    }

    private fun executeShellCommand(command: String): Boolean {
        return try {
            Log.d(TAG, "Executing: $command")
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val exitCode = process.waitFor()
            Log.d(TAG, "Command executed, exit code: $exitCode")
            exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute command: $command", e)
            false
        }
    }

    // Main thread handler for delayed actions
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    // Data classes
    data class CommandResult(val packageName: String, val success: Boolean)
    data class DisableResult(val success: Boolean, val results: List<CommandResult>, val message: String)
    data class EnableResult(val success: Boolean, val results: List<CommandResult>, val message: String)
    data class ForceSetResult(val success: Boolean, val results: List<CommandResult>, val message: String)
    data class StockLauncherStatus(
        val packageName: String,
        val installed: Boolean,
        val enabled: Boolean,
        val isCurrent: Boolean
    )
    data class StatusSummary(
        val isDefault: Boolean,
        val currentLauncher: String,
        val stockLaunchers: List<StockLauncherStatus>,
        val enabledStockCount: Int
    )
}

