package app.teslamate.client.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF9B1B30),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9DE),
    onPrimaryContainer = Color(0xFF3F0012),
    secondary = Color(0xFF3E5C76),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5E4F4),
    background = Color(0xFFF7F4F4),
    surface = Color(0xFFFFFBFA),
    surfaceVariant = Color(0xFFF0E6E6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB3BC),
    onPrimary = Color(0xFF680016),
    primaryContainer = Color(0xFF8E1328),
    onPrimaryContainer = Color(0xFFFFD9DE),
    secondary = Color(0xFFA9C7E3),
    onSecondary = Color(0xFF0B3448),
    secondaryContainer = Color(0xFF254556),
    background = Color(0xFF121416),
    surface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFF2A2426),
)

@Composable
fun TeslaMateTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
