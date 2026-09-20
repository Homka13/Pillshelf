package com.example.pillshelf.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.outlined.PriceChange
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pillshelf.ui.viewmodel.PillshelfViewModel
import com.example.pillshelf.ui.viewmodel.TrackedMedicationPrice
import java.net.URLEncoder

/**
 * Екран цін — ЧЕСНИЙ.
 *
 * Джерело даних: tabletki.ua. Станом на вересень 2026 їх API віддає
 * HTTP 403 для не-браузерних клієнтів, тому програмне порівняння цін
 * недоступне. Застосунок НІКОЛИ не показує вигадані ціни: якщо даних
 * немає — екран прямо каже «ціни недоступні» і пропонує відкрити
 * пошук на Tabletki.ua (де ціни бачить сама людина, у браузері).
 */
@Composable
fun PricesScreen(
    viewModel: PillshelfViewModel,
    onAddMedicationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trackedPrices by viewModel.trackedPrices.collectAsStateWithLifecycle()
    val priceMonitoringEnabled by viewModel.priceMonitoringEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("prices_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "Ціни в аптеках",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Джерело: Tabletki.ua (перехід у браузер)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Honest explanation card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("prices_honesty_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Автоматичне порівняння цін зараз недоступне",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Сервіс Tabletki.ua блокує запити з застосунків (Cloudflare, 403), " +
                            "тож застосунок не може сам отримати ціни й не показуватиме вигадані. " +
                            "Перевіряти наявність і ціни можна на сайті — кнопка під кожним препаратом.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Monitoring toggle: за замовчуванням вимкнений
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Фонова перевірка цін",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Вимкнено за замовчуванням. Якщо увімкнути, назви ваших препаратів " +
                                "надсилатимуться на tabletki.ua двічі на день.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = priceMonitoringEnabled,
                        onCheckedChange = { viewModel.setPriceMonitoring(it) },
                        modifier = Modifier.testTag("price_monitoring_switch")
                    )
                }
            }
        }

        // Tracked medications: негайний перехід на Tabletki.ua
        if (trackedPrices.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Outlined.PriceChange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "Немає препаратів з відстеженням",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Увімкніть 'Відстежувати ціни' в картці препарату, щоб мати швидкий перехід на Tabletki.ua",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Button(onClick = onAddMedicationClick) {
                            Text("Додати препарат")
                        }
                    }
                }
            }
        } else {
            items(
                items = trackedPrices,
                key = { it.medication.id }
            ) { item ->
                TrackedMedicationCard(
                    item = item,
                    onOpenTabletki = {
                        val query = URLEncoder.encode(item.medication.name, "UTF-8")
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://tabletki.ua/uk/search/?q=$query"))
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
private fun TrackedMedicationCard(
    item: TrackedMedicationPrice,
    onOpenTabletki: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.medication.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.medication.dosageForm,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Ціни недоступні з застосунку — перевірте на сайті",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("prices_unavailable_${item.medication.id}")
            )

            Spacer(modifier = Modifier.height(12.dp))

            androidx.compose.material3.FilledTonalButton(
                onClick = onOpenTabletki,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Відкрити в tabletki.ua")
            }
        }
    }
}
