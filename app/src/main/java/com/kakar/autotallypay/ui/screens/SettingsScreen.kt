package com.kakar.autotallypay.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kakar.autotallypay.BuildConfig
import com.kakar.autotallypay.ui.AppLanguage
import com.kakar.autotallypay.ui.BackupResult
import com.kakar.autotallypay.ui.MainViewModel
import com.kakar.autotallypay.ui.components.BigButton

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val strings by vm.strings.collectAsState()
    val language by vm.language.collectAsState()
    val mappings by vm.mappings.collectAsState()
    val customers by vm.customers.collectAsState()
    var password by remember { mutableStateOf("") }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            vm.exportBackup(password, uri) { result ->
                backupMessage = when (result) {
                    BackupResult.SUCCESS -> strings.backupDone
                    else -> strings.backupFailed
                }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            vm.importBackup(password, uri) { result ->
                backupMessage = when (result) {
                    BackupResult.SUCCESS -> strings.importDone
                    BackupResult.WRONG_PASSWORD -> strings.wrongPassword
                    BackupResult.FAILED -> strings.importFailed
                }
            }
        }
    }

    val isLite = BuildConfig.FLAVOR == "lite"

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                strings.settings,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(strings.offlineNote, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Text(
                "${strings.version}: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        if (isLite) strings.liteMode else strings.fullMode,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isLite) strings.liteModeHint else strings.fullModeHint,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        item {
            Text(strings.language, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (language == AppLanguage.ENGLISH) {
                    Button(
                        onClick = {},
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) { Text("English", style = MaterialTheme.typography.titleMedium) }
                } else {
                    OutlinedButton(
                        onClick = { vm.setLanguage(AppLanguage.ENGLISH) },
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) { Text("English", style = MaterialTheme.typography.titleMedium) }
                }
                if (language == AppLanguage.URDU) {
                    Button(
                        onClick = {},
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) { Text("اردو", style = MaterialTheme.typography.titleMedium) }
                } else {
                    OutlinedButton(
                        onClick = { vm.setLanguage(AppLanguage.URDU) },
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) { Text("اردو", style = MaterialTheme.typography.titleMedium) }
                }
            }
        }

        item {
            Text(strings.nameMemory, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(strings.nameMemoryHint, style = MaterialTheme.typography.bodyLarge)
        }
        if (mappings.isEmpty()) {
            item { Text(strings.noMappingsYet, style = MaterialTheme.typography.bodyLarge) }
        } else {
            items(mappings) { mapping ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                mapping.label.ifBlank { mapping.senderKey },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                customers.firstOrNull { it.id == mapping.customerId }?.name ?: "",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                mapping.senderKey,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                        IconButton(onClick = { vm.deleteMapping(mapping) }) {
                            Icon(Icons.Filled.Delete, contentDescription = strings.delete)
                        }
                    }
                }
            }
        }

        item {
            Text(strings.backup, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(strings.backupHint, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; backupMessage = null },
                label = { Text(strings.backupPassword) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            BigButton(
                strings.exportBackup,
                enabled = password.length >= 4
            ) {
                exportLauncher.launch("autotally-backup.atb")
            }
            Spacer(modifier = Modifier.height(12.dp))
            BigButton(
                strings.importBackup,
                enabled = password.length >= 4
            ) {
                pendingImport = true
                importLauncher.launch(arrayOf("*/*"))
            }
            backupMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}
