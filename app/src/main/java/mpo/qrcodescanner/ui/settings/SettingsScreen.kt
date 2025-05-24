package mpo.qrcodescanner.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import mpo.qrcodescanner.billing.BillingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    billingViewModel: BillingViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToSubscription: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val isPremium by billingViewModel.isPremium.collectAsState()
    val settingsState by settingsViewModel.settingsState.collectAsState()
    
    var showVibrationDialog by remember { mutableStateOf(false) }
    var showSoundDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showBrightnessDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Scan Settings Section
            SettingsSection(title = "Scan Settings") {
                SwitchSettingsItem(
                    icon = Icons.Outlined.Vibration,
                    title = "Vibration",
                    subtitle = "Vibrate on successful scan",
                    checked = settingsState.vibrationEnabled,
                    onCheckedChange = { settingsViewModel.updateVibrationEnabled(it) },
                    onClick = { showVibrationDialog = true }
                )
                SwitchSettingsItem(
                    icon = Icons.Outlined.VolumeUp,
                    title = "Sound",
                    subtitle = "Play sound on successful scan",
                    checked = settingsState.soundEnabled,
                    onCheckedChange = { settingsViewModel.updateSoundEnabled(it) },
                    onClick = { showSoundDialog = true }
                )
                SwitchSettingsItem(
                    icon = Icons.Outlined.Save,
                    title = "Auto-save",
                    subtitle = "Automatically save scanned codes",
                    checked = settingsState.autoSaveEnabled,
                    onCheckedChange = { settingsViewModel.updateAutoSaveEnabled(it) }
                )
            }

            // Appearance Section
            SettingsSection(title = "Appearance") {
                SettingsItem(
                    icon = Icons.Outlined.Palette,
                    title = "Theme",
                    subtitle = when (settingsState.themeMode) {
                        ThemeMode.LIGHT -> "Light"
                        ThemeMode.DARK -> "Dark"
                        ThemeMode.SYSTEM -> "System default"
                    },
                    onClick = { showThemeDialog = true }
                )
                SettingsItem(
                    icon = Icons.Outlined.Brightness6,
                    title = "Scanner Brightness",
                    subtitle = "Adjust scanner screen brightness",
                    onClick = { showBrightnessDialog = true }
                )
            }

            // Premium Features Section
            SettingsSection(title = "Premium Features") {
                SettingsItem(
                    icon = Icons.Outlined.Star,
                    title = if (isPremium) "Premium Active" else "Upgrade to Premium",
                    subtitle = if (isPremium) "Thank you for your support!" else "Remove ads and unlock all features",
                    onClick = { if (!isPremium) onNavigateToSubscription() }
                )
            }

            // About Section
            SettingsSection(title = "About") {
                SettingsItem(
                    icon = Icons.Outlined.Info,
                    title = "Version",
                    subtitle = "1.0.0",
                    onClick = null
                )
                SettingsItem(
                    icon = Icons.Outlined.Policy,
                    title = "Privacy Policy",
                    subtitle = "Read our privacy policy",
                    onClick = { uriHandler.openUri("https://raw.githubusercontent.com/visiosoft/mypaperlessoffice.org/refs/heads/main/qrcodeprivacy.html") }
                )
                SettingsItem(
                    icon = Icons.Outlined.Description,
                    title = "Terms of Service",
                    subtitle = "Read our terms of service",
                    onClick = { uriHandler.openUri("https://raw.githubusercontent.com/visiosoft/mypaperlessoffice.org/0eec97d4f257e5fe154b01385355f139fd7cbbaf/qr_code_terms.html") }
                )
                SettingsItem(
                    icon = Icons.Outlined.Email,
                    title = "Contact Support",
                    subtitle = "Get help or send feedback",
                    onClick = { settingsViewModel.sendSupportEmail() }
                )
            }
        }
    }

    // Vibration Dialog
    if (showVibrationDialog) {
        AlertDialog(
            onDismissRequest = { showVibrationDialog = false },
            title = { Text("Vibration Settings") },
            text = {
                Column {
                    Text("Vibration intensity")
                    Slider(
                        value = settingsState.vibrationIntensity,
                        onValueChange = { settingsViewModel.updateVibrationIntensity(it) },
                        valueRange = 0f..1f,
                        steps = 4
                    )
                    Text(
                        when (settingsState.vibrationIntensity) {
                            0f -> "Off"
                            in 0.01f..0.33f -> "Low"
                            in 0.34f..0.66f -> "Medium"
                            else -> "High"
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showVibrationDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Sound Dialog
    if (showSoundDialog) {
        val sounds = listOf("Default", "Beep", "Chime", "Success")
        AlertDialog(
            onDismissRequest = { showSoundDialog = false },
            title = { Text("Sound Settings") },
            text = {
                Column {
                    sounds.forEach { sound ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settingsState.selectedSound == sound,
                                onClick = { settingsViewModel.updateSelectedSound(sound) }
                            )
                            Text(sound, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSoundDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Theme Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme") },
            text = {
                Column {
                    ThemeMode.values().forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settingsState.themeMode == theme,
                                onClick = { settingsViewModel.updateThemeMode(theme) }
                            )
                            Text(
                                when (theme) {
                                    ThemeMode.LIGHT -> "Light"
                                    ThemeMode.DARK -> "Dark"
                                    ThemeMode.SYSTEM -> "System default"
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Brightness Dialog
    if (showBrightnessDialog) {
        AlertDialog(
            onDismissRequest = { showBrightnessDialog = false },
            title = { Text("Scanner Brightness") },
            text = {
                Column {
                    Text("Adjust brightness")
                    Slider(
                        value = settingsState.scannerBrightness,
                        onValueChange = { settingsViewModel.updateScannerBrightness(it) },
                        valueRange = 0f..1f
                    )
                    Text(
                        when (settingsState.scannerBrightness) {
                            in 0f..0.33f -> "Low"
                            in 0.34f..0.66f -> "Medium"
                            else -> "High"
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showBrightnessDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(16.dp)
        )
        content()
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?
) {
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (onClick != null) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SwitchSettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: (() -> Unit)? = null
) {
    Surface(
        onClick = { onClick?.invoke() },
        enabled = true,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
} 