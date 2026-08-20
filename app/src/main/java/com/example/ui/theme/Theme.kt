package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.model.PreparationMode

private val AcademicColorScheme = lightColorScheme(
    primary = AcademicPrimary,
    onPrimary = Color.White,
    primaryContainer = AcademicCardContainer,
    onPrimaryContainer = AcademicPrimaryVariant,
    secondary = AcademicSecondary,
    onSecondary = Color.White,
    background = AcademicBackground,
    onBackground = TextPrimary,
    surface = AcademicSurface,
    onSurface = TextPrimary,
    surfaceVariant = AcademicCardContainer,
    onSurfaceVariant = TextSecondary,
    outline = AcademicCardBorder
)

private val GateColorScheme = lightColorScheme(
    primary = GatePrimary,
    onPrimary = Color.White,
    primaryContainer = GateCardContainer,
    onPrimaryContainer = GatePrimaryVariant,
    secondary = GateSecondary,
    onSecondary = Color.White,
    background = GateBackground,
    onBackground = TextPrimary,
    surface = GateSurface,
    onSurface = TextPrimary,
    surfaceVariant = GateCardContainer,
    onSurfaceVariant = TextSecondary,
    outline = GateCardBorder
)

@Composable
fun AppTheme(
    mode: PreparationMode = PreparationMode.ACADEMIC,
    content: @Composable () -> Unit
) {
    val colorScheme = when (mode) {
        PreparationMode.PROFESSIONAL_GATE -> GateColorScheme
        else -> AcademicColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    AppTheme(mode = PreparationMode.ACADEMIC, content = content)
}
