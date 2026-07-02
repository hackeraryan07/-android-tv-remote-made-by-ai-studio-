package com.example.tvremote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tvremote.protocol.RemoteKeyCode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteApp(viewModel: TvRemoteViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Android TV Remote") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                is RemoteState.Scanning -> ScanningScreen(viewModel)
                is RemoteState.FoundTvs -> FoundTvsScreen(s, viewModel)
                is RemoteState.Pairing -> PairingScreen("Connecting...")
                is RemoteState.AwaitingCode -> AwaitingCodeScreen(viewModel)
                is RemoteState.Connected -> RemoteControlScreen(s.tvName, viewModel)
                is RemoteState.Error -> ErrorScreen(s.message, viewModel)
            }
        }
    }
}

@Composable
fun ScanningScreen(viewModel: TvRemoteViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text("Scanning for Android TVs...")
        Spacer(modifier = Modifier.height(32.dp))
        ManualIpInput(viewModel)
    }
}

@Composable
fun FoundTvsScreen(state: RemoteState.FoundTvs, viewModel: TvRemoteViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Select a TV to pair:", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (state.tvs.isEmpty()) {
            Text("No TVs found yet. Scanning...")
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.tvs) { tv ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { viewModel.initiatePairing(tv) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(tv.name, style = MaterialTheme.typography.titleMedium)
                            Text(tv.address.hostAddress ?: "", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        ManualIpInput(viewModel)
    }
}

@Composable
fun ManualIpInput(viewModel: TvRemoteViewModel) {
    var ipAddress by remember { mutableStateOf("") }
    Column {
        Text("Or enter IP address manually:")
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = ipAddress,
                onValueChange = { ipAddress = it },
                label = { Text("IP Address") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { viewModel.connectManually(ipAddress) }) {
                Text("Connect")
            }
        }
    }
}

@Composable
fun PairingScreen(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(message)
    }
}

@Composable
fun AwaitingCodeScreen(viewModel: TvRemoteViewModel) {
    var code by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Enter the 6-digit code shown on your TV", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= 6) code = it },
            label = { Text("Code") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.submitPairingCode(code) },
            enabled = code.length == 6
        ) {
            Text("Pair")
        }
    }
}

@Composable
fun ErrorScreen(message: String, viewModel: TvRemoteViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Error", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { viewModel.startScanning() }) {
            Text("Try Again")
        }
    }
}

@Composable
fun RemoteControlScreen(tvName: String, viewModel: TvRemoteViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Connected to: \$tvName", color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(32.dp))

        // Top Row: Power
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            RemoteButton(Icons.Default.PowerSettingsNew, "Power") { viewModel.sendCommand(RemoteKeyCode.KEYCODE_POWER) }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // D-Pad
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            RemoteButton(Icons.Default.KeyboardArrowUp, "Up", Modifier.align(Alignment.TopCenter).padding(8.dp)) { viewModel.sendCommand(RemoteKeyCode.KEYCODE_DPAD_UP) }
            RemoteButton(Icons.Default.KeyboardArrowDown, "Down", Modifier.align(Alignment.BottomCenter).padding(8.dp)) { viewModel.sendCommand(RemoteKeyCode.KEYCODE_DPAD_DOWN) }
            RemoteButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Left", Modifier.align(Alignment.CenterStart).padding(8.dp)) { viewModel.sendCommand(RemoteKeyCode.KEYCODE_DPAD_LEFT) }
            RemoteButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Right", Modifier.align(Alignment.CenterEnd).padding(8.dp)) { viewModel.sendCommand(RemoteKeyCode.KEYCODE_DPAD_RIGHT) }
            
            // Center OK
            Button(
                onClick = { viewModel.sendCommand(RemoteKeyCode.KEYCODE_DPAD_CENTER) },
                shape = CircleShape,
                modifier = Modifier.size(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("OK")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Navigation Row: Back, Home
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RemoteButton(Icons.AutoMirrored.Filled.ArrowBack, "Back") { viewModel.sendCommand(RemoteKeyCode.KEYCODE_BACK) }
            RemoteButton(Icons.Default.Home, "Home") { viewModel.sendCommand(RemoteKeyCode.KEYCODE_HOME) }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Volume Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RemoteButton(Icons.Default.VolumeDown, "Vol Down") { viewModel.sendCommand(RemoteKeyCode.KEYCODE_VOLUME_DOWN) }
            RemoteButton(Icons.Default.VolumeOff, "Mute") { viewModel.sendCommand(RemoteKeyCode.KEYCODE_MUTE) }
            RemoteButton(Icons.Default.VolumeUp, "Vol Up") { viewModel.sendCommand(RemoteKeyCode.KEYCODE_VOLUME_UP) }
        }
    }
}

@Composable
fun RemoteButton(icon: ImageVector, description: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(56.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(32.dp))
    }
}
