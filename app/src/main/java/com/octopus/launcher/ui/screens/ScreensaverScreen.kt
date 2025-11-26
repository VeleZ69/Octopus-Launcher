package com.octopus.launcher.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.ImageLoader
import com.octopus.launcher.ui.viewmodel.ScreensaverViewModel
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@Composable
private fun rememberWallpaperImageLoader(context: android.content.Context): ImageLoader {
    return remember {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
        
        ImageLoader.Builder(context)
            .okHttpClient(okHttpClient)
            .crossfade(true)
            .build()
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ScreensaverScreen(
    onUserInteraction: () -> Unit,
    viewModel: ScreensaverViewModel = viewModel()
) {
    val wallpaper by viewModel.wallpaper.collectAsState()
    val context = LocalContext.current
    val imageLoader = rememberWallpaperImageLoader(context)
    var isImageLoaded by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var loadingState by remember { mutableStateOf("Loading UHD wallpaper...") }

    // Загружаем обои при старте
    LaunchedEffect(Unit) {
        viewModel.loadWallpaperStoreImage()
    }

    // Меняем обои каждые 5 минут
    LaunchedEffect(Unit) {
        while (true) {
            delay(5 * 60 * 1000L) // 5 минут
            viewModel.loadWallpaperStoreImage()
            isImageLoaded = false
            hasError = false
            loadingState = "Loading new wallpaper..."
        }
    }

    // Обработка изменений wallpaper
    LaunchedEffect(wallpaper) {
        wallpaper?.let { wp ->
            isImageLoaded = false
            hasError = false
            loadingState = "Loading UHD image..."
            android.util.Log.d("ScreensaverScreen", "Loading wallpaper: ${wp.url}")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    onUserInteraction()
                    true
                } else {
                    false
                }
            }
    ) {
        // Показываем загрузку
        if (!isImageLoaded && !hasError) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🖼️ Wallpaper Store",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                    Text(
                        text = loadingState,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    wallpaper?.let { wp ->
                        Text(
                            text = wp.title,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // Загружаем изображение
        wallpaper?.let { wp ->
            val imageRequest = ImageRequest.Builder(context)
                .data(wp.url)
                .crossfade(true)
                .listener(
                    onStart = {
                        loadingState = "Downloading UHD wallpaper..."
                    },
                    onSuccess = { _, _ ->
                        isImageLoaded = true
                        hasError = false
                        loadingState = "UHD Wallpaper loaded"
                    },
                    onError = { _, result ->
                        android.util.Log.e("ScreensaverScreen", "Wallpaper load error: ${result.throwable.message}")
                        loadingState = "Error loading wallpaper"
                        isImageLoaded = false
                        hasError = true
                    }
                )
                .build()

            Image(
                painter = rememberAsyncImagePainter(
                    model = imageRequest,
                    imageLoader = imageLoader
                ),
                contentDescription = wp.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Информация о wallpaper
        wallpaper?.let { wp ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🖼️ ${wp.title}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontSize = 18.sp
                )
                Text(
                    text = wp.copyright,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "wallpaper-store.netlify.app",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}