package com.nocturne.music.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nocturne.music.designsystem.NocturnePalette
import com.nocturne.music.designsystem.matteSurface
import com.nocturne.music.model.AppIconOption

@Composable
fun AppIconSelectionScreen(
    currentIconId: String,
    onSelectIcon: (AppIconOption) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOption = AppIconOption.fromId(currentIconId)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NocturnePalette.ObsidianSurface)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = NocturnePalette.TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "App Icon",
                    style = MaterialTheme.typography.titleLarge,
                    color = NocturnePalette.TextPrimary
                )
                Text(
                    text = "Select home screen & app drawer launcher icon",
                    style = MaterialTheme.typography.bodySmall,
                    color = NocturnePalette.TextMuted
                )
            }
        }

        // Active Icon Preview Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .matteSurface(RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(NocturnePalette.AmoledBlack)
                        .border(
                            width = 1.5.dp,
                            color = currentOption.accentColor,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = currentOption.previewDrawableRes),
                        contentDescription = currentOption.title,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Active Icon",
                            style = MaterialTheme.typography.labelSmall,
                            color = currentOption.accentColor
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentOption.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = NocturnePalette.TextPrimary
                    )
                    Text(
                        text = currentOption.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = NocturnePalette.TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "AVAILABLE LAUNCHER ICONS",
            style = MaterialTheme.typography.labelSmall,
            color = NocturnePalette.TextMuted,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )

        // Grid of icons
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(AppIconOption.entries.toList(), key = { it.id }) { iconOption ->
                val isSelected = iconOption == currentOption

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) {
                                NocturnePalette.CharcoalElevated
                            } else {
                                NocturnePalette.ObsidianSurface
                            }
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) iconOption.accentColor else NocturnePalette.GlassBorderHairline,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { onSelectIcon(iconOption) }
                        .padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            iconOption.accentColor.copy(alpha = 0.2f),
                                            NocturnePalette.AmoledBlack
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    color = iconOption.accentColor.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(18.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = iconOption.previewDrawableRes),
                                contentDescription = iconOption.title,
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = iconOption.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isSelected) iconOption.accentColor else NocturnePalette.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = iconOption.subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = NocturnePalette.TextMuted,
                            maxLines = 2
                        )
                    }

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(NocturnePalette.AmoledBlack),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Selected",
                                tint = iconOption.accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
