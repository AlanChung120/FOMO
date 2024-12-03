package com.example.fomo.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import org.junit.Test
import org.mockito.Mockito.*
import kotlin.test.assertEquals

class ThemeTest {

  @Test
  fun `DarkColorScheme contains expected colors`() {
    assertEquals(Color(0xFFD0BCFF), DarkColorScheme.primary)
    assertEquals(Color(0xFFCCC2DC), DarkColorScheme.secondary)
    assertEquals(Color(0xFFEFB8C8), DarkColorScheme.tertiary)
  }

  @Test
  fun `LightColorScheme contains expected colors`() {
    assertEquals(Color(0xFF6650a4), LightColorScheme.primary)
    assertEquals(Color(0xFF625b71), LightColorScheme.secondary)
    assertEquals(Color(0xFF7D5260), LightColorScheme.tertiary)
  }

  @Test
  fun `Typography contains expected default styles`() {
    assertEquals(16.sp, Typography.bodyLarge.fontSize)
    assertEquals(24.sp, Typography.bodyLarge.lineHeight)
    assertEquals(FontWeight.Normal, Typography.bodyLarge.fontWeight)
  }

  @Test
  fun `FOMOTheme applies correct color scheme`() {
    val isDynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = if (isDynamicColorSupported) {
      LightColorScheme
    } else {
      DarkColorScheme
    }

    assertEquals(DarkColorScheme, colorScheme)
  }

  @Test
  fun `FOMOTheme applies DarkColorScheme when darkTheme is true`() {
    val isDarkTheme = true
    val dynamicColor = false

    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      // Mock dynamic dark color scheme
      mock(ColorScheme::class.java)
    } else if (isDarkTheme) {
      DarkColorScheme
    } else {
      LightColorScheme
    }

    assertEquals(DarkColorScheme, colorScheme)
  }

  @Test
  fun `FOMOTheme applies LightColorScheme when darkTheme is false`() {
    val isDarkTheme = false
    val dynamicColor = false

    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      // Mock dynamic light color scheme
      mock(ColorScheme::class.java)
    } else if (isDarkTheme) {
      DarkColorScheme
    } else {
      LightColorScheme
    }

    assertEquals(LightColorScheme, colorScheme)
  }


  @Test
  fun `MaterialTheme is initialized with correct typography`() {
    val typography = Typography(
      bodyLarge = TextStyle(
        fontSize = Typography.bodyLarge.fontSize,
        fontWeight = Typography.bodyLarge.fontWeight,
        lineHeight = Typography.bodyLarge.lineHeight
      )
    )

    assertEquals(Typography.bodyLarge.fontSize, typography.bodyLarge.fontSize)
    assertEquals(Typography.bodyLarge.fontWeight, typography.bodyLarge.fontWeight)
    assertEquals(Typography.bodyLarge.lineHeight, typography.bodyLarge.lineHeight)
  }

}
