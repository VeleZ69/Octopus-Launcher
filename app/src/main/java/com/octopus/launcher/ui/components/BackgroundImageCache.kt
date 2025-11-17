package com.octopus.launcher.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Global cache for background images to prevent reloading on navigation
 * Uses LruCache for memory-efficient caching
 */
object BackgroundImageCache {
    // Calculate cache size: use 1/8 of available memory
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8
    
    private val memoryCache: LruCache<String, ImageBitmap> = object : LruCache<String, ImageBitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: ImageBitmap): Int {
            // Estimate size in KB
            return (bitmap.width * bitmap.height * 4) / 1024
        }
        
        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: ImageBitmap,
            newValue: ImageBitmap?
        ) {
            // Bitmap will be garbage collected automatically
            // No need to recycle as ImageBitmap manages its own lifecycle
        }
    }
    
    /**
     * Get bitmap from cache or load it
     */
    suspend fun getBitmap(
        imagePath: String?,
        screenWidth: Int,
        screenHeight: Int
    ): ImageBitmap? = withContext(Dispatchers.Default) {
        if (imagePath == null) return@withContext null
        
        // Check cache first
        memoryCache.get(imagePath)?.let { return@withContext it }
        
        // Load from file
        try {
            val file = File(imagePath)
            if (!file.exists() || !file.canRead()) {
                return@withContext null
            }
            
            // Decode with options to avoid OOM
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
            
            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return@withContext null
            }
            
            // Calculate sample size to reduce memory usage
            val sampleSize = calculateInSampleSize(options, screenWidth, screenHeight)
            
            options.inJustDecodeBounds = false
            options.inSampleSize = sampleSize
            options.inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory
            options.inDither = false
            options.inScaled = false
            
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return@withContext null
            
            // Scale to screen size if needed
            val processedBitmap = if (bitmap.width > screenWidth || bitmap.height > screenHeight) {
                val widthRatio = screenWidth.toFloat() / bitmap.width
                val heightRatio = screenHeight.toFloat() / bitmap.height
                val scaleFactor = kotlin.math.max(widthRatio, heightRatio)
                
                if (scaleFactor < 1f) {
                    val newWidth = (bitmap.width * scaleFactor).toInt().coerceAtLeast(1)
                    val newHeight = (bitmap.height * scaleFactor).toInt().coerceAtLeast(1)
                    val resized = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                    if (resized != bitmap) {
                        bitmap.recycle() // Free memory
                    }
                    resized
                } else {
                    bitmap
                }
            } else {
                bitmap
            }
            
            val imageBitmap = processedBitmap.asImageBitmap()
            
            // Cache it
            memoryCache.put(imagePath, imageBitmap)
            
            imageBitmap
        } catch (e: Exception) {
            android.util.Log.e("BackgroundImageCache", "Error loading bitmap", e)
            null
        }
    }
    
    /**
     * Preload bitmap into cache
     */
    suspend fun preloadBitmap(
        imagePath: String?,
        screenWidth: Int,
        screenHeight: Int
    ) {
        if (imagePath != null && memoryCache.get(imagePath) == null) {
            getBitmap(imagePath, screenWidth, screenHeight)
        }
    }
    
    /**
     * Clear cache for a specific path (when image is changed/deleted)
     */
    fun clearCache(imagePath: String?) {
        if (imagePath != null) {
            memoryCache.remove(imagePath)
        }
    }
    
    /**
     * Clear all cache
     */
    fun clearAll() {
        memoryCache.evictAll()
    }
    
    /**
     * Check if bitmap is cached
     */
    fun isCached(imagePath: String?): Boolean {
        return imagePath != null && memoryCache.get(imagePath) != null
    }
    
    /**
     * Get bitmap from cache synchronously (if available)
     * Returns null if not cached
     */
    fun getCachedBitmap(imagePath: String?): ImageBitmap? {
        return if (imagePath != null) {
            memoryCache.get(imagePath)
        } else {
            null
        }
    }
    
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height, width) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
}

