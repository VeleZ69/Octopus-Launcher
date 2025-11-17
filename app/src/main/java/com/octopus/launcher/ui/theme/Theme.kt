package com.octopus.launcher.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun OctopusLauncherTheme(
    content: @Composable () -> Unit,
) {
    val colorScheme = darkColorScheme(
        primary = OctopusPrimary,
        secondary = OctopusSecondary,
        tertiary = OctopusAccent,
        background = OctopusBackground,
        surface = OctopusSurface,
        surfaceVariant = OctopusSurfaceVariant,
        onPrimary = OctopusOnPrimary,
        onBackground = OctopusOnBackground,
        onSurface = OctopusOnSurface,
        error = OctopusError
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}