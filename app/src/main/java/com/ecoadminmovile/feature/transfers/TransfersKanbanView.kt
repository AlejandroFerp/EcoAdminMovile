package com.ecoadminmovile.feature.transfers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ecoadminmovile.core.model.TrasladoDto
import com.ecoadminmovile.ui.components.EcoCard
import com.ecoadminmovile.ui.components.EcoStatusPill
import com.ecoadminmovile.ui.theme.EcoTextMuted
import com.ecoadminmovile.ui.theme.EcoTextStrong
import com.ecoadminmovile.ui.theme.EcoTextSubtle
import kotlinx.coroutines.launch

private data class KanbanColumn(
    val estado: String,
    val label: String,
    val color: Color,
    val bgColor: Color
)

private val KANBAN_COLUMNS = listOf(
    KanbanColumn("PENDIENTE", "Pendiente", Color(0xFFF59E0B), Color(0xFFFEF3C7)),
    KanbanColumn("EN_TRANSITO", "En tránsito", Color(0xFF3B82F6), Color(0xFFDBEAFE)),
    KanbanColumn("ENTREGADO", "Entregado", Color(0xFF8B5CF6), Color(0xFFEDE9FE)),
    KanbanColumn("COMPLETADO", "Completado", Color(0xFF10B981), Color(0xFFD1FAE5))
)

@Composable
fun TransfersKanbanView(
    transfers: List<TrasladoDto>,
    onTransferSelected: (Long) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { KANBAN_COLUMNS.size })
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        // Column tabs
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            edgePadding = 8.dp,
            divider = {}
        ) {
            KANBAN_COLUMNS.forEachIndexed { index, column ->
                val count = transfers.count { it.estado.equals(column.estado, ignoreCase = true) }
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = column.color,
                                modifier = Modifier.size(8.dp)
                            ) {}
                            Text(column.label, style = MaterialTheme.typography.labelMedium)
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = column.bgColor
                            ) {
                                Text(
                                    text = "$count",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = column.color,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                )
            }
        }

        // Kanban pages
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val column = KANBAN_COLUMNS[page]
            val columnTransfers = transfers.filter {
                it.estado.equals(column.estado, ignoreCase = true)
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(column.bgColor.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                if (columnTransfers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Sin traslados en ${column.label.lowercase()}",
                                color = EcoTextSubtle,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                items(columnTransfers, key = { it.id }) { transfer ->
                    KanbanCard(
                        transfer = transfer,
                        accentColor = column.color,
                        onClick = { onTransferSelected(transfer.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun KanbanCard(
    transfer: TrasladoDto,
    accentColor: Color,
    onClick: () -> Unit
) {
    EcoCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = transfer.codigo.ifBlank { "#${transfer.id}" },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Text(
                    text = transfer.fechaCreacion?.take(10).orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = EcoTextSubtle
                )
            }

            Text(
                text = transfer.residuo?.let {
                    "${it.codigoLER.orEmpty()} ${it.descripcion.orEmpty()}".trim()
                } ?: "Sin residuo",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = EcoTextStrong,
                maxLines = 1
            )

            Text(
                text = "${transfer.centroProductor?.nombre.orEmpty()} → ${transfer.centroGestor?.nombre.orEmpty()}",
                style = MaterialTheme.typography.bodySmall,
                color = EcoTextMuted,
                maxLines = 1
            )
        }
    }
}
