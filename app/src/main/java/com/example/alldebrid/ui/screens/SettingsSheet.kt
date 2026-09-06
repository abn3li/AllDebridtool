package com.example.alldebrid.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.alldebrid.data.ThemeMode
import com.example.alldebrid.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class SettingsScreen { MAIN, THEME, ACCOUNT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(viewModel: AppViewModel, onDismiss: () -> Unit) {
    val themeMode by viewModel.themeMode.collectAsState()
    val user by viewModel.user.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    var currentScreen by remember { mutableStateOf(SettingsScreen.MAIN) }

    LaunchedEffect(Unit) {
        viewModel.refreshUserInfo()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 24.dp, end = 24.dp, top = 8.dp),
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    if (targetState != SettingsScreen.MAIN) {
                        slideInHorizontally { it }.togetherWith(slideOutHorizontally { -it })
                    } else {
                        slideInHorizontally { -it }.togetherWith(slideOutHorizontally { it })
                    }
                },
                label = "SettingsNav"
            ) { screen ->
                when (screen) {
                    SettingsScreen.MAIN -> {
                        Column {
                            Text("Settings", style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.height(16.dp))

                            SettingsCategoryRow(
                                title = "Account Information",
                                subtitle = user?.username ?: "Not logged in",
                                icon = Icons.Default.AccountCircle
                            ) {
                                currentScreen = SettingsScreen.ACCOUNT
                            }

                            SettingsCategoryRow(
                                title = "Themes",
                                subtitle = when (themeMode) {
                                    ThemeMode.LIGHT -> "Light"
                                    ThemeMode.DARK -> "Dark"
                                    ThemeMode.SYSTEM -> "Follow system"
                                },
                                icon = Icons.Default.Palette
                            ) {
                                currentScreen = SettingsScreen.THEME
                            }
                        }
                    }

                    SettingsScreen.THEME -> {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { currentScreen = SettingsScreen.MAIN }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                                Text("Themes", style = MaterialTheme.typography.headlineSmall)
                            }
                            Spacer(Modifier.height(16.dp))

                            ThemeOptionRow("Light", ThemeMode.LIGHT, themeMode) { viewModel.setThemeMode(it) }
                            ThemeOptionRow("Dark", ThemeMode.DARK, themeMode) { viewModel.setThemeMode(it) }
                            ThemeOptionRow("Follow system", ThemeMode.SYSTEM, themeMode) { viewModel.setThemeMode(it) }
                        }
                    }

                    SettingsScreen.ACCOUNT -> {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { currentScreen = SettingsScreen.MAIN }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                                Text("Account Information", style = MaterialTheme.typography.headlineSmall)
                            }
                            Spacer(Modifier.height(24.dp))

                            user?.let {
                                AccountInfoItem("Username", it.username ?: "N/A")
                                AccountInfoItem("Email", it.email ?: "N/A")
                                AccountInfoItem(
                                    label = "Status",
                                    value = if (it.isPremium == true) "Premium" else "Free",
                                    valueColor = if (it.isPremium == true) Color(0xFFFACC15) else MaterialTheme.colorScheme.onSurface
                                )
                                if ((it.isPremium == true) && (it.premiumUntil != null)) {
                                    val dateStr = remember(it.premiumUntil) {
                                        val date = Date(it.premiumUntil * 1000L)
                                        SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(date)
                                    }
                                    AccountInfoItem("Premium Until", dateStr)
                                }
                            } ?: run {
                                Text("No account information available", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountInfoItem(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = valueColor)
    }
}

@Composable
private fun SettingsCategoryRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun ThemeOptionRow(
    label: String,
    value: ThemeMode,
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected == value) { onSelect(value) }
            .padding(vertical = 10.dp)
    ) {
        RadioButton(selected = selected == value, onClick = { onSelect(value) })
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}
