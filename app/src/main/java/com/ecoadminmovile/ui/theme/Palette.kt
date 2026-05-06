/**
 * Paleta de colores (design tokens) de la aplicacion EcoAdmin.
 *
 * Conceptos Kotlin demostrados:
 * - `val`: referencia inmutable. Una vez asignado, el valor no puede cambiar.
 * - Declaraciones top-level: en Kotlin no es obligatorio envolver todo en una clase.
 *   Estas constantes viven directamente en el archivo y se acceden importandolas.
 * - `Color(0xFF...)`: constructor que recibe un literal hexadecimal UInt.
 *   El prefijo `0xFF` indica canal alfa al 100% (totalmente opaco).
 *
 * Patron de diseno:
 * - **Design Tokens / Color Tokens**: centralizar colores con nombres semanticos
 *   (Primary, Danger, Surface...) para mantener consistencia visual en toda la app.
 */
package com.ecoadminmovile.ui.theme

import androidx.compose.ui.graphics.Color

// Base Colors from Web App
// 0xFF = alfa 255 (opaco) + 6 digitos hex RGB
val EcoPrimary = Color(0xFF3B82F6)
val EcoPrimaryDark = Color(0xFF1D4ED8)
val EcoDanger = Color(0xFFEF4444)
val EcoBg = Color(0xFFF1F5F9)
val EcoSurface = Color(0xFFFFFFFF)
val EcoBorder = Color(0xFFE2E8F0)
val EcoTextStrong = Color(0xFF0F172A)
val EcoText = Color(0xFF1E293B)
val EcoTextMuted = Color(0xFF475569)
val EcoTextSubtle = Color(0xFF94A3B8)

// Status Colors (BG, Text, Dot)
val EcoPendingBg = Color(0xFFFEF3C7)
val EcoPendingText = Color(0xFFB45309)
val EcoPendingDot = Color(0xFFF59E0B)

val EcoTransitBg = Color(0xFFDBEAFE)
val EcoTransitText = Color(0xFF1D4ED8)
val EcoTransitDot = Color(0xFF3B82F6)

val EcoDeliveredBg = Color(0xFFEDE9FE)
val EcoDeliveredText = Color(0xFF6D28D9)
val EcoDeliveredDot = Color(0xFF8B5CF6)

val EcoCompletedBg = Color(0xFFD1FAE5)
val EcoCompletedText = Color(0xFF047857)
val EcoCompletedDot = Color(0xFF10B981)

val EcoCanceledBg = Color(0xFFFEE2E2)
val EcoCanceledText = Color(0xFFB91C1C)
val EcoCanceledDot = Color(0xFFEF4444)

// Legacy compatibility (if needed)
val EcoBlue = EcoPrimary
val EcoSlate = EcoTextStrong
val EcoMint = EcoCompletedDot
val EcoGold = EcoPendingDot

// Metric Card Semantic Tokens
// Colores semanticos para las metricas del Dashboard.
// Cada metrica tiene un par (iconBg, icon) para mantener coherencia visual.
// Si se implementa dark mode, se pueden sobreescribir aqui sin tocar los composables.

// Centros
val EcoMetricCentrosBg = Color(0xFFEBF2FF)
val EcoMetricCentrosIcon = EcoPrimary

// Residuos
val EcoMetricResiduosBg = Color(0xFFFFF7ED)
val EcoMetricResiduosIcon = Color(0xFFF97316)
val EcoMetricResiduosBadge = Color(0xFFC2410C)
val EcoMetricResiduosBadgeBg = Color(0xFFFFEDD5)

// Pendientes
val EcoMetricPendingBg = Color(0xFFFEFCE8)
val EcoMetricPendingIcon = Color(0xFFEAB308)

// En transito
val EcoMetricTransitBg = Color(0xFFEBF2FF)
val EcoMetricTransitIcon = Color(0xFF3B82F6)

// Entregados
val EcoMetricDeliveredBg = Color(0xFFEDE9FE)
val EcoMetricDeliveredIcon = Color(0xFF8B5CF6)

// Completados
val EcoMetricCompletedBg = Color(0xFFECFDF5)
val EcoMetricCompletedIcon = Color(0xFF10B981)
