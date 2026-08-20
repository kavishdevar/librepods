package me.kavishdevar.librepods.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Minimal first-build screen; protocol controls are added after transport stabilizes. */
@Composable
fun AirPodsHomeScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("LibrePods Wear", style = MaterialTheme.typography.titleLarge)
        Text("Autonomous AirPods controller")
        Text("Bluetooth core ready for integration")
        Button(onClick = { /* Controller wiring follows transport stabilization. */ }) {
            Text("Connect")
        }
    }
}
