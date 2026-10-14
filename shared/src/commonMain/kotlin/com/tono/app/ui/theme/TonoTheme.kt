package com.tono.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.tono.app.data.local.globalSettings
import wang.harlon.appsettings.appearance.AppearanceOptions
import wang.harlon.appsettings.appearance.AppearanceTheme
import wang.harlon.appsettings.appearance.ColorSource
import wang.harlon.appsettings.appearance.rememberAppearanceState
import androidx.compose.ui.graphics.Color

// 色值出自 ~/TonoProjects/docs/theme/theme-design.md：黑白中性色，固定色板，不跟随壁纸
private val LightColors = lightColorScheme(
    primary = Color(0xFF1A1A1A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE8E8E8),
    onPrimaryContainer = Color(0xFF1A1A1A),
    secondary = Color(0xFF1A1A1A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8E8E8),
    onSecondaryContainer = Color(0xFF1A1A1A),
    tertiary = Color(0xFF1A1A1A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE8E8E8),
    onTertiaryContainer = Color(0xFF1A1A1A),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF4F0F4),
    onSurfaceVariant = Color(0xFF79747E),
    surfaceContainerLow = Color(0xFFF7F3F7),
    surfaceContainer = Color(0xFFF0ECF0),
    surfaceContainerHigh = Color(0xFFEAE6EA),
    outline = Color(0xFFCAC4D0),
    outlineVariant = Color(0xFFE7E0EC),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE0E0E0),
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFF3A3A3A),
    onPrimaryContainer = Color(0xFFE0E0E0),
    secondary = Color(0xFFE0E0E0),
    onSecondary = Color(0xFF1A1A1A),
    secondaryContainer = Color(0xFF3A3A3A),
    onSecondaryContainer = Color(0xFFE0E0E0),
    tertiary = Color(0xFFE0E0E0),
    onTertiary = Color(0xFF1A1A1A),
    tertiaryContainer = Color(0xFF3A3A3A),
    onTertiaryContainer = Color(0xFFE0E0E0),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF2C2C30),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceContainerLow = Color(0xFF211F26),
    surfaceContainer = Color(0xFF252329),
    surfaceContainerHigh = Color(0xFF2E2C32),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFF49454F),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF2B8B5),
)

/** Tono 的外观：品牌黑白色写死（theme-design.md），只开放深浅模式；色板、调色台、壁纸取色都不显示。 */
val TonoAppearanceOptions = AppearanceOptions(
    colorSource = ColorSource.Fixed(light = LightColors, dark = DarkColors),
    allowCustom = false,
    allowSystemDynamic = false,
)

/** 挂在根上：深浅模式、纯黑档、切换动画与系统栏都由 appsettings-kmp 管。 */
@Composable
fun TonoTheme(content: @Composable () -> Unit) {
    val appearance = rememberAppearanceState(globalSettings, TonoAppearanceOptions)
    AppearanceTheme(appearance, content = content)
}
