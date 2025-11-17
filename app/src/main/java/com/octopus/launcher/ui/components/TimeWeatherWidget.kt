package com.octopus.launcher.ui.components

import androidx.compose.animation.core.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.octopus.launcher.ui.viewmodel.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TimeWeatherWidget(
    modifier: Modifier = Modifier,
    viewModel: WeatherViewModel = viewModel()
) {
    val weatherInfo by viewModel.weatherInfo.collectAsState()
    
    // Weather is already updated by WeatherViewModel every 30 minutes
    // Just refresh once on first load if needed
    LaunchedEffect(Unit) {
        // Only refresh if weather info is null (first load)
        if (weatherInfo == null) {
            viewModel.refreshWeather()
        }
    }
    
    var currentHour1 by remember { mutableStateOf("") }
    var currentHour2 by remember { mutableStateOf("") }
    var currentMinute1 by remember { mutableStateOf("") }
    var currentMinute2 by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }
    
    // Cache formatters to avoid recreating them on each recomposition
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, d MMMM", Locale.forLanguageTag("ru-RU")) }
    
    // Reusable Calendar instance to avoid creating new objects
    val calendar = remember { java.util.Calendar.getInstance() }
    
    // Update time every second for smooth flip animation
    // Use try-catch to handle cancellation properly
    LaunchedEffect(Unit) {
        fun updateTime() {
            try {
                // Reuse calendar instance instead of creating new Date() objects
                calendar.timeInMillis = System.currentTimeMillis()
                
                val timeStr = timeFormat.format(calendar.time)
                if (timeStr.length >= 5) {
                    // Convert to strings once and compare
                    val h1Str = timeStr[0].toString()
                    val h2Str = timeStr[1].toString()
                    val m1Str = timeStr[3].toString()
                    val m2Str = timeStr[4].toString()
                    
                    // Only update if changed to avoid unnecessary recompositions
                    if (currentHour1 != h1Str) currentHour1 = h1Str
                    if (currentHour2 != h2Str) currentHour2 = h2Str
                    if (currentMinute1 != m1Str) currentMinute1 = m1Str
                    if (currentMinute2 != m2Str) currentMinute2 = m2Str
                }
                
                // Only update date if it changed (not every second)
                val newDate = dateFormat.format(calendar.time)
                if (currentDate != newDate) {
                    currentDate = newDate
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Properly handle cancellation - rethrow to allow coroutine cancellation
                throw e
            }
        }
        
        // Initial update
        updateTime()
        
        // Update every second - will be cancelled when composable leaves composition
        try {
            while (true) {
                delay(1000)
                updateTime()
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Expected when composable is removed - just exit
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time section - simple clock display
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Time display - large bold numbers
                // Use cached timeFormat instead of creating new SimpleDateFormat
                val currentTime = if (currentHour1.isNotEmpty() && currentHour2.isNotEmpty() && 
                                     currentMinute1.isNotEmpty() && currentMinute2.isNotEmpty()) {
                    "$currentHour1$currentHour2:$currentMinute1$currentMinute2"
                } else {
                    timeFormat.format(calendar.apply { timeInMillis = System.currentTimeMillis() }.time)
                }
                
                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                
                // Date text - use cached dateFormat
                Text(
                    text = currentDate.ifEmpty { 
                        dateFormat.format(calendar.apply { timeInMillis = System.currentTimeMillis() }.time)
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 18.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                )
            }
            
            // Weather section - only show if weather info is available (no default values)
            weatherInfo?.let { info ->
                // Animation for weather widget appearance
                var isVisible by remember { mutableStateOf(false) }
                val alpha by animateFloatAsState(
                    targetValue = if (isVisible) 1f else 0f,
                    animationSpec = tween(durationMillis = 800),
                    label = "weatherAlpha"
                )
                
                LaunchedEffect(info) {
                    isVisible = true
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.alpha(alpha)
                ) {
                    // Weather icon with animation
                    WeatherIcon(
                        condition = info.condition,
                        modifier = Modifier.size(64.dp)
                    )
                    
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${info.temperature}°",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Light,
                                color = Color.White.copy(alpha = 0.95f)
                            )
                        )
                        Text(
                            text = info.condition,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 18.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        )
                        Text(
                            text = info.city,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherIcon(
    condition: String,
    modifier: Modifier = Modifier
) {
    val isSnow = condition.contains("Снег", ignoreCase = true) || 
                 condition.contains("снегопад", ignoreCase = true) ||
                 condition.contains("снежные", ignoreCase = true)
    val isSunny = condition.contains("Ясно", ignoreCase = true) || 
                  condition.contains("Солнечно", ignoreCase = true) ||
                  condition.contains("ясно", ignoreCase = true) ||
                  condition.contains("Преимущественно", ignoreCase = true)
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when {
            isSnow -> SnowAnimation()
            isSunny -> SunAnimation()
            else -> {
                // Default icon for other conditions
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.6f),
                        radius = size.minDimension / 3
                    )
                }
            }
        }
    }
}

@Composable
fun SunAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "sunAnimation")
    
    // Rotation animation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sunRotation"
    )
    
    // Pulsing animation
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sunPulse"
    )
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 3.5f
            val rayLength = radius * 0.5f
            val rayCount = 8
            
            // Draw sun circle
            drawCircle(
                color = Color(0xFFFFD700), // Gold color
                radius = radius,
                center = center
            )
            
            // Draw rays
            rotate(rotation, center) {
                for (i in 0 until rayCount) {
                    val angle = (360f / rayCount) * i
                    val radians = (angle * PI / 180f).toFloat()
                    val startX = center.x + (radius + rayLength * 0.2f) * cos(radians)
                    val startY = center.y + (radius + rayLength * 0.2f) * sin(radians)
                    val endX = center.x + (radius + rayLength) * cos(radians)
                    val endY = center.y + (radius + rayLength) * sin(radians)
                    
                    drawLine(
                        color = Color(0xFFFFD700),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 4.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
fun SnowAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "snowAnimation")
    
    // Multiple snowflakes with different speeds
    val snowflake1Y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "snowflake1"
    )
    
    val snowflake2Y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "snowflake2"
    )
    
    val snowflake3Y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "snowflake3"
    )
    
    val snowflake4Y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "snowflake4"
    )
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sizePx = size.minDimension
            
            // Draw multiple snowflakes
            drawSnowflake(
                center = Offset(size.width * 0.3f, size.height * snowflake1Y),
                size = sizePx * 0.2f
            )
            drawSnowflake(
                center = Offset(size.width * 0.7f, size.height * snowflake2Y),
                size = sizePx * 0.15f
            )
            drawSnowflake(
                center = Offset(size.width * 0.5f, size.height * snowflake3Y),
                size = sizePx * 0.25f
            )
            drawSnowflake(
                center = Offset(size.width * 0.15f, size.height * snowflake4Y),
                size = sizePx * 0.12f
            )
        }
    }
}

