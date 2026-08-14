package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Primary Brand Palette - Foryou Electric Neon & Obsidian
val ForyouViolet = Color(0xFF8B5CF6)
val ForyouVioletDark = Color(0xFF6D28D9)
val ForyouVioletLight = Color(0xFFA78BFA)

val ForyouPink = Color(0xFFEC4899)
val ForyouPinkDark = Color(0xFFBE185D)
val ForyouPinkLight = Color(0xFFF472B6)

val ForyouCyan = Color(0xFF06B6D4)
val ForyouCyanLight = Color(0xFF22D3EE)

val ForyouAmber = Color(0xFFF59E0B)
val ForyouEmerald = Color(0xFF10B981)
val ForyouRose = Color(0xFFF43F5E)

// Dark Theme Surfaces (Obsidian Immersive Glass)
val DarkBackground = Color(0xFF0A0A0A)
val DarkSurface = Color(0xFF141417)
val DarkSurfaceVariant = Color(0xFF1C1C21)
val DarkSurfaceElevated = Color(0xFF24242C)
val DarkBorder = Color(0xFF26262C)
val DarkTextPrimary = Color(0xFFF4F4F6)
val DarkTextSecondary = Color(0xFFA1A1AA)
val DarkTextTertiary = Color(0xFF71717A)

// Light Theme Surfaces (Crisp Minimalist)
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightSurfaceElevated = Color(0xFFE2E8F0)
val LightBorder = Color(0xFFE2E8F0)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)
val LightTextTertiary = Color(0xFF94A3B8)

val ForyouGradientStart = Color(0xFF8B5CF6)
val ForyouGradientEnd = Color(0xFFEC4899)

// Gradients
val ForyouStoryGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFF007A),
        Color(0xFF7928CA),
        Color(0xFF00DFD8)
    )
)

val ForyouBrandGradient = Brush.horizontalGradient(
    colors = listOf(
        ForyouPink,
        ForyouViolet,
        ForyouCyan
    )
)

val ForyouButtonGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF8B5CF6),
        Color(0xFFEC4899)
    )
)
