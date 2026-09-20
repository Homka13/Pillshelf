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
import androidx.compose.material3.Checkbox
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
    var activeSubstance by remember { mutableStateOf(medicationToEdit?.activeSubstance ?: "") }
    var dosageForm by remember { mutableStateOf(medicationToEdit?.dosageForm ?: "Таблетки 500 мг") }
    var manufacturer by remember { mutableStateOf(medicationToEdit?.manufacturer ?: "") }
    var category by remember { mutableStateOf(medicationToEdit?.category ?: "Аптечка") }
    var totalQuantityText by remember { mutableStateOf(medicationToEdit?.totalQuantity?.toString() ?: "30") }
    var remainingQuantityText by remember { mutableStateOf(medicationToEdit?.remainingQuantity?.toString() ?: totalQuantityText) }

    var expiryDateText by remember { mutableStateOf(medicationToEdit?.expiryDate ?: LocalDate.now().plusMonths(12).toString()) }

    var scheduleType by remember { mutableStateOf(medicationToEdit?.scheduleType ?: "DAILY") }
    var intervalHoursText by remember { mutableStateOf(medicationToEdit?.intervalHours?.toString() ?: "8") }
    var morningSelected by remember { mutableStateOf(medicationToEdit?.timeOfDay?.contains("MORNING", ignoreCase = true) ?: true) }
    var afternoonSelected by remember { mutableStateOf(medicationToEdit?.timeOfDay?.contains("AFTERNOON", ignoreCase = true) ?: false) }
    var eveningSelected by remember { mutableStateOf(medicationToEdit?.timeOfDay?.contains("EVENING", ignoreCase = true) ?: true) }

    var courseDurationText by remember { mutableStateOf(medicationToEdit?.courseDurationDays?.toString() ?: "7") }
    var takeBeforeMeal by remember { mutableStateOf(medicationToEdit?.takeBeforeMeal ?: false) }
    var notes by remember { mutableStateOf(medicationToEdit?.notes ?: "") }

    var trackPrices by remember { mutableStateOf(medicationToEdit?.trackPrices ?: false) }
    var targetPriceText by remember { mutableStateOf(if ((medicationToEdit?.targetPrice ?: 0.0) > 0) medicationToEdit?.targetPrice.toString() else "") }

    var selectedColorHex by remember { mutableStateOf(medicationToEdit?.colorHex ?: 0xFF0D9488) }
    var nameError by remember { mutableStateOf(false) }

    val categories = listOf("Аптечка", "Знеболювальні", "Вітаміни", "Травлення", "Рецептурні", "Протизастудні")
    val dosageForms = listOf("Таблетки", "Капсули", "Сироп", "Спрей", "Краплі", "Мазь / Крем", "Порошок")
    val colorPalette = listOf(
        0xFF0D9488, // Teal
        0xFFE11D48, // Rose / Red
        0xFFD97706, // Amber
        0xFF059669, // Emerald
        0xFF2563EB, // Blue
        0xFF7C3AED, // Violet
        0xFF4F46E5  // Indigo
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("add_edit_medication_sheet"),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "Редагувати препарат" else "Додати ліки",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Закрити")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Color Palette Selector
            Text(
                text = "Колірна мітка",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                colorPalette.forEach { hex ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(hex))
                            .clickable { selectedColorHex = hex }
                            .border(
                                width = if (selectedColorHex == hex) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedColorHex == hex) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Name (Required)
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (it.isNotBlank()) nameError = false
                },
                label = { Text("Назва ліку *") },
                placeholder = { Text("Наприклад, Парацетамол, Ібупрофен") },
                singleLine = true,
                isError = nameError,
                supportingText = {
                    if (nameError) Text("Назва є обов'язковою", color = MaterialTheme.colorScheme.error)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("medication_name_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Active Substance & Manufacturer
            OutlinedTextField(
                value = activeSubstance,
                onValueChange = { activeSubstance = it },
                label = { Text("Діюча речовина (Active Substance)") },
                placeholder = { Text("Наприклад, Paracetamol, Ibuprofen") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("medication_substance_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = dosageForm,
                    onValueChange = { dosageForm = it },
                    label = { Text("Форма випуску") },
                    placeholder = { Text("Таблетки 500 мг") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = manufacturer,
                    onValueChange = { manufacturer = it },
                    label = { Text("Виробник") },
                    placeholder = { Text("Фармак, Дарниця") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dosage form quick chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                dosageForms.forEach { form ->
                    FilterChip(
                        selected = dosageForm.startsWith(form, ignoreCase = true),
                        onClick = { dosageForm = form },
                        label = { Text(form) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Category selection
            Text(
                text = "Категорія",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Quantities & Expiry Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = totalQuantityText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            totalQuantityText = input
                            if (!isEditing) remainingQuantityText = input
                        }
                    },
                    label = { Text("В упаковці (шт)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = remainingQuantityText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) remainingQuantityText = input
                    },
                    label = { Text("Залишок (шт)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = expiryDateText,
                onValueChange = { expiryDateText = it },
                label = { Text("Термін придатності (РРРР-ММ-ДД)") },
                placeholder = { Text("2027-05-30") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Schedule Type
            Text(
                text = "Графік прийому",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = scheduleType == "DAILY",
                    onClick = { scheduleType = "DAILY" },
                    label = { Text("Щодня") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = scheduleType == "EVERY_N_HOURS",
                    onClick = { scheduleType = "EVERY_N_HOURS" },
                    label = { Text("Інтервально") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = scheduleType == "COURSE",
                    onClick = { scheduleType = "COURSE" },
                    label = { Text("Курс лікування") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (scheduleType) {
                "DAILY" -> {
                    Text(
                        text = "Час прийому протягом дня:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = morningSelected,
                            onClick = { morningSelected = !morningSelected },
                            label = { Text("Ранок (08:00)") }
                        )
                        FilterChip(
                            selected = afternoonSelected,
                            onClick = { afternoonSelected = !afternoonSelected },
                            label = { Text("День (13:00)") }
                        )
                        FilterChip(
                            selected = eveningSelected,
                            onClick = { eveningSelected = !eveningSelected },
                            label = { Text("Вечір (20:00)") }
                        )
                    }
                }
                "EVERY_N_HOURS" -> {
                    OutlinedTextField(
                        value = intervalHoursText,
                        onValueChange = { if (it.all { char -> char.isDigit() }) intervalHoursText = it },
                        label = { Text("Інтервал між прийомами (годин)") },
                        placeholder = { Text("8") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                "COURSE" -> {
                    OutlinedTextField(
                        value = courseDurationText,
                        onValueChange = { if (it.all { char -> char.isDigit() }) courseDurationText = it },
                        label = { Text("Тривалість курсу (днів)") },
                        placeholder = { Text("7") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Meal Relationship
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { takeBeforeMeal = !takeBeforeMeal }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = takeBeforeMeal,
                        onCheckedChange = { takeBeforeMeal = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (takeBeforeMeal) "Приймати до їжі (на голодний шлунок)" else "Приймати після їжі (або під час)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Нотатки та особливі вказівки") },
                placeholder = { Text("Запити водою, не розжовувати, зберігати в темному місці") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 6. Price Tracking Section
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { trackPrices = !trackPrices },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = trackPrices,
                            onCheckedChange = { trackPrices = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Відстежувати ціни в аптеках",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Моніторинг через Tabletki.ua та мережі аптек України",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (trackPrices) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = targetPriceText,
                            onValueChange = { targetPriceText = it },
                            label = { Text("Бажана ціна (UAH) для сповіщень") },
                            placeholder = { Text("Наприклад, 50.0") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = { Text("₴", modifier = Modifier.padding(end = 12.dp)) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons: Save & Cancel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            nameError = true
                            return@Button
                        }

                        val timeSlots = mutableListOf<String>()
                        if (morningSelected) timeSlots.add("MORNING")
                        if (afternoonSelected) timeSlots.add("AFTERNOON")
                        if (eveningSelected) timeSlots.add("EVENING")
                        if (timeSlots.isEmpty()) timeSlots.add("MORNING")

                        val totalQty = totalQuantityText.toIntOrNull() ?: 30
                        val remainingQty = remainingQuantityText.toIntOrNull() ?: totalQty
                        val interval = intervalHoursText.toIntOrNull() ?: 8
                        val courseDays = courseDurationText.toIntOrNull() ?: 7
                        val targetPrice = targetPriceText.toDoubleOrNull() ?: 0.0

                        val med = (medicationToEdit ?: Medication(name = name)).copy(
                            name = name.trim(),
                            activeSubstance = activeSubstance.trim(),
                            dosageForm = dosageForm.trim(),
                            manufacturer = manufacturer.trim(),
                            category = category,
                            totalQuantity = totalQty,
                            remainingQuantity = remainingQty,
                            expiryDate = expiryDateText.trim(),
                            scheduleType = scheduleType,
                            intervalHours = interval,
                            timeOfDay = timeSlots.joinToString(","),
                            courseDurationDays = courseDays,
                            takeBeforeMeal = takeBeforeMeal,
                            notes = notes.trim(),
                            trackPrices = trackPrices,
                            targetPrice = targetPrice,
                            colorHex = selectedColorHex,
                            updatedAt = System.currentTimeMillis()
                        )

                        onSave(med)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_medication_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (isEditing) "Зберегти зміни" else "Додати ліки",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
