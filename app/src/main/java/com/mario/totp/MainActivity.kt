package com.mario.totp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mario.totp.ui.theme.TOTPTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TOTPTheme {
                TotpApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TotpApp(viewModel: MainViewModel = viewModel()) {
    val entries by viewModel.entries.collectAsState()
    val secondsRemaining by viewModel.currentTime.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showFetchDialog by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TOTP Authenticator") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                actions = {
                    if (syncStatus != null) {
                        Text(
                            text = syncStatus!!,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (showFabMenu) {
                    ExtendedFloatingActionButton(
                        onClick = { 
                            showSyncDialog = true
                            showFabMenu = false
                        },
                        icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                        text = { Text("Sync (Push/Pull)") },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    ExtendedFloatingActionButton(
                        onClick = { 
                            showFetchDialog = true
                            showFabMenu = false
                        },
                        icon = { Icon(Icons.Default.List, contentDescription = null) },
                        text = { Text("Fetch from JSON") },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    ExtendedFloatingActionButton(
                        onClick = { 
                            showAddDialog = true
                            showFabMenu = false
                        },
                        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        text = { Text("Add Manually") },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                FloatingActionButton(onClick = { showFabMenu = !showFabMenu }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            LinearProgressIndicator(
                progress = { secondsRemaining / 30f },
                modifier = Modifier.fillMaxWidth(),
            )
            
            if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No accounts added yet", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(entries) { entry ->
                        TotpCard(entry, secondsRemaining)
                    }
                }
            }
        }

        if (showAddDialog) {
            AddManualDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, secret ->
                    viewModel.addEntry(name, secret)
                    showAddDialog = false
                }
            )
        }

        if (showFetchDialog) {
            FetchJsonDialog(
                onDismiss = { showFetchDialog = false },
                onFetch = { url ->
                    viewModel.fetchFromUrl(url)
                    showFetchDialog = false
                }
            )
        }

        if (showSyncDialog) {
            SyncDialog(
                onDismiss = { showSyncDialog = false },
                onSync = { url ->
                    viewModel.syncWithUrl(url)
                    showSyncDialog = false
                }
            )
        }
    }
}

@Composable
fun TotpCard(entry: TotpEntry, secondsRemaining: Int) {
    val code = remember(entry.secret, secondsRemaining / 30) {
        TotpGenerator.generateTotp(entry.secret)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "${code.take(3)} ${code.takeLast(3)}",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "$secondsRemaining",
                style = MaterialTheme.typography.labelLarge,
                color = if (secondsRemaining < 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun AddManualDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Account Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = secret, onValueChange = { secret = it }, label = { Text("Secret Key") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank() && secret.isNotBlank()) onAdd(name, secret) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun FetchJsonDialog(onDismiss: () -> Unit, onFetch: (String) -> Unit) {
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fetch from URL") },
        text = {
            Column {
                Text("Enter URL to JSON (e.g. { \"Key\": \"Secret\" })", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("JSON API URL") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (url.isNotBlank()) onFetch(url) }) {
                Text("Fetch")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SyncDialog(onDismiss: () -> Unit, onSync: (String) -> Unit) {
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sync (Two-Way)") },
        text = {
            Column {
                Text("Downloads from server, combines with local, and pushes the total list back.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Sync API URL") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (url.isNotBlank()) onSync(url) }) {
                Text("Sync Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
