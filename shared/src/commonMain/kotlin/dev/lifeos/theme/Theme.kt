package dev.lifeos.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.lifeos.resources.Res
import dev.lifeos.resources.roboto_flex
import org.jetbrains.compose.resources.Font

/**
 * Esquema de color del sistema: Material You en Android y Caelestia
 * (colores del fondo de pantalla) en el escritorio. No hay paleta fija.
 */
@Composable
expect fun platformColorScheme(): ColorScheme

@Composable
private fun robotoFlex(): FontFamily = FontFamily(
    listOf(400, 500, 600, 700).map { w ->
        Font(
            Res.font.roboto_flex,
            weight = FontWeight(w),
            variationSettings = FontVariation.Settings(FontVariation.weight(w)),
        )
    },
)

@Composable
private fun typography(): Typography {
    val f = robotoFlex()
    val b = Typography()
    return Typography(
        displayLarge = b.displayLarge.copy(fontFamily = f),
        displayMedium = b.displayMedium.copy(fontFamily = f),
        displaySmall = b.displaySmall.copy(fontFamily = f),
        headlineLarge = b.headlineLarge.copy(fontFamily = f, fontWeight = FontWeight.SemiBold),
        headlineMedium = b.headlineMedium.copy(fontFamily = f, fontWeight = FontWeight.SemiBold),
        headlineSmall = b.headlineSmall.copy(fontFamily = f, fontWeight = FontWeight.SemiBold),
        titleLarge = b.titleLarge.copy(fontFamily = f, fontWeight = FontWeight.SemiBold),
        titleMedium = b.titleMedium.copy(fontFamily = f, fontWeight = FontWeight.SemiBold),
        titleSmall = b.titleSmall.copy(fontFamily = f, fontWeight = FontWeight.SemiBold),
        bodyLarge = b.bodyLarge.copy(fontFamily = f),
        bodyMedium = b.bodyMedium.copy(fontFamily = f),
        bodySmall = b.bodySmall.copy(fontFamily = f),
        labelLarge = b.labelLarge.copy(fontFamily = f),
        labelMedium = b.labelMedium.copy(fontFamily = f),
        labelSmall = b.labelSmall.copy(fontFamily = f),
    )
}

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun LifeOsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = platformColorScheme(),
        typography = typography(),
        shapes = shapes,
        content = content,
    )
}
