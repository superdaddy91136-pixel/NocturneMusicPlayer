package com.nocturne.music.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nocturne.music.designsystem.NocturnePalette
import kotlinx.coroutines.delay

@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    buttonSize: Dp = 48.dp,
    unfavoriteColor: Color = NocturnePalette.TextSecondary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    var popScale by remember { mutableFloatStateOf(1f) }
    
    LaunchedEffect(isFavorite) {
        if (isFavorite) {
            popScale = 1.25f
            delay(100)
            popScale = 1f
        }
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else popScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "favoriteScale"
    )

    val tint by animateColorAsState(
        targetValue = if (isFavorite) NocturnePalette.CrimsonRose else unfavoriteColor,
        animationSpec = tween(250),
        label = "favoriteTint"
    )

    IconButton(
        onClick = onClick,
        modifier = modifier.size(buttonSize),
        interactionSource = interactionSource
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
            tint = tint,
            modifier = Modifier
                .size(iconSize)
                .scale(animatedScale) 
        )
    }
}
