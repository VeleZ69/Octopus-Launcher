package com.octopus.launcher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.octopus.launcher.data.SettingsRepository
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.octopus.launcher.ui.screens.LauncherScreen
import com.octopus.launcher.ui.screens.SettingsScreen
import com.octopus.launcher.ui.theme.OctopusLauncherTheme
import com.octopus.launcher.ui.viewmodel.LauncherViewModel

class MainActivity : ComponentActivity() {
    
    // Permission launcher for location permissions
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permission results
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            // Permissions granted
        } else {
            // Some permissions denied - app will work with limited functionality
        }
    }
    
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        android.util.Log.d("MainActivity", "onCreate called")
        android.util.Log.d("MainActivity", "Intent action: ${intent?.action}, categories: ${intent?.categories}")
        android.util.Log.d("MainActivity", "Intent component: ${intent?.component}")
        
        super.onCreate(savedInstanceState)
        android.util.Log.d("MainActivity", "onCreate completed")
        
        // Log if we're being started as HOME launcher
        if (intent?.categories?.contains(Intent.CATEGORY_HOME) == true) {
            android.util.Log.d("MainActivity", "Started as HOME launcher")
        }
        
        // Set black background immediately to prevent white flash
        window.setBackgroundDrawableResource(android.R.color.black)
        
        // Request permissions if not granted
        requestPermissionsIfNeeded()
        
        // Set window background to match launcher gradient (use first color) or black if image is set
        val settingsRepository = SettingsRepository(this)
        val backgroundImagePath = settingsRepository.getBackgroundImagePath()
        
        if (backgroundImagePath == null) {
            // Only set gradient if no background image
            val color1 = settingsRepository.getBackgroundColor1AsInt()
            val color2 = settingsRepository.getBackgroundColor2().let { color ->
                android.graphics.Color.argb(
                    (color.alpha * 255).toInt(),
                    (color.red * 255).toInt(),
                    (color.green * 255).toInt(),
                    (color.blue * 255).toInt()
                )
            }
            val color3 = settingsRepository.getBackgroundColor3().let { color ->
                android.graphics.Color.argb(
                    (color.alpha * 255).toInt(),
                    (color.red * 255).toInt(),
                    (color.green * 255).toInt(),
                    (color.blue * 255).toInt()
                )
            }
            
            // Create gradient drawable for window background
            val gradientDrawable = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(color1, color2, color3)
            )
            window.setBackgroundDrawable(gradientDrawable)
        } else {
            // When image is set, use gradient as window background so it shows through rounded corners
            // The image will be on top in BlurredBackground, but gradient will show in rounded corners
            val color1 = settingsRepository.getBackgroundColor1AsInt()
            val color2 = settingsRepository.getBackgroundColor2().let { color ->
                android.graphics.Color.argb(
                    (color.alpha * 255).toInt(),
                    (color.red * 255).toInt(),
                    (color.green * 255).toInt(),
                    (color.blue * 255).toInt()
                )
            }
            val color3 = settingsRepository.getBackgroundColor3().let { color ->
                android.graphics.Color.argb(
                    (color.alpha * 255).toInt(),
                    (color.red * 255).toInt(),
                    (color.green * 255).toInt(),
                    (color.blue * 255).toInt()
                )
            }
            val gradientDrawable = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(color1, color2, color3)
            )
            window.setBackgroundDrawable(gradientDrawable)
        }
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
        
        setContent {
            OctopusLauncherTheme {
                Navigation()
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        android.util.Log.d("MainActivity", "onNewIntent called with intent: ${intent.action}, categories: ${intent.categories}")
        // Handle Home button press - return to launcher home screen
        if (intent.categories?.contains(Intent.CATEGORY_HOME) == true) {
            android.util.Log.d("MainActivity", "CATEGORY_HOME detected, setting intent")
            setIntent(intent)
        }
    }
    
    override fun onResume() {
        super.onResume()
        android.util.Log.d("MainActivity", "onResume called")
        
        // Check if we're the default launcher
        val packageManager = packageManager
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        val currentDefaultLauncher = resolveInfo?.activityInfo?.packageName
        android.util.Log.d("MainActivity", "Current default launcher: $currentDefaultLauncher")
        android.util.Log.d("MainActivity", "Our package name: $packageName")
        android.util.Log.d("MainActivity", "Is default launcher: ${currentDefaultLauncher == packageName}")
        
        // When returning to launcher (Home button pressed), ensure we're on home screen
        // This will trigger recomposition in Navigation composable
    }
    
    override fun onPause() {
        super.onPause()
        android.util.Log.d("MainActivity", "onPause called")
    }
    
    override fun onStop() {
        super.onStop()
        android.util.Log.d("MainActivity", "onStop called")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("MainActivity", "onDestroy called")
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        android.util.Log.d("MainActivity", "onWindowFocusChanged called with hasFocus: $hasFocus")
        // When window gains focus (e.g., Home button pressed), return to launcher home
        if (hasFocus) {
            // Trigger recomposition to reset to launcher screen
            // This is handled in Navigation composable via LaunchedEffect
        }
    }
    
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (keyCode == KeyEvent.KEYCODE_SETTINGS && event.action == KeyEvent.ACTION_UP) {
            if (!com.octopus.launcher.utils.TVKeyInjector.openTVSettings(this)) {
                android.util.Log.w("MainActivity", "Hardware settings key pressed but failed to open settings")
            }
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_NOTIFICATION && event.action == KeyEvent.ACTION_UP) {
            if (!com.octopus.launcher.utils.TVKeyInjector.openNotifications(this)) {
                android.util.Log.w("MainActivity", "Hardware notifications key pressed but failed to open notifications")
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }
    
    private fun requestPermissionsIfNeeded() {
        val permissionsToRequest = mutableListOf<String>()
        
        // Check location permissions
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        
        // For Android 13+ (API 33+), request media permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_VIDEO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
            // Notifications on 33+
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // For older versions, request storage permission
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        
        // Request permissions if any are missing
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
        
        // Special permissions (prompt only when needed, and avoid repeat prompts across restarts)
        com.octopus.launcher.utils.PermissionUtils.ensureOverlayPermission(this)
        com.octopus.launcher.utils.PermissionUtils.ensureNotificationListenerPermission(this)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Navigation() {
    val context = LocalContext.current
    val activity = context as? MainActivity
    var currentScreen by remember { mutableStateOf("launcher") }
    val launcherViewModel: LauncherViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    // Preload WeatherViewModel to cache weather data early
    val weatherViewModel: com.octopus.launcher.ui.viewmodel.WeatherViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val lifecycleOwner = LocalLifecycleOwner.current
    val backgroundColors by launcherViewModel.backgroundColors.collectAsState()
    
    // Get background image path
    val backgroundImagePath by launcherViewModel.backgroundImagePath.collectAsState()
    
    // Update window background when colors change (only if no image is set)
    LaunchedEffect(backgroundColors, backgroundImagePath) {
        activity?.let { act ->
            if (backgroundImagePath == null) {
                // Only set gradient if no background image
                val (color1, color2, color3) = backgroundColors
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
            } else {
                // When image is set, use gradient as window background so it shows through rounded corners
                val (color1, color2, color3) = backgroundColors
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
    }
    
    // Handle Home button press - always return to launcher home screen
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // When activity resumes (Home button pressed), return to launcher home
                currentScreen = "launcher"
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Handle back button press
    // Prevent launcher from closing on main screen - ignore back button
    BackHandler(enabled = currentScreen == "launcher") {
        // Do nothing - prevent launcher from closing
        // Launcher should stay open when back is pressed on main screen
    }
    
    // Handle back button on settings screen
    BackHandler(enabled = currentScreen == "settings") {
        launcherViewModel.refreshBackgroundColors()
        currentScreen = "launcher"
    }
    
    // Show launcher screen immediately - no splash
    // Use AnimatedContent for smooth transitions between screens
    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (targetState == "settings") {
                // Entering settings - smooth fade and slide
                fadeIn(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) togetherWith fadeOut(
                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                ) + slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                )
            } else {
                // Returning to launcher - smooth fade and slide
                fadeIn(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) togetherWith fadeOut(
                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                ) + slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                )
            }
        },
        label = "screenNavigation"
    ) { screen ->
        when (screen) {
            "launcher" -> LauncherScreen(
                viewModel = launcherViewModel,
                weatherViewModel = weatherViewModel,
                onNavigateToSettings = { currentScreen = "settings" },
                onShowMoreMenu = { /* Handle more menu */ }
            )
            "settings" -> SettingsScreen(
                onBack = { 
                    launcherViewModel.refreshBackgroundColors()
                    currentScreen = "launcher" 
                },
                onImageChanged = {
                    // Refresh background image immediately when changed
                    launcherViewModel.refreshBackgroundColors()
                }
            )
        }
    }
}