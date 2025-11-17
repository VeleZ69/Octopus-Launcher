package com.octopus.launcher.data

import android.app.ActivityManager
import android.content.Context
import kotlinx.coroutines.delay
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException

data class SystemMetrics(
    val memoryUsagePercent: Int,
    val cpuUsagePercent: Int
)

class SystemMetricsRepository(private val context: Context) {
    
    suspend fun getSystemMetrics(): SystemMetrics {
        return SystemMetrics(
            memoryUsagePercent = getMemoryUsagePercent(),
            cpuUsagePercent = getCpuUsagePercent()
        )
    }
    
    private fun getMemoryUsagePercent(): Int {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            
            val totalMemory = memInfo.totalMem
            val availableMemory = memInfo.availMem
            val usedMemory = totalMemory - availableMemory
            
            val usagePercent = ((usedMemory.toFloat() / totalMemory.toFloat()) * 100).toInt()
            return usagePercent.coerceIn(0, 100)
        } catch (e: Exception) {
            return 0
        }
    }
    
    private suspend fun getCpuUsagePercent(): Int {
        return try {
            // Read CPU stats twice with a small delay to calculate usage
            val stats1 = readCpuStats()
            delay(100) // Small delay
            val stats2 = readCpuStats()
            
            if (stats1 == null || stats2 == null) return 0
            
            val total1 = stats1.total
            val idle1 = stats1.idle
            val total2 = stats2.total
            val idle2 = stats2.idle
            
            val totalDiff = total2 - total1
            val idleDiff = idle2 - idle1
            
            if (totalDiff <= 0) return 0
            
            val usage = ((totalDiff - idleDiff).toFloat() / totalDiff.toFloat() * 100).toInt()
            usage.coerceIn(0, 100)
        } catch (e: Exception) {
            0
        }
    }
    
    private data class CpuStat(
        val user: Long,
        val nice: Long,
        val system: Long,
        val idle: Long,
        val iowait: Long,
        val irq: Long,
        val softirq: Long
    ) {
        val total: Long get() = user + nice + system + idle + iowait + irq + softirq
    }
    
    private fun readCpuStats(): CpuStat? {
        return try {
            val reader = BufferedReader(FileReader("/proc/stat"))
            val line = reader.readLine() // Read first cpu line (aggregate)
            reader.close()
            
            if (line?.startsWith("cpu ") == true) {
                val parts = line.split("\\s+".toRegex())
                if (parts.size >= 8) {
                    CpuStat(
                        user = parts[1].toLongOrNull() ?: 0L,
                        nice = parts[2].toLongOrNull() ?: 0L,
                        system = parts[3].toLongOrNull() ?: 0L,
                        idle = parts[4].toLongOrNull() ?: 0L,
                        iowait = parts[5].toLongOrNull() ?: 0L,
                        irq = parts[6].toLongOrNull() ?: 0L,
                        softirq = parts[7].toLongOrNull() ?: 0L
                    )
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: IOException) {
            null
        }
    }
}

