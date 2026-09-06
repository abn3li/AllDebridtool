package com.example.alldebrid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.alldebrid.data.ThemeMode
import com.example.alldebrid.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(viewModel: AppViewModel, onDismiss: () -> Unit) {
    val themeMode by viewModel.themeMode.collectAsState()
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Text("Settings", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(20.dp))
            Text("Appearance", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))

            ThemeOptionRow("Light", ThemeMode.LIGHT, themeMode) { viewModel.setThemeMode(it) }
            ThemeOptionRow("Dark", ThemeMode.DARK, themeMode) { viewModel.setThemeMode(it) }
            ThemeOptionRow("Follow system", ThemeMode.SYSTEM, themeMode) { viewModel.setThemeMode(it) }

            Spacer(Modifier.height(12.dp))
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
