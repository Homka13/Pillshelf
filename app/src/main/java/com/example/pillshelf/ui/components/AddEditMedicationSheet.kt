package com.example.pillshelf.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.pillshelf.data.model.Medication
import com.example.pillshelf.data.model.MedicationCategory
import com.example.pillshelf.data.model.MedicationForm
import com.example.pillshelf.data.model.ScheduleType
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditMedicationSheet(
    sheetState: SheetState,
    medicationToEdit: Medication? = null,
    onDismiss: () -> Unit,
    onSave: (Medication) -> Unit
) {
    val isEditing = medicationToEdit != null

    var name by remember { mutableStateOf(medicationToEdit?.name ?: "") }
    var brandOrGeneric by remember { mutableStateOf(medicationToEdit?.brandOrGeneric ?: "") }
    var selectedForm by remember { mutableStateOf(medicationToEdit?.form ?: MedicationForm.TABLET) }
    var strength by remember { mutableStateOf(medicationToEdit?.strength ?: "") }
    var selectedCategory by remember { mutableStateOf(medicationToEdit?.category ?: MedicationCategory.OTHER) }
    var stockQuantityText by remember { mutableStateOf(medicationToEdit?.stockQuantity?.toString() ?: "30") }
    var lowStockThresholdText by remember { mutableStateOf(medicationToEdit?.lowStockThreshold?.toString() ?: "5") }
    var unit by remember { mutableStateOf(medicationToEdit?.unit ?: selectedForm.defaultUnit) }
    var storageLocation by remember { mutableStateOf(medicationToEdit?.storageLocation ?: "Medicine Cabinet") }
    var instructions by remember { mutableStateOf(medicationToEdit?.instructions ?: "") }
    var selectedSchedule by remember { mutableStateOf(medicationToEdit?.scheduleType ?: ScheduleType.DAILY) }
    var scheduledTimes by remember { mutableStateOf(medicationToEdit?.scheduledTimes ?: "08:00") }

    // Expiry months from today
    val todayEpoch = LocalDate.now().toEpochDay()
    val initialMonthsAhead = if (medicationToEdit != null && medicationToEdit.expiryDateEpochDays > todayEpoch) {
        ((medicationToEdit.expiryDateEpochDays - todayEpoch) / 30).toInt().coerceIn(1, 48)
    } else 12
    var monthsAhead by remember { mutableStateOf(initialMonthsAhead) }

    val colorOptions = listOf(
        0xFF0D9488, // Teal
        0xFF3B82F6, // Blue
        0xFFF59E0B, // Amber
        0xFFE11D48, // Rose
        0xFF8B5CF6, // Purple
        0xFF10B981, // Emerald
        0xFF0284C7  // Sky
    )
    var selectedColor by remember {
        mutableStateOf(medicationToEdit?.colorHex ?: colorOptions.first())
    }

    var nameError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "Edit Medication" else "Add to Shelf",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Medication Name
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (it.isNotBlank()) nameError = false
                },
                label = { Text("Medication Name *") },
                placeholder = { Text("e.g. Ibuprofen, Amoxicillin, Vitamin C") },
                isError = nameError,
                supportingText = if (nameError) { { Text("Name is required") } } else null,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("med_name_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Strength and Brand/Generic
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = strength,
                    onValueChange = { strength = it },
                    label = { Text("Strength / Dosage") },
                    placeholder = { Text("e.g. 500 mg, 10 ml") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("med_strength_input")
                )
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedTextField(
                    value = brandOrGeneric,
                    onValueChange = { brandOrGeneric = it },
                    label = { Text("Brand / Maker") },
                    placeholder = { Text("e.g. Advil, Bayer") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("med_brand_input")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Medication Form Selector
            Text(
                text = "Form & Unit",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MedicationForm.values().forEach { form ->
                    FilterChip(
                        selected = selectedForm == form,
                        onClick = {
                            selectedForm = form
                            unit = form.defaultUnit
                        },
                        label = { Text(form.displayName) },
                        modifier = Modifier.testTag("form_chip_${form.name}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Selector
            Text(
                text = "Category",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MedicationCategory.values().filter { it != MedicationCategory.ALL }.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.displayName) },
                        modifier = Modifier.testTag("category_chip_${cat.name}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stock & Low Stock Threshold
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = stockQuantityText,
                    onValueChange = {
                        if (it.all { c -> c.isDigit() }) stockQuantityText = it
                    },
                    label = { Text("Initial Stock ($unit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("med_stock_input")
                )
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedTextField(
                    value = lowStockThresholdText,
                    onValueChange = {
                        if (it.all { c -> c.isDigit() }) lowStockThresholdText = it
                    },
                    label = { Text("Low Alert At") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("med_low_threshold_input")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Expiry Estimate Selection
            Text(
                text = "Shelf Expiration Estimate",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            val calculatedExpiryDate = LocalDate.now().plusMonths(monthsAhead.toLong())
            Text(
                text = "Expires approximately: ${calculatedExpiryDate.month.name.take(3)} ${calculatedExpiryDate.year}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(3, 6, 12, 24).forEach { months ->
                    FilterChip(
                        selected = monthsAhead == months,
                        onClick = { monthsAhead = months },
                        label = { Text("$months mos") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Storage Location
            OutlinedTextField(
                value = storageLocation,
                onValueChange = { storageLocation = it },
                label = { Text("Storage Location on Shelf") },
                placeholder = { Text("e.g. Medicine Cabinet, Bathroom, Fridge") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("med_location_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Instructions & Food notes
            OutlinedTextField(
                value = instructions,
                onValueChange = { instructions = it },
                label = { Text("Instructions & Notes") },
                placeholder = { Text("e.g. Take with food; do not drive") },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("med_instructions_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Schedule Type
            Text(
                text = "Schedule Routine",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScheduleType.values().forEach { sched ->
                    FilterChip(
                        selected = selectedSchedule == sched,
                        onClick = {
                            selectedSchedule = sched
                            scheduledTimes = when (sched) {
                                ScheduleType.DAILY -> "08:00"
                                ScheduleType.TWICE_DAILY -> "08:00, 20:00"
                                ScheduleType.THREE_TIMES_DAILY -> "08:00, 14:00, 20:00"
                                ScheduleType.AS_NEEDED -> ""
                                ScheduleType.CUSTOM -> "09:00"
                            }
                        },
                        label = { Text(sched.displayName) }
                    )
                }
            }

            if (selectedSchedule != ScheduleType.AS_NEEDED) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = scheduledTimes,
                    onValueChange = { scheduledTimes = it },
                    label = { Text("Scheduled Time(s) (HH:mm)") },
                    placeholder = { Text("e.g. 08:00 or 08:00, 20:00") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tag Color
            Text(
                text = "Tag Color",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                colorOptions.forEach { colorVal ->
                    val isSelected = selectedColor == colorVal
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(colorVal))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedColor = colorVal },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected color",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    val stock = stockQuantityText.toIntOrNull() ?: 0
                    val lowThreshold = lowStockThresholdText.toIntOrNull() ?: 5
                    val expiryDays = LocalDate.now().plusMonths(monthsAhead.toLong()).toEpochDay()

                    val med = (medicationToEdit ?: Medication(name = name)).copy(
                        name = name.trim(),
                        brandOrGeneric = brandOrGeneric.trim(),
                        form = selectedForm,
                        strength = strength.trim(),
                        category = selectedCategory,
                        stockQuantity = stock,
                        lowStockThreshold = lowThreshold,
                        unit = unit.ifBlank { selectedForm.defaultUnit },
                        expiryDateEpochDays = expiryDays,
                        storageLocation = storageLocation.trim().ifBlank { "Medicine Cabinet" },
                        instructions = instructions.trim(),
                        scheduleType = selectedSchedule,
                        scheduledTimes = scheduledTimes.trim(),
                        colorHex = selectedColor
                    )
                    onSave(med)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_medication_btn")
            ) {
                Text(
                    text = if (isEditing) "Save Changes" else "Add to Shelf",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
