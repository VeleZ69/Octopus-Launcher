package com.octopus.launcher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.drawable.toBitmap
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.octopus.launcher.data.AppInfo
import com.octopus.launcher.R

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AppManagementDialog(
    popularApps: List<AppInfo>,
    allApps: List<AppInfo>,
    onAddApp: (String) -> Unit,
    onRemoveApp: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val editAppsText = stringResource(R.string.edit_apps)
    val removeText = stringResource(R.string.remove)
    val addText = stringResource(R.string.add)
    val deleteAppQuestionText = stringResource(R.string.delete_app_question)
    val pressUpToConfirmText = stringResource(R.string.press_up_to_confirm)
    val cancelText = stringResource(R.string.cancel)
    
    var showAddDialog by remember { mutableStateOf(false) }
    var editingApp by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onDismiss)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.15f))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = editAppsText,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )
                
                LazyRow(
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(popularApps) { app ->
                        EditableAppIcon(
                            app = app,
                            isEditing = editingApp == app.packageName,
                            showDeleteButton = editingApp == app.packageName,
                            onEdit = { 
                                if (editingApp == app.packageName) {
                                    editingApp = null
                                } else {
                                    editingApp = app.packageName
                                }
                            },
                            onRemove = { 
                                showDeleteConfirm = app.packageName
                            }
                        )
                    }
                    
                    // Add button
                    item {
                        AddAppButtonFromDialog(
                            onClick = { showAddDialog = true }
                        )
                    }
                }
                
                if (showAddDialog) {
                    AddAppDialogFromManagement(
                        availableApps = allApps.filter { app -> 
                            !popularApps.any { it.packageName == app.packageName }
                        },
                        onAppSelected = { packageName ->
                            onAddApp(packageName)
                            showAddDialog = false
                        },
                        onDismiss = { showAddDialog = false }
                    )
                }
                
                if (showDeleteConfirm != null) {
                    DeleteConfirmDialog(
                        appName = popularApps.find { it.packageName == showDeleteConfirm }?.name ?: "",
                        onConfirm = {
                            onRemoveApp(showDeleteConfirm!!)
                            showDeleteConfirm = null
                            editingApp = null
                        },
                        onDismiss = { showDeleteConfirm = null }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EditableAppIcon(
    app: AppInfo,
    isEditing: Boolean,
    showDeleteButton: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val removeText = stringResource(R.string.remove)
    var isFocused by remember { mutableStateOf(false) }
    // Unified animations - same as PopularAppIcon
    val animatedScale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "editableAppIconScale"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.8f,
        animationSpec = tween(durationMillis = 150),
        label = "editableAppIconAlpha"
    )
    
    Box(
        modifier = modifier
            .size(80.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                    alpha = animatedAlpha
                }
                .onFocusChanged { isFocused = it.isFocused }
                .focusable()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .then(
                    if (isFocused || isEditing) {
                        Modifier.border(
                            width = 2.dp,
                            color = Color.White,
                            shape = RoundedCornerShape(16.dp)
                        )
                    } else {
                        Modifier
                    }
                )
                .clickable(onClick = onEdit),
            contentAlignment = Alignment.Center
        ) {
            app.icon?.let { drawable ->
                val bitmap = drawable.toBitmap(56, 56)
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = app.name,
                    modifier = Modifier.size(56.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
        
        if (showDeleteButton) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Color.Red)
                    .clickable(onClick = onRemove)
                    .focusable()
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = removeText,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AddAppButtonFromDialog(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val addText = stringResource(R.string.add)
    var isFocused by remember { mutableStateOf(false) }
    // Unified animations - same as PopularAppIcon
    val animatedScale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "addAppButtonFromDialogScale"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.8f,
        animationSpec = tween(durationMillis = 150),
        label = "addAppButtonFromDialogAlpha"
    )
    
    Box(
        modifier = modifier
            .size(64.dp)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                alpha = animatedAlpha
            }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.1f))
            .then(
                if (isFocused) {
                    Modifier.border(
                        width = 2.dp,
                        color = Color.White,
                        shape = CircleShape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = addText,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AddAppDialogFromManagement(
    availableApps: List<AppInfo>,
    onAppSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectAppText = stringResource(R.string.select_app)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.2f))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = selectAppText,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(availableApps.take(20)) { app ->
                    var isFocused by remember { mutableStateOf(false) }
                    // Unified animations - same as PopularAppIcon
                    val animatedScale by animateFloatAsState(
                        targetValue = if (isFocused) 1.15f else 1f,
                        animationSpec = tween(durationMillis = 150),
                        label = "addAppDialogItemScale"
                    )
                    val animatedAlpha by animateFloatAsState(
                        targetValue = if (isFocused) 1f else 0.8f,
                        animationSpec = tween(durationMillis = 150),
                        label = "addAppDialogItemAlpha"
                    )
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .graphicsLayer {
                                scaleX = animatedScale
                                scaleY = animatedScale
                                alpha = animatedAlpha
                            }
                            .onFocusChanged { isFocused = it.isFocused }
                            .focusable()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable(onClick = { onAppSelected(app.packageName) })
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        app.icon?.let { drawable ->
                            val bitmap = drawable.toBitmap(48, 48)
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = app.name,
                                modifier = Modifier.size(48.dp),
                                contentScale = ContentScale.Fit
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
fun DeleteConfirmDialog(
    appName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val deleteAppQuestionText = stringResource(R.string.delete_app_question, appName)
    val pressUpToConfirmText = stringResource(R.string.press_up_to_confirm)
    val removeText = stringResource(R.string.remove)
    val cancelText = stringResource(R.string.cancel)
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.2f))
            .padding(24.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = deleteAppQuestionText,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.9f)
            )
            
            Text(
                text = pressUpToConfirmText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                var isConfirmFocused by remember { mutableStateOf(false) }
                // Unified animations - same as PopularAppIcon
                val confirmAnimatedScale by animateFloatAsState(
                    targetValue = if (isConfirmFocused) 1.15f else 1f,
                    animationSpec = tween(durationMillis = 150),
                    label = "confirmButtonScale"
                )
                val confirmAnimatedAlpha by animateFloatAsState(
                    targetValue = if (isConfirmFocused) 1f else 0.8f,
                    animationSpec = tween(durationMillis = 150),
                    label = "confirmButtonAlpha"
                )
                
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = confirmAnimatedScale
                            scaleY = confirmAnimatedScale
                            alpha = confirmAnimatedAlpha
                        }
                        .onFocusChanged { isConfirmFocused = it.isFocused }
                        .focusable()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            color = if (isConfirmFocused) {
                                Color.White.copy(alpha = 0.25f)
                            } else {
                                Color.White.copy(alpha = 0.15f)
                            }
                        )
                        .clickable(onClick = onConfirm)
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = removeText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                
                var isCancelFocused by remember { mutableStateOf(false) }
                val cancelAnimatedScale by animateFloatAsState(
                    targetValue = if (isCancelFocused) 1.15f else 1f,
                    animationSpec = tween(durationMillis = 150),
                    label = "cancelButtonScale"
                )
                val cancelAnimatedAlpha by animateFloatAsState(
                    targetValue = if (isCancelFocused) 1f else 0.8f,
                    animationSpec = tween(durationMillis = 150),
                    label = "cancelButtonAlpha"
                )
                
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = cancelAnimatedScale
                            scaleY = cancelAnimatedScale
                            alpha = cancelAnimatedAlpha
                        }
                        .onFocusChanged { isCancelFocused = it.isFocused }
                        .focusable()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = cancelText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

