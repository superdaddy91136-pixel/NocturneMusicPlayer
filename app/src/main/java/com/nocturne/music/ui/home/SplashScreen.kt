package com.nocturne.music.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nocturne.music.R
import com.nocturne.music.designsystem.NocturnePalette

@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash_anim")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_anim"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_anim"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NocturnePalette.AmoledBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_music_note),
                contentDescription = "Nocturne Logo",
                colorFilter = ColorFilter.tint(NocturnePalette.EmeraldGlow),
                modifier = Modifier
                    .size(96.dp)
                    .scale(scale)
                    .alpha(alpha)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "NOCTURNE",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                modifier = Modifier.alpha(alpha)
            )
        }
    }
}
