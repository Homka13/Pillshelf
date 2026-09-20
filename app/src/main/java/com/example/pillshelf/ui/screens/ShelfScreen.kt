package com.example.pillshelf.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pillshelf.R
import com.example.pillshelf.data.model.Medication
import com.example.pillshelf.ui.components.MedicationCard
import com.example.pillshelf.ui.theme.StatusError
import com.example.pillshelf.ui.theme.StatusWarning
import com.example.pillshelf.ui.viewmodel.PillshelfViewModel

@Composable
fun ShelfScreen(
    viewModel: PillshelfViewModel,
    onMedicationClick: (Medication) -> Unit,
    onAddMedicationClick: () -> Unit,
    onRestockClick: (Medication) -> Unit,
    modifier: Modifier = Modifier
) {
    val medications by viewModel.filteredMedications.collectAsStateWithLifecycle()
    val stats by viewModel.shelfStats.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val lowStockOnly by viewModel.filterLowStockOnly.collectAsStateWithLifecycle()
    val expiringOnly by viewModel.filterExpiringOnly.collectAsStateWithLifecycle()
    val interactions by viewModel.interactionWarnings.collectAsStateWithLifecycle()

    val categories = listOf("Всі", "Аптечка", "Знеболювальні", "Вітаміни", "Травлення", "Рецептурні", "Протизастудні")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddMedicationClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_medication_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Додати ліки")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Hero Banner & Title
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_pillshelf_banner),
                        contentDescription = "Аптечка банер",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    ) {}

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Домашня аптечка",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Pillshelf • Облік та контроль ліків",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            IconButton(
                                onClick = { viewModel.sendTestReminder() },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .testTag("test_reminder_button")
                            ) {
                                Icon(
                                    Icons.Outlined.Notifications,
                                    contentDescription = "Тестове нагадування",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Statistics Row Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatMiniCard(
                                title = "Всього",
                                count = stats.totalCount.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            StatMiniCard(
                                title = "Закінчуються",
                                count = stats.lowStockCount.toString(),
                                isWarning = stats.lowStockCount > 0,
                                modifier = Modifier.weight(1.1f)
                            )
                            StatMiniCard(
                                title = "Термін",
                                count = stats.expiringCount.toString(),
                                isWarning = stats.expiringCount > 0,
                                modifier = Modifier.weight(1f)
                            )
                            if (stats.expiredCount > 0) {
                                StatMiniCard(
                                    title = "Прострочено",
                                    count = stats.expiredCount.toString(),
                                    isError = true,
                                    modifier = Modifier.weight(1.2f)
                                )
                            }
                        }
                    }
                }
            }

            // Drug Interaction Warnings Banner (if any)
            if (interactions.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        interactions.forEach { warning ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = StatusError.copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = StatusError,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = warning.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusError
                                        )
                                        Text(
                                            text = warning.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar & Filter Chips
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Пошук за назвою, діючою речовиною...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Очистити")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shelf_search_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Filter Scroll
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { viewModel.setSelectedCategory(cat) },
                                label = { Text(cat) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick Alert Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = lowStockOnly,
                            onClick = { viewModel.toggleFilterLowStock() },
                            label = { Text("Закінчується запас (<= 5)") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )

                        FilterChip(
                            selected = expiringOnly,
                            onClick = { viewModel.toggleFilterExpiring() },
                            label = { Text("Спливає термін (< 30 дн)") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }

            // Medication Cards List
            if (medications.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, bottom = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Medication,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = if (searchQuery.isNotEmpty() || lowStockOnly || expiringOnly)
                                    "Нічого не знайдено за фільтрами"
                                else
                                    "Ваша аптечка порожня",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (searchQuery.isNotEmpty() || lowStockOnly || expiringOnly) {
                                Button(onClick = { viewModel.clearFilters() }) {
                                    Text("Скинути фільтри")
                                }
                            } else {
                                Button(onClick = onAddMedicationClick) {
                                    Text("Додати перші ліки")
                                }
                            }
                        }
                    }
                }
            } else {
                items(
                    items = medications,
                    key = { it.id }
                ) { medication ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        MedicationCard(
                            medication = medication,
                            currentEpochDay = viewModel.currentEpochDay,
                            onClick = { onMedicationClick(medication) },
                            onTakeDose = { viewModel.recordIntake(medication, taken = true) },
                            onRestock = { onRestockClick(medication) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMiniCard(
    title: String,
    count: String,
    modifier: Modifier = Modifier,
    isWarning: Boolean = false,
    isError: Boolean = false
) {
    val bgColor = when {
        isError -> StatusError.copy(alpha = 0.18f)
        isWarning -> StatusWarning.copy(alpha = 0.18f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }

    val textColor = when {
        isError -> StatusError
        isWarning -> StatusWarning
        else -> MaterialTheme.colorScheme.onSurface
    }

    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}
