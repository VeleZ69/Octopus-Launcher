package com.octopus.launcher.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.octopus.launcher.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class ImageItem(
    val path: String,
    val name: String,
    val thumbnail: android.graphics.Bitmap?
)

// Cache for image paths only (not thumbnails)
private var cachedImagePaths: List<Pair<String, String>>? = null // List of (path, name) pairs
private var cacheTimestamp: Long = 0
private const val CACHE_DURATION_MS = 30000L // 30 seconds cache

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ImageFileBrowser(
    onImageSelected: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val backText = stringResource(R.string.back)
    val selectImageTitle = stringResource(R.string.select_image_title)
    val loadingImagesText = stringResource(R.string.loading_images)
    val errorText = stringResource(R.string.error)
    val ensurePermissionsText = stringResource(R.string.ensure_permissions)
    val noImagesFoundText = stringResource(R.string.no_images_found)
    val noPreviewText = stringResource(R.string.no_preview)
    val errorLoadingImagesText = stringResource(R.string.error_loading_images)
    
    var images by remember { mutableStateOf<List<ImageItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Scan images every time the browser composable is created/shown
    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        android.util.Log.d("ImageFileBrowser", "Scanning images on browser open")
        
        try {
            // Check cache first - only cache paths, not thumbnails
            val now = System.currentTimeMillis()
            val cachedPaths = if (cachedImagePaths != null && (now - cacheTimestamp) < CACHE_DURATION_MS) {
                android.util.Log.d("ImageFileBrowser", "Using cached image paths (${cachedImagePaths!!.size} items)")
                cachedImagePaths
            } else {
                null
            }
            
            images = if (cachedPaths != null) {
                // Load thumbnails on demand from cached paths
                withContext(Dispatchers.IO) {
                    cachedPaths.map { (path, name) ->
                        ImageItem(
                            path = path,
                            name = name,
                            thumbnail = loadThumbnail(path) // Load thumbnail on demand
                        )
                    }
                }
            } else {
                withContext(Dispatchers.IO) {
                    try {
                        val loaded = loadImagesFromDevice(context)
                        // Cache only paths, not thumbnails
                        cachedImagePaths = loaded.map { it.path to it.name }
                        cacheTimestamp = System.currentTimeMillis()
                        android.util.Log.d("ImageFileBrowser", "Loaded and cached ${loaded.size} image paths (thumbnails loaded on demand)")
                        loaded
                    } catch (e: Exception) {
                        errorMessage = String.format(errorLoadingImagesText, e.message ?: "")
                        android.util.Log.e("ImageFileBrowser", "Error loading images", e)
                        emptyList()
                    }
                }
            }
        } catch (e: Exception) {
            // Handle cancellation gracefully
            if (e !is kotlinx.coroutines.CancellationException) {
                errorMessage = String.format(errorLoadingImagesText, e.message ?: "")
                android.util.Log.e("ImageFileBrowser", "Error in LaunchedEffect", e)
            }
            images = emptyList()
        } finally {
            isLoading = false
        }
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Header with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = backText,
                    tint = Color.White
                )
            }
            Text(
                text = selectImageTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.width(48.dp)) // Balance the back button
        }
        
        // Content
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = loadingImagesText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = errorMessage ?: errorText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Red.copy(alpha = 0.9f)
                            )
                            Text(
                                text = ensurePermissionsText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                images.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = noImagesFoundText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 200.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(images) { imageItem ->
                            ImageThumbnail(
                                imageItem = imageItem,
                                onClick = { onImageSelected(imageItem.path) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ImageThumbnail(
    imageItem: ImageItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    var thumbnail by remember(imageItem.path) { mutableStateOf<android.graphics.Bitmap?>(imageItem.thumbnail) }
    
    // Unified animations - same as PopularAppIcon
    val animatedScale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "imageThumbnailScale"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.8f,
        animationSpec = tween(durationMillis = 150),
        label = "imageThumbnailAlpha"
    )
    
    // Load thumbnail on demand if not already loaded
    LaunchedEffect(imageItem.path) {
        if (thumbnail == null) {
            thumbnail = withContext(Dispatchers.IO) {
                loadThumbnail(imageItem.path)
            }
        }
    }
    
    // Clean up thumbnail when composable is removed to prevent memory leaks
    DisposableEffect(imageItem.path) {
        onDispose {
            thumbnail?.let { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
            thumbnail = null
        }
    }
    
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                alpha = animatedAlpha
            }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clip(RoundedCornerShape(12.dp))
            .background(
                color = if (isFocused) {
                    Color.White.copy(alpha = 0.3f)
                } else {
                    Color.White.copy(alpha = 0.1f)
                }
            )
            .clickable(onClick = onClick)
            .padding(if (isFocused) 4.dp else 8.dp)
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail!!.asImageBitmap(),
                contentDescription = imageItem.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Нет превью",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

suspend fun loadImagesFromDevice(context: Context): List<ImageItem> = withContext(Dispatchers.IO) {
    val images = mutableListOf<ImageItem>()
    val imagePaths = mutableSetOf<String>() // Track canonical paths to avoid duplicates
    
    try {
        // First, try MediaStore (standard way)
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE
        )
        
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        
        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )
        
        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            
            while (it.moveToNext() && images.size < 150) {
                try {
                    val id = it.getLong(idColumn)
                    val name = it.getString(nameColumn) ?: "Unknown"
                    val pathRaw = it.getString(dataColumn) ?: continue
                    val size = it.getLong(sizeColumn)
                    
                    val file = File(pathRaw)
                    val path = try { file.canonicalPath } catch (e: Exception) { file.absolutePath }
                    
                    // Skip if path already added or file doesn't exist
                    if (imagePaths.contains(path)) continue
                    if (!file.exists() || !file.canRead()) continue
                    
                    // Skip very large files (>50MB) to avoid memory issues
                    if (size > 50 * 1024 * 1024) continue
                    
                    // Check if it's a valid image file
                    val extension = file.extension.lowercase()
                    if (extension !in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp")) continue
                    
                    // Don't load thumbnail here - load on demand
                    images.add(ImageItem(path = path, name = name, thumbnail = null))
                    imagePaths.add(path)
                } catch (e: Exception) {
                    android.util.Log.w("ImageFileBrowser", "Error processing image: ${e.message}")
                    continue
                }
            }
        }
        
        // Also scan /sdcard directly for new images not yet in MediaStore
        try {
            val sdcardDir = File("/sdcard")
            if (sdcardDir.exists() && sdcardDir.canRead()) {
                scanDirectoryForImages(sdcardDir, images, imagePaths, maxDepth = 3)
            }
            
            // Also check standard Android directories
            val dcimDir = File("/sdcard/DCIM")
            if (dcimDir.exists() && dcimDir.canRead()) {
                scanDirectoryForImages(dcimDir, images, imagePaths, maxDepth = 5)
            }
            
            val picturesDir = File("/sdcard/Pictures")
            if (picturesDir.exists() && picturesDir.canRead()) {
                scanDirectoryForImages(picturesDir, images, imagePaths, maxDepth = 5)
            }
        } catch (e: Exception) {
            android.util.Log.w("ImageFileBrowser", "Error scanning directories: ${e.message}")
        }
        
        // Sort by modification date (newest first)
        images.sortByDescending { File(it.path).lastModified() }
        
        // Limit to 200 most recent
        return@withContext images.take(200)
    } catch (e: SecurityException) {
        android.util.Log.e("ImageFileBrowser", "Permission denied", e)
        throw e
    } catch (e: Exception) {
        android.util.Log.e("ImageFileBrowser", "Error loading images", e)
        throw e
    }
}

// Load thumbnail on demand (not cached)
private fun loadThumbnail(path: String): android.graphics.Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)
        
        if (options.outWidth > 0 && options.outHeight > 0) {
            options.inSampleSize = calculateInSampleSize(options, 200, 200)
            options.inJustDecodeBounds = false
            BitmapFactory.decodeFile(path, options)
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

private fun scanDirectoryForImages(
    directory: File,
    images: MutableList<ImageItem>,
    imagePaths: MutableSet<String>,
    maxDepth: Int,
    currentDepth: Int = 0
) {
    if (currentDepth >= maxDepth) return
    
    try {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                scanDirectoryForImages(file, images, imagePaths, maxDepth, currentDepth + 1)
            } else if (file.isFile && file.canRead()) {
                val extension = file.extension.lowercase()
                if (extension in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp")) {
                    val path = try { file.canonicalPath } catch (e: Exception) { file.absolutePath }
                    if (!imagePaths.contains(path) && file.length() > 0 && file.length() < 50 * 1024 * 1024) {
                        try {
                            // Don't load thumbnail here - load on demand
                            images.add(ImageItem(path = path, name = file.name, thumbnail = null))
                            imagePaths.add(path)
                        } catch (e: Exception) {
                            // Skip this file
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {
        // Skip this directory
    }
}

private fun calculateInSampleSize(
    options: android.graphics.BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int
): Int {
    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1
    
    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2
        
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    
    return inSampleSize
}

