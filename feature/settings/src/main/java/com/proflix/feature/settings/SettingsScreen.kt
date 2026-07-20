package com.proflix.feature.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.proflix.provider.domain.ProviderType

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val subtitleEnabled by viewModel.subtitleEnabled.collectAsStateWithLifecycle()
    val selectedProvider by viewModel.selectedProvider.collectAsStateWithLifecycle()
    val domainState by viewModel.domainState.collectAsStateWithLifecycle()

    var showProviderDialog by remember { mutableStateOf(false) }
    var editingDomain by remember { mutableStateOf<ProviderType?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            text = "Provider",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clickable { showProviderDialog = true },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Active Provider",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    Text(
                        text = selectedProvider.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Icon(
                    imageVector = if (showProviderDialog) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Select",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Text(
            text = "Domain Settings",
            style = MaterialTheme.typography.titleSmall,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        DomainItem(
            providerType = ProviderType.ANOBOY,
            displayName = "Anoboy",
            currentDomain = domainState.anoboyDomain,
            defaultDomain = "https://anoboy.pk",
            isSelected = selectedProvider == ProviderType.ANOBOY,
            onEdit = { editingDomain = ProviderType.ANOBOY }
        )

        HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

        DomainItem(
            providerType = ProviderType.SAMEHADAKU,
            displayName = "Samehadaku",
            currentDomain = domainState.samehadakuDomain,
            defaultDomain = "https://v2.samehadaku.how",
            isSelected = selectedProvider == ProviderType.SAMEHADAKU,
            onEdit = { editingDomain = ProviderType.SAMEHADAKU }
        )

        HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

        DomainItem(
            providerType = ProviderType.OPLOVERZ,
            displayName = "Oploverz",
            currentDomain = domainState.oploverzDomain,
            defaultDomain = "https://backapi.oploverz.ac",
            isSelected = selectedProvider == ProviderType.OPLOVERZ,
            onEdit = { editingDomain = ProviderType.OPLOVERZ }
        )

        HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f), modifier = Modifier.padding(top = 16.dp))

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Playback",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        SettingsSwitchItem(
            title = "Dark Theme",
            subtitle = "Enable dark mode",
            checked = isDarkTheme,
            onCheckedChange = { viewModel.setDarkTheme(it) }
        )

        HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

        SettingsSwitchItem(
            title = "Subtitles",
            subtitle = "Enable subtitles by default",
            checked = subtitleEnabled,
            onCheckedChange = { viewModel.setSubtitleEnabled(it) }
        )

        HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f), modifier = Modifier.padding(top = 16.dp))

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "About",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "ProFlix v1.0.0",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Fast, private, modular streaming.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }

    if (showProviderDialog) {
        ProviderSelectionDialog(
            selectedProvider = selectedProvider,
            onProviderSelected = {
                viewModel.selectProvider(it)
                showProviderDialog = false
            },
            onDismiss = { showProviderDialog = false }
        )
    }

    editingDomain?.let { providerType ->
        val currentDomain = when (providerType) {
            ProviderType.ANOBOY -> domainState.anoboyDomain
            ProviderType.SAMEHADAKU -> domainState.samehadakuDomain
            ProviderType.OPLOVERZ -> domainState.oploverzDomain
        }
        DomainEditDialog(
            providerType = providerType,
            currentDomain = currentDomain,
            onConfirm = { newDomain ->
                viewModel.updateDomain(providerType, newDomain)
                editingDomain = null
            },
            onDismiss = { editingDomain = null }
        )
    }
}

@Composable
private fun ProviderSelectionDialog(
    selectedProvider: ProviderType,
    onProviderSelected: (ProviderType) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Provider",
                color = Color.White
            )
        },
        text = {
            Column {
                ProviderType.entries.forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onProviderSelected(type) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = type.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        if (type == selectedProvider) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun DomainItem(
    providerType: ProviderType,
    displayName: String,
    currentDomain: String,
    defaultDomain: String,
    isSelected: Boolean,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = currentDomain,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 1
            )
        }
        Text(
            text = "Edit",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun DomainEditDialog(
    providerType: ProviderType,
    currentDomain: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentDomain) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit ${providerType.displayName} Domain",
                color = Color.White
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter the base URL for this provider:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Domain URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text("Save", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
