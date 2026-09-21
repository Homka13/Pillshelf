package com.example.pillshelf.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.PriceChange
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pillshelf.R
import com.example.pillshelf.data.model.Medication
import com.example.pillshelf.ui.components.AddEditMedicationSheet
import com.example.pillshelf.ui.components.MedicationDetailSheet
import com.example.pillshelf.ui.components.RestockDialog
import com.example.pillshelf.ui.components.SettingsSheet
import com.example.pillshelf.ui.screens.HistoryScreen
import com.example.pillshelf.ui.screens.PricesScreen
import com.example.pillshelf.ui.screens.ScheduleScreen
import com.example.pillshelf.ui.screens.ShelfScreen
import com.example.pillshelf.ui.viewmodel.PillshelfViewModel
import kotlinx.coroutines.launch

enum class AppTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    SHELF("Аптечка", Icons.Filled.Inventory2, Icons.Outlined.Inventory2, "tab_shelf"),
    SCHEDULE("Розклад", Icons.Filled.Schedule, Icons.Outlined.Schedule, "tab_schedule"),
    PRICES("Ціни", Icons.Filled.PriceChange, Icons.Outlined.PriceChange, "tab_prices"),
    HISTORY("Журнал", Icons.Filled.History, Icons.Outlined.History, "tab_history")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PillshelfApp(
    viewModel: PillshelfViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(AppTab.SHELF) }
    val scope = rememberCoroutineScope()

    // Sheet and dialog states
    var showAddEditSheet by remember { mutableStateOf(false) }
    var editingMedication by remember { mutableStateOf<Medication?>(null) }
    val addEditSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedMedicationDetail by remember { mutableStateOf<Medication?>(null) }
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showSettingsSheet by remember { mutableStateOf(false) }
    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var restockingMedication by remember { mutableStateOf<Medication?>(null) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        if (isExpandedScreen) {
            // Adaptive wide layout: NavigationRail on the left
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.testTag("desktop_navigation_rail"),
                    header = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Medication,
                                    contentDescription = "Pillshelf Logo",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Pillshelf",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    AppTab.entries.forEach { tab ->
                        NavigationRailItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == tab) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            },
                            label = { Text(tab.title) },
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    NavigationRailItem(
                        selected = false,
                        onClick = { showSettingsSheet = true },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Налаштування та довідка"
                            )
                        },
                        label = { Text("Довідка") },
                        modifier = Modifier.testTag("rail_settings_button")
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = 960.dp)
                    ) {
                        AppScreenContent(
                            selectedTab = selectedTab,
                            viewModel = viewModel,
                            onMedicationClick = { med -> selectedMedicationDetail = med },
                            onAddMedicationClick = {
                                editingMedication = null
                                showAddEditSheet = true
                            },
                            onRestockClick = { med -> restockingMedication = med }
                        )
                    }
                }
            }
        } else {
            // Mobile standard layout: TopBar + Screen + Bottom Navigation
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Medication,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Pillshelf",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { showSettingsSheet = true },
                                modifier = Modifier.testTag("open_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = "Налаштування та довідка"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier.testTag("bottom_navigation_bar"),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        AppTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == tab) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title
                                    )
                                },
                                label = { Text(tab.title) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    AppScreenContent(
                        selectedTab = selectedTab,
                        viewModel = viewModel,
                        onMedicationClick = { med -> selectedMedicationDetail = med },
                        onAddMedicationClick = {
                            editingMedication = null
                            showAddEditSheet = true
                        },
                        onRestockClick = { med -> restockingMedication = med }
                    )
                }
            }
        }

        // Add / Edit Medication Bottom Sheet
        if (showAddEditSheet) {
            AddEditMedicationSheet(
                sheetState = addEditSheetState,
                medicationToEdit = editingMedication,
                onDismiss = {
                    scope.launch { addEditSheetState.hide() }.invokeOnCompletion {
                        showAddEditSheet = false
                        editingMedication = null
                    }
                },
                onSave = { med ->
                    if (editingMedication != null) {
                        viewModel.updateMedication(med)
                    } else {
                        viewModel.addMedication(med)
                    }
                    scope.launch { addEditSheetState.hide() }.invokeOnCompletion {
                        showAddEditSheet = false
                        editingMedication = null
                    }
                }
            )
        }

        // Medication Details Bottom Sheet
        selectedMedicationDetail?.let { med ->
            MedicationDetailSheet(
                medication = med,
                currentEpochDay = viewModel.currentEpochDay,
                sheetState = detailSheetState,
                onDismiss = {
                    scope.launch { detailSheetState.hide() }.invokeOnCompletion {
                        selectedMedicationDetail = null
                    }
                },
                onEdit = {
                    editingMedication = med
                    scope.launch { detailSheetState.hide() }.invokeOnCompletion {
                        selectedMedicationDetail = null
                        showAddEditSheet = true
                    }
                },
                onDelete = {
                    viewModel.deleteMedication(med)
                    scope.launch { detailSheetState.hide() }.invokeOnCompletion {
                        selectedMedicationDetail = null
                    }
                },
                onTakeDose = {
                    viewModel.recordIntake(med, taken = true)
                    scope.launch { detailSheetState.hide() }.invokeOnCompletion {
                        selectedMedicationDetail = null
                    }
                },
                onRestock = {
                    restockingMedication = med
                }
            )
        }

        // Restock Dialog
        restockingMedication?.let { med ->
            RestockDialog(
                medication = med,
                onDismiss = { restockingMedication = null },
                onConfirmRestock = { addedAmount ->
                    viewModel.restockMedication(med, addedAmount)
                    restockingMedication = null
                }
            )
        }

        // Settings & Medical Disclaimer Bottom Sheet
        if (showSettingsSheet) {
            SettingsSheet(
                sheetState = settingsSheetState,
                onDismiss = {
                    scope.launch { settingsSheetState.hide() }.invokeOnCompletion {
                        showSettingsSheet = false
                    }
                }
            )
        }
    }
}

@Composable
private fun AppScreenContent(
    selectedTab: AppTab,
    viewModel: PillshelfViewModel,
    onMedicationClick: (Medication) -> Unit,
    onAddMedicationClick: () -> Unit,
    onRestockClick: (Medication) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = selectedTab,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "AppScreenTransition",
        modifier = modifier.fillMaxSize()
    ) { tab ->
        when (tab) {
            AppTab.SHELF -> ShelfScreen(
                viewModel = viewModel,
                onMedicationClick = onMedicationClick,
                onAddMedicationClick = onAddMedicationClick,
                onRestockClick = onRestockClick
            )
            AppTab.SCHEDULE -> ScheduleScreen(
                viewModel = viewModel,
                onAddMedicationClick = onAddMedicationClick
            )
            AppTab.PRICES -> PricesScreen(
                viewModel = viewModel,
                onAddMedicationClick = onAddMedicationClick
            )
            AppTab.HISTORY -> HistoryScreen(
                viewModel = viewModel
            )
        }
    }
}
