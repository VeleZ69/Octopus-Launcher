package com.octopus.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {
    
    suspend fun getAllApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        try {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            
            // Try to get all apps - use 0 flag to get all apps (works with QUERY_ALL_PACKAGES permission)
            val resolveInfos: List<ResolveInfo> = try {
                // Try with 0 flag first (gets all apps)
                pm.queryIntentActivities(intent, 0)
            } catch (e: SecurityException) {
                // Fallback to MATCH_DEFAULT_ONLY
                try {
                    pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                } catch (e2: Exception) {
                    e2.printStackTrace()
                    emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                } catch (e2: Exception) {
                    e2.printStackTrace()
                    emptyList()
                }
            }
            
            resolveInfos.mapNotNull { resolveInfo ->
                try {
                    val appInfo = resolveInfo.activityInfo?.applicationInfo
                    if (appInfo != null) {
                        AppInfo.fromApplicationInfo(pm, appInfo)
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }.distinctBy { it.packageName }
             .sortedBy { it.name.lowercase() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    suspend fun getLeanbackApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        try {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            }
            
            val resolveInfos: List<ResolveInfo> = try {
                // Try with 0 flag first
                pm.queryIntentActivities(intent, 0)
            } catch (e: SecurityException) {
                try {
                    pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                } catch (e2: Exception) {
                    e2.printStackTrace()
                    emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                } catch (e2: Exception) {
                    e2.printStackTrace()
                    emptyList()
                }
            }
            
            resolveInfos.mapNotNull { resolveInfo ->
                try {
                    val appInfo = resolveInfo.activityInfo?.applicationInfo
                    if (appInfo != null) {
                        AppInfo.fromApplicationInfo(pm, appInfo)
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }.distinctBy { it.packageName }
             .sortedBy { it.name.lowercase() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    fun launchApp(packageName: String) {
        try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(packageName)
            intent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // Use applicationContext to avoid potential context leaks
                // FLAG_ACTIVITY_NEW_TASK allows starting activity from non-Activity context
                context.applicationContext.startActivity(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