private fun DrawScope.drawSnowflake(center: Offset, size: Float) {
    val color = Color.White.copy(alpha = 1f)
    val branchLength = size / 2
    
    // Draw 6 branches
    for (i in 0 until 6) {
        val angle = (60f * i) * (PI / 180f).toFloat()
        val endX = center.x + branchLength * cos(angle)
        val endY = center.y + branchLength * sin(angle)
        
        // Main branch
        drawLine(
            color = color,
            start = center,
            end = Offset(endX, endY),
            strokeWidth = 3.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        
        // Side branches
        val sideBranchLength = branchLength * 0.4f
        val sideAngle1 = angle + (30f * (PI / 180f).toFloat())
        val sideAngle2 = angle - (30f * (PI / 180f).toFloat())
        val midX = center.x + branchLength * 0.6f * cos(angle)
        val midY = center.y + branchLength * 0.6f * sin(angle)
        
        drawLine(
            color = color,
            start = Offset(midX, midY),
            end = Offset(
                midX + sideBranchLength * cos(sideAngle1),
                midY + sideBranchLength * sin(sideAngle1)
            ),
            strokeWidth = 2.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(midX, midY),
            end = Offset(
                midX + sideBranchLength * cos(sideAngle2),
                midY + sideBranchLength * sin(sideAngle2)
            ),
            strokeWidth = 2.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
    
    // Center circle
    drawCircle(
        color = color,
        radius = size * 0.12f,
        center = center
    )
}

