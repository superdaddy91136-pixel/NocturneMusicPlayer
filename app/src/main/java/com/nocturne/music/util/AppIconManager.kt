package com.nocturne.music.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.nocturne.music.model.AppIconOption

object AppIconManager {

    fun setAppIcon(context: Context, iconOption: AppIconOption) {
        try {
            val pm = context.packageManager
            val packageName = context.packageName

            val allAliases = AppIconOption.entries.mapNotNull { it.activityAliasName }
            val targetAlias = iconOption.activityAliasName ?: return

            // Enable target alias
            val targetComp = ComponentName(packageName, targetAlias)
            pm.setComponentEnabledSetting(
                targetComp,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            // Disable all other aliases
            for (alias in allAliases) {
                if (alias != targetAlias) {
                    val comp = ComponentName(packageName, alias)
                    pm.setComponentEnabledSetting(
                        comp,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            }
        } catch (_: Exception) {
            // Fail gracefully if component setting is not supported by device launcher
        }
    }
}
