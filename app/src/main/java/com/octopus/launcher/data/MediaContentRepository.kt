package com.octopus.launcher.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MediaContent(
    val title: String,
    val thumbnailUrl: String? = null,
    val videoId: String? = null,
    val appPackage: String,
    val appName: String
)

data class MediaAppContent(
    val appPackage: String,
    val appName: String,
    val contents: List<MediaContent>
)

class MediaContentRepository(private val context: Context) {
    
    // Media app packages
    private val mediaApps = listOf(
        "com.google.android.youtube",
        "com.netflix.mediaclient",
        "com.amazon.avod.thirdpartyclient",
        "com.spotify.music",
        "com.hulu.plus",
        "com.disney.disneyplus"
    )
    
    suspend fun getMediaAppsWithContent(): List<MediaAppContent> = withContext(Dispatchers.IO) {
        val installedMediaApps = getInstalledMediaApps()
        
        installedMediaApps.mapNotNull { appInfo ->
            val content = getRecentContentForApp(appInfo.packageName)
            if (content.isNotEmpty()) {
                MediaAppContent(
                    appPackage = appInfo.packageName,
                    appName = appInfo.name,
                    contents = content
                )
            } else {
                null
            }
        }
    }
    
    private fun getInstalledMediaApps(): List<AppInfo> {
        val pm = context.packageManager
        return mediaApps.mapNotNull { packageName ->
            try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                AppInfo.fromApplicationInfo(pm, appInfo)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }
    }
    
    private suspend fun getRecentContentForApp(packageName: String): List<MediaContent> = withContext(Dispatchers.IO) {
        // Try to get content from Content Provider first
        // For streaming apps, Content Providers may not be available
        // In that case, we fall back to mock data or app-specific APIs
        val contentFromProvider = tryGetContentFromProvider(packageName)
        if (contentFromProvider.isNotEmpty()) {
            android.util.Log.d("MediaContentRepository", "Got ${contentFromProvider.size} items from Content Provider for $packageName")
            contentFromProvider
        } else {
            // Fallback to app-specific methods or mock data
            // Note: For production, consider using:
            // - YouTube Data API v3 for YouTube
            // - Netflix Partner API for Netflix
            // - Amazon Prime Video API for Prime Video
            // - Spotify Web API for Spotify
            android.util.Log.d("MediaContentRepository", "Using fallback data for $packageName")
            when (packageName) {
                "com.google.android.youtube" -> getYouTubeRecentVideos()
                "com.netflix.mediaclient" -> getNetflixRecentContent()
                "com.amazon.avod.thirdpartyclient" -> getPrimeVideoRecentContent()
                "com.spotify.music" -> getSpotifyRecentContent()
                else -> emptyList()
            }
        }
    }
    
