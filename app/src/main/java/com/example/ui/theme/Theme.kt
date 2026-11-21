package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme =
  lightColorScheme(
    primary = EspressoBrown,
    onPrimary = CremaWhite,
    primaryContainer = WarmCream,
    onPrimaryContainer = EspressoBrown,
    secondary = MochaBrown,
    onSecondary = CremaWhite,
    secondaryContainer = SoftCaramel,
    onSecondaryContainer = EspressoBrown,
    tertiary = CaramelGold,
    onTertiary = EspressoBrown,
    tertiaryContainer = AmberGoldContainer,
    onTertiaryContainer = EspressoBrown,
    background = CremaWhite,
    onBackground = EspressoBrown,
    surface = CremaWhite,
    onSurface = EspressoBrown,
    surfaceVariant = SoftLatteSurface,
    onSurfaceVariant = MutedCocoa,
    surfaceContainerLowest = CremaWhite,
    surfaceContainerLow = SoftLatteSurface,
    surfaceContainer = WarmCream,
    surfaceContainerHigh = Color(0xFFEFE1CB),
    surfaceContainerHighest = Color(0xFFE6D4BA),
    outline = WarmBorder,
    outlineVariant = Color(0xFFEBDCCB),
    error = BerryRed,
    onError = CremaWhite,
    errorContainer = BerryRedContainer,
    onErrorContainer = BerryRed,
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = CaramelGold,
    onPrimary = EspressoBrown,
    primaryContainer = RoastedChestnut,
    onPrimaryContainer = WarmCream,
    secondary = SoftCaramel,
    onSecondary = EspressoBrown,
    secondaryContainer = MochaBrown,
    onSecondaryContainer = CremaWhite,
    tertiary = WarmCream,
    onTertiary = EspressoBrown,
    background = DeepRoast,
    onBackground = WarmCream,
    surface = EspressoBrown,
    onSurface = WarmCream,
    surfaceVariant = Color(0xFF33231C),
    onSurfaceVariant = SoftCaramel,
    surfaceContainerLowest = DeepRoast,
    surfaceContainerLow = Color(0xFF1F140F),
    surfaceContainer = EspressoBrown,
    surfaceContainerHigh = Color(0xFF2F201A),
    surfaceContainerHighest = Color(0xFF3B2921),
    outline = Color(0xFF5A4035),
    outlineVariant = Color(0xFF422E25),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = BerryRedContainer,
  )

val BrewHouseShapes =
  Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
  )

@Composable
fun BrewHouseTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = BrewHouseTypography,
    shapes = BrewHouseShapes,
    content = content,
  )
}
