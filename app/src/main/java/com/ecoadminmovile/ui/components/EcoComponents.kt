package com.ecoadminmovile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecoadminmovile.ui.theme.*

@Composable
fun EcoCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = Color(0x0F0F172A)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        content = content
    )
}

@Composable
fun EcoStatusPill(
    status: String,
    modifier: Modifier = Modifier
) {
    val (colors, label) = when (status.uppercase()) {
        "PENDIENTE" -> Triple(EcoPendingBg, EcoPendingText, EcoPendingDot) to "Pendiente"
        "EN_TRANSITO" -> Triple(EcoTransitBg, EcoTransitText, EcoTransitDot) to "En tránsito"
        "ENTREGADO" -> Triple(EcoDeliveredBg, EcoDeliveredText, EcoDeliveredDot) to "Entregado"
        "COMPLETADO" -> Triple(EcoCompletedBg, EcoCompletedText, EcoCompletedDot) to "Completado"
        "CANCELADO" -> Triple(EcoCanceledBg, EcoCanceledText, EcoCanceledDot) to "Cancelado"
        else -> Triple(EcoBg, EcoTextMuted, EcoTextSubtle) to status
    }
    val (bgColor, textColor, dotColor) = colors

    Surface(
        color = bgColor,
        shape = CircleShape,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(dotColor, CircleShape)
            )
            Text(
                text = label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EcoMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    badgeColor: Color = Color(0xFF10B981),
    badgeBgColor: Color = Color(0xFFD1FAE5)
) {
    EcoCard(modifier = modifier) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = iconBgColor,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = EcoTextStrong
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = EcoTextMuted
                )
            }

            if (badgeText != null) {
                Surface(
                    color = badgeBgColor,
                    shape = CircleShape
                ) {
                    Text(
                        text = badgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }
        }
    }
}
