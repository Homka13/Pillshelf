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
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import com.example.pillshelf.ui.screens.HistoryScreen
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
    SHELF("Shelf", Icons.Filled.Inventory2, Icons.Outlined.Inventory2, "tab_shelf"),
    SCHEDULE("Today", Icons.Filled.Schedule, Icons.Outlined.Schedule, "tab_schedule"),
    HISTORY("History", Icons.Filled.History, Icons.Outlined.History, "tab_history")
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

    var restockingMedication by remember { mutableStateOf<Medication?>(null) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // Adaptive Navigation Rail for larger screens
            if (isExpandedScreen) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Spacer(modifier = Modifier.padding(top = 16.dp))
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_pillshelf_icon),
                            contentDescription = "Pillshelf",
                            modifier = Modifier.size(32.dp).clip(CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.padding(top = 24.dp))
                    AppTab.values().forEach { tab ->
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
                }
            }

            // Main Content Area
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_pillshelf_icon),
                                        contentDescription = "Pillshelf Logo",
                                        modifier = Modifier.size(32.dp)
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
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    if (!isExpandedScreen) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp
                        ) {
                            AppTab.values().forEach { tab ->
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
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 700.dp)
                    ) {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "tab_transition"
                        ) { tab ->
                            when (tab) {
                                AppTab.SHELF -> ShelfScreen(
                                    viewModel = viewModel,
                                    onMedicationClick = { med ->
                                        selectedMedicationDetail = med
                                    },
                                    onAddMedicationClick = {
                                        editingMedication = null
                                        showAddEditSheet = true
                                    },
                                    onRestockClick = { med ->
                                        restockingMedication = med
                                    }
                                )
                                AppTab.SCHEDULE -> ScheduleScreen(
                                    viewModel = viewModel,
                                    onAddMedicationClick = {
                                        editingMedication = null
                                        showAddEditSheet = true
                                    }
                                )
                                AppTab.HISTORY -> HistoryScreen(
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Medication BottomSheet
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
                viewModel.saveMedication(med)
                scope.launch { addEditSheetState.hide() }.invokeOnCompletion {
                    showAddEditSheet = false
                    editingMedication = null
                }
            }
        )
    }

    // Medication Detail BottomSheet
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
            onRestock = {
                restockingMedication = med
            },
            onTakeDose = {
                viewModel.quickTakeDose(med)
            },
            onDelete = {
                viewModel.deleteMedication(med)
                scope.launch { detailSheetState.hide() }.invokeOnCompletion {
                    selectedMedicationDetail = null
                }
            }
        )
    }

    // Restock Dialog
    restockingMedication?.let { med ->
        RestockDialog(
            medication = med,
            onDismiss = { restockingMedication = null },
            onConfirmRestock = { addedAmount ->
                viewModel.restockMedication(med.id, addedAmount)
                restockingMedication = null
            }
        )
    }
}
