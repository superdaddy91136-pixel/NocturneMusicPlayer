package com.nocturne.music.ui.home

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nocturne.music.designsystem.NocturnePalette
import com.nocturne.music.model.UserSettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userSettings: UserSettings,
    onSaveProfile: (String, String?) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(userSettings.profileName) }
    var pictureUri by remember { mutableStateOf<String?>(userSettings.profilePictureUri) }
    val context = LocalContext.current
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            // Need to take persistable URI permission
            try {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                pictureUri = uri.toString()
            } catch (e: Exception) {
                // Ignore if not supported
                pictureUri = uri.toString()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NocturnePalette.ObsidianSurface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Edit Profile", style = MaterialTheme.typography.titleLarge, color = Color.White)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(NocturnePalette.CharcoalElevated)
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (pictureUri != null) {
                    AsyncImage(
                        model = pictureUri,
                        contentDescription = "Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = NocturnePalette.TextSecondary, modifier = Modifier.size(64.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(onClick = { pictureUri = null }) {
                Text("Remove Picture", color = NocturnePalette.CrimsonRose)
            }

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Display Name") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NocturnePalette.EmeraldGlow,
                    focusedLabelColor = NocturnePalette.EmeraldGlow,
                    unfocusedBorderColor = NocturnePalette.TextSecondary,
                    unfocusedLabelColor = NocturnePalette.TextSecondary,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    onSaveProfile(name.ifBlank { "Nocturne User" }, pictureUri)
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NocturnePalette.EmeraldGlow),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Profile", color = NocturnePalette.AmoledBlack, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onExportBackup: () -> Unit,
    onImportBackup: (Uri) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            onImportBackup(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NocturnePalette.ObsidianSurface)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Backup & Restore", style = MaterialTheme.typography.titleLarge, color = Color.White)
        }
        
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Keep your library safe. Export your playlists, favorites, history, and preferences to a portable backup file.", color = NocturnePalette.TextSecondary, style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NocturnePalette.CharcoalElevated)
                    .clickable { onExportBackup() }
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.FileUpload, contentDescription = null, tint = NocturnePalette.EmeraldGlow, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Export Backup", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text("Save your library data to your device", style = MaterialTheme.typography.bodySmall, color = NocturnePalette.TextSecondary)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NocturnePalette.CharcoalElevated)
                    .clickable { importLauncher.launch("application/json") }
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.FileDownload, contentDescription = null, tint = NocturnePalette.LiquidIndigo, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Import Backup", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text("Restore library data from a backup file", style = MaterialTheme.typography.bodySmall, color = NocturnePalette.TextSecondary)
                    }
                }
            }
        }
    }
}
