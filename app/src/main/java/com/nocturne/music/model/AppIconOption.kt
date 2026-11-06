package com.nocturne.music.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.nocturne.music.R

enum class AppIconOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val activityAliasName: String?,
    val accentColor: Color,
    @DrawableRes val previewDrawableRes: Int
) {
    DEFAULT(
        id = "default",
        title = "Default Nocturne",
        subtitle = "Original Mint & Matte Obsidian",
        activityAliasName = "com.nocturne.music.MainActivityAliasDefault",
        accentColor = Color(0xFF57CF8F),
        previewDrawableRes = R.drawable.ic_launcher_foreground_image
    ),
    VARIANT_1(
        id = "var1",
        title = "Custom Variant 1",
        subtitle = "Supplied Custom Artwork 1",
        activityAliasName = "com.nocturne.music.MainActivityAliasVar1",
        accentColor = Color(0xFF9E9E9E),
        previewDrawableRes = R.drawable.ic_launcher_foreground_var1_image
    ),
    VARIANT_2(
        id = "var2",
        title = "Custom Variant 2",
        subtitle = "Supplied Custom Artwork 2",
        activityAliasName = "com.nocturne.music.MainActivityAliasVar2",
        accentColor = Color(0xFF9E9E9E),
        previewDrawableRes = R.drawable.ic_launcher_foreground_var2_image
    ),
    VARIANT_3(
        id = "var3",
        title = "Custom Variant 3",
        subtitle = "Supplied Custom Artwork 3",
        activityAliasName = "com.nocturne.music.MainActivityAliasVar3",
        accentColor = Color(0xFF9E9E9E),
        previewDrawableRes = R.drawable.ic_launcher_foreground_var3_image
    ),
    VARIANT_4(
        id = "var4",
        title = "Custom Variant 4",
        subtitle = "Supplied Custom Artwork 4",
        activityAliasName = "com.nocturne.music.MainActivityAliasVar4",
        accentColor = Color(0xFF9E9E9E),
        previewDrawableRes = R.drawable.ic_launcher_foreground_var4_image
    ),
    GLOWING_BLUE(
        id = "blue",
        title = "Glowing Blue",
        subtitle = "Glowing Blue Musical Note",
        activityAliasName = "com.nocturne.music.MainActivityAliasBlue",
        accentColor = Color(0xFF007FFF),
        previewDrawableRes = R.drawable.ic_launcher_foreground_blue_image
    ),
    HAUNTED_PUMPKIN(
        id = "pumpkin",
        title = "Haunted Pumpkin",
        subtitle = "Haunted Neon Pumpkin Music Icon",
        activityAliasName = "com.nocturne.music.MainActivityAliasPumpkin",
        accentColor = Color(0xFFFF6D00),
        previewDrawableRes = R.drawable.ic_launcher_foreground_pumpkin_image
    ),
    VARIANT_5(
        id = "var5",
        title = "Custom Variant 5",
        subtitle = "Supplied Custom Artwork 5",
        activityAliasName = "com.nocturne.music.MainActivityAliasVar5",
        accentColor = Color(0xFF9E9E9E),
        previewDrawableRes = R.drawable.ic_launcher_foreground_var5_image
    ),
    VARIANT_6(
        id = "var6",
        title = "Custom Variant 6",
        subtitle = "Supplied Custom Artwork 6",
        activityAliasName = "com.nocturne.music.MainActivityAliasVar6",
        accentColor = Color(0xFF9E9E9E),
        previewDrawableRes = R.drawable.ic_launcher_foreground_var6_image
    ),
    VARIANT_7(
        id = "var7",
        title = "Custom Variant 7",
        subtitle = "Supplied Custom Artwork 7",
        activityAliasName = "com.nocturne.music.MainActivityAliasVar7",
        accentColor = Color(0xFF9E9E9E),
        previewDrawableRes = R.drawable.ic_launcher_foreground_var7_image
    ),
    VARIANT_8(
        id = "var8",
        title = "Custom Variant 8",
        subtitle = "Supplied Custom Artwork 8",
        activityAliasName = "com.nocturne.music.MainActivityAliasVar8",
        accentColor = Color(0xFF9E9E9E),
        previewDrawableRes = R.drawable.ic_launcher_foreground_var8_image
    ),
    VARIANT_9(
        id = "var9",
        title = "Custom Variant 9",
        subtitle = "Supplied Custom Artwork 9",
        activityAliasName = "com.nocturne.music.MainActivityAliasVar9",
        accentColor = Color(0xFF9E9E9E),
        previewDrawableRes = R.drawable.ic_launcher_foreground_var9_image
    ),
    VARIANT_10(
        id = "var10",
        title = "Custom Variant 10",
        subtitle = "Supplied Custom Artwork 10",
        activityAliasName = "com.nocturne.music.MainActivityAliasVar10",
        accentColor = Color(0xFF9E9E9E),
        previewDrawableRes = R.drawable.ic_launcher_foreground_var10_image
    ),
    VARIANT_11(
        id = "var11",
        title = "Custom Variant 11",
        subtitle = "Supplied Custom Artwork 11",
        activityAliasName = "com.nocturne.music.MainActivityAliasVar11",
        accentColor = Color(0xFF9E9E9E),
        previewDrawableRes = R.drawable.ic_launcher_foreground_var11_image
    ),
    VARIANT_12(
        id = "var12",
        title = "Custom Variant 12",
        subtitle = "Supplied Custom Artwork 12",
        activityAliasName = "com.nocturne.music.MainActivityAliasVar12",
        accentColor = Color(0xFF9E9E9E),
        previewDrawableRes = R.drawable.ic_launcher_foreground_var12_image
    ),
    VARIANT_13(
        id = "var13",
        title = "Custom Variant 13",
        subtitle = "Supplied Custom Artwork 13",
        activityAliasName = "com.nocturne.music.MainActivityAliasVar13",
        accentColor = Color(0xFF9E9E9E),
        previewDrawableRes = R.drawable.ic_launcher_foreground_var13_image
    ),
    VARIANT_14(
        id = "var14",
        title = "Custom Variant 14",
        subtitle = "Supplied Custom Artwork 14",
        activityAliasName = "com.nocturne.music.MainActivityAliasVar14",
        accentColor = Color(0xFF9E9E9E),
        previewDrawableRes = R.drawable.ic_launcher_foreground_var14_image
    ),
    VARIANT_15(
        id = "var15",
        title = "Custom Variant 15",
        subtitle = "Supplied Custom Artwork 15",
        activityAliasName = "com.nocturne.music.MainActivityAliasVar15",
        accentColor = Color(0xFF9E9E9E),
        previewDrawableRes = R.drawable.ic_launcher_foreground_var15_image
    );

    companion object {
        fun fromId(id: String?): AppIconOption {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
        }
    }
}