    private fun tryGetContentFromProvider(packageName: String): List<MediaContent> {
        return try {
            // Try to get recent videos from MediaStore
            val recentVideos = getRecentVideosFromMediaStore(packageName)
            if (recentVideos.isNotEmpty()) {
                recentVideos
            } else {
                // Try app-specific Content Providers
                when (packageName) {
                    "com.google.android.youtube" -> tryGetYouTubeContent()
                    else -> emptyList()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    private fun getRecentVideosFromMediaStore(packageName: String): List<MediaContent> {
        val contentResolver: ContentResolver = context.contentResolver
        val videos = mutableListOf<MediaContent>()
        
        try {
            // Query MediaStore for recent videos
            // Note: MediaStore typically contains locally stored videos, not streaming app history
            // For streaming apps (YouTube, Netflix, etc.), we need to use their APIs or Content Providers
            val projection = arrayOf(
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media._ID
            )
            
            val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
            val cursor: Cursor? = contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )
            
            cursor?.use {
                if (it.count == 0) {
                    return emptyList()
                }
                
                val titleColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val dataColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                
                var count = 0
                while (it.moveToNext() && count < 5) {
                    val title = it.getString(titleColumn) ?: "Неизвестное видео"
                    val data = it.getString(dataColumn)
                    val id = it.getLong(idColumn)
                    
                    // For streaming apps, MediaStore usually won't have their content
                    // But we can still show recent local videos if available
                    // For better results, use app-specific APIs (YouTube Data API, etc.)
                    if (data != null) {
                        // Try to match by package name in path (may not work for streaming apps)
                        val matchesPackage = packageName.isEmpty() || 
                            data.contains(packageName, ignoreCase = true) ||
                            data.contains("/Android/data/$packageName/", ignoreCase = true)
                        
                        if (matchesPackage || packageName.isEmpty()) {
                            // Generate thumbnail URI from MediaStore
                            // Note: Thumbnails API is deprecated, but we'll use it for now
                            // In production, consider using ContentResolver.loadThumbnail() for Android 10+
                            val thumbnailUri = try {
                                @Suppress("DEPRECATION")
                                Uri.withAppendedPath(
                                    MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI,
                                    id.toString()
                                )
                            } catch (e: Exception) {
                                null
                            }
                            
                            videos.add(
                                MediaContent(
                                    title = title,
                                    thumbnailUrl = thumbnailUri?.toString(),
                                    videoId = id.toString(),
                                    appPackage = packageName,
                                    appName = getAppName(packageName)
                                )
                            )
                            count++
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            // Permission denied - user needs to grant READ_MEDIA_VIDEO permission
            android.util.Log.w("MediaContentRepository", "Permission denied for MediaStore access")
        } catch (e: Exception) {
            android.util.Log.e("MediaContentRepository", "Error querying MediaStore", e)
        }
        
        return videos
    }
    
    private fun tryGetYouTubeContent(): List<MediaContent> {
        // Try to access YouTube Content Provider (if available)
        // Note: YouTube doesn't provide public Content Provider for watch history
        // For real watch history, you need to use YouTube Data API v3 with OAuth2
        // This method attempts to access internal Content Providers (may not work)
        return try {
            val contentResolver: ContentResolver = context.contentResolver
            
            // Try common YouTube Content Provider URIs
            // These are not publicly documented and may not work
            val youtubeUris = listOf(
                Uri.parse("content://com.google.android.youtube.provider/history"),
                Uri.parse("content://com.google.android.youtube/watch_history"),
                Uri.parse("content://com.google.android.youtube/recent"),
                Uri.parse("content://com.google.android.youtube.provider/recent_watches")
            )
            
            for (uri in youtubeUris) {
                try {
                    val cursor: Cursor? = contentResolver.query(
                        uri,
                        null,
                        null,
                        null,
                        null
                    )
                    cursor?.use {
                        if (it.count > 0) {
                            val videos = mutableListOf<MediaContent>()
                            // Process cursor if available
                            // Note: YouTube Content Provider structure is not publicly documented
                            // Column names and structure are unknown
                            android.util.Log.d("MediaContentRepository", "Found YouTube Content Provider: $uri")
                            // TODO: Parse cursor columns if structure is known
                            return videos
                        }
                    }
                } catch (e: SecurityException) {
                    // Permission denied or provider doesn't exist
                    continue
                } catch (e: Exception) {
                    // URI not available, try next
                    continue
                }
            }
            emptyList()
        } catch (e: Exception) {
            android.util.Log.d("MediaContentRepository", "YouTube Content Provider not available", e)
            emptyList()
        }
    }
    
    private fun getAppName(packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
    
    private fun getYouTubeRecentVideos(): List<MediaContent> {
        // TODO: Use YouTube API or Content Provider to get actual watch history
        // For now, return mock data
        return listOf(
            MediaContent(
                title = "Последнее просмотренное видео 1",
                videoId = "mock1",
                appPackage = "com.google.android.youtube",
                appName = "YouTube"
            ),
            MediaContent(
                title = "Последнее просмотренное видео 2",
                videoId = "mock2",
                appPackage = "com.google.android.youtube",
                appName = "YouTube"
            ),
            MediaContent(
                title = "Последнее просмотренное видео 3",
                videoId = "mock3",
                appPackage = "com.google.android.youtube",
                appName = "YouTube"
            ),
            MediaContent(
                title = "Последнее просмотренное видео 4",
                videoId = "mock4",
                appPackage = "com.google.android.youtube",
                appName = "YouTube"
            ),
            MediaContent(
                title = "Последнее просмотренное видео 5",
                videoId = "mock5",
                appPackage = "com.google.android.youtube",
                appName = "YouTube"
            )
        )
    }
    
    private fun getNetflixRecentContent(): List<MediaContent> {
        return listOf(
            MediaContent(
                title = "Недавно просмотренный сериал",
                appPackage = "com.netflix.mediaclient",
                appName = "Netflix"
            ),
            MediaContent(
                title = "Продолжить просмотр",
                appPackage = "com.netflix.mediaclient",
                appName = "Netflix"
            ),
            MediaContent(
                title = "Рекомендация Netflix",
                appPackage = "com.netflix.mediaclient",
                appName = "Netflix"
            )
        )
    }
    
    private fun getPrimeVideoRecentContent(): List<MediaContent> {
        return listOf(
            MediaContent(
                title = "Недавно просмотренный фильм",
                appPackage = "com.amazon.avod.thirdpartyclient",
                appName = "Prime Video"
            ),
            MediaContent(
                title = "Продолжить просмотр",
                appPackage = "com.amazon.avod.thirdpartyclient",
                appName = "Prime Video"
            )
        )
    }
    
    private fun getSpotifyRecentContent(): List<MediaContent> {
        return listOf(
            MediaContent(
                title = "Недавно прослушанный плейлист",
                appPackage = "com.spotify.music",
                appName = "Spotify"
            ),
            MediaContent(
                title = "Продолжить прослушивание",
                appPackage = "com.spotify.music",
                appName = "Spotify"
            )
        )
    }
    
    fun openMediaContent(content: MediaContent) {
        try {
            when (content.appPackage) {
                "com.google.android.youtube" -> {
                    // Open YouTube video
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setPackage("com.google.android.youtube")
                        data = android.net.Uri.parse("https://www.youtube.com/watch?v=${content.videoId}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                else -> {
                    // Open app
                    val intent = context.packageManager.getLaunchIntentForPackage(content.appPackage)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

