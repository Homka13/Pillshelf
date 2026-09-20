package com.example.pillshelf.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pillshelf.data.local.PillshelfDatabase
import com.example.pillshelf.data.model.DoseLog
import com.example.pillshelf.data.model.DoseStatus
import com.example.pillshelf.data.model.Medication
import com.example.pillshelf.data.model.MedicationCategory
import com.example.pillshelf.data.model.ScheduleType
import com.example.pillshelf.data.repository.PillshelfRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ScheduledDoseItem(
    val medication: Medication,
    val scheduledTime: String,
    val timeSlotLabel: String, // "Morning", "Afternoon", "Evening", "Bedtime", "As Needed"
    val isTaken: Boolean = false,
    val isSkipped: Boolean = false,
    val logId: Long? = null,
    val loggedTimestamp: Long? = null
)

data class AdherenceSummary(
    val totalScheduled: Int = 0,
    val takenCount: Int = 0,
    val skippedCount: Int = 0,
    val percentage: Int = 0
)

data class ShelfStats(
    val totalCount: Int = 0,
    val lowStockCount: Int = 0,
    val expiringCount: Int = 0,
    val expiredCount: Int = 0
)

class PillshelfViewModel(
    application: Application,
    private val repository: PillshelfRepository
) : AndroidViewModel(application) {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(MedicationCategory.ALL)
    val selectedCategory: StateFlow<MedicationCategory> = _selectedCategory.asStateFlow()

    private val _filterLowStockOnly = MutableStateFlow(false)
    val filterLowStockOnly: StateFlow<Boolean> = _filterLowStockOnly.asStateFlow()

    private val _filterExpiringOnly = MutableStateFlow(false)
    val filterExpiringOnly: StateFlow<Boolean> = _filterExpiringOnly.asStateFlow()

    private val _selectedDateEpochDay = MutableStateFlow(LocalDate.now().toEpochDay())
    val selectedDateEpochDay: StateFlow<Long> = _selectedDateEpochDay.asStateFlow()

    val allMedications: StateFlow<List<Medication>> = repository.allMedications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allLogs: StateFlow<List<DoseLog>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentEpochDay: Long
        get() = LocalDate.now().toEpochDay()

    // Filtered Medications for Shelf
    val filteredMedications: StateFlow<List<Medication>> = combine(
        allMedications,
        _searchQuery,
        _selectedCategory,
        _filterLowStockOnly,
        _filterExpiringOnly
    ) { meds, query, category, lowStock, expiring ->
        val today = LocalDate.now().toEpochDay()
        meds.filter { med ->
            val matchesQuery = query.isBlank() ||
                    med.name.contains(query, ignoreCase = true) ||
                    med.brandOrGeneric.contains(query, ignoreCase = true) ||
                    med.instructions.contains(query, ignoreCase = true) ||
                    med.storageLocation.contains(query, ignoreCase = true)

            val matchesCategory = category == MedicationCategory.ALL || med.category == category
            val matchesLowStock = !lowStock || med.isLowStock()
            val matchesExpiring = !expiring || med.isExpiringSoon(today) || med.isExpired(today)

            matchesQuery && matchesCategory && matchesLowStock && matchesExpiring
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Shelf Overview Statistics
    val shelfStats: StateFlow<ShelfStats> = allMedications.combine(_selectedDateEpochDay) { meds, today ->
        val todayEpoch = LocalDate.now().toEpochDay()
        ShelfStats(
            totalCount = meds.size,
            lowStockCount = meds.count { it.isLowStock() },
            expiringCount = meds.count { it.isExpiringSoon(todayEpoch) },
            expiredCount = meds.count { it.isExpired(todayEpoch) }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ShelfStats()
    )

    // Today's Scheduled Dose Items
    val scheduledDoses: StateFlow<List<ScheduledDoseItem>> = combine(
        allMedications,
        allLogs,
        _selectedDateEpochDay
    ) { meds, logs, selectedDay ->
        val dayLogs = logs.filter { it.dateEpochDay == selectedDay }
        val items = mutableListOf<ScheduledDoseItem>()

        for (med in meds) {
            when (med.scheduleType) {
                ScheduleType.AS_NEEDED -> {
                    // For PRN, show logged doses if any, plus one "As Needed" action row
                    val prnLogs = dayLogs.filter { it.medicationId == med.id }
                    if (prnLogs.isNotEmpty()) {
                        for (log in prnLogs) {
                            items.add(
                                ScheduledDoseItem(
                                    medication = med,
                                    scheduledTime = log.scheduledTime.ifBlank { "PRN" },
                                    timeSlotLabel = "As Needed",
                                    isTaken = log.status == DoseStatus.TAKEN,
                                    isSkipped = log.status == DoseStatus.SKIPPED,
                                    logId = log.id,
                                    loggedTimestamp = log.timestamp
                                )
                            )
                        }
                    } else {
                        // Empty PRN entry ready to take
                        items.add(
                            ScheduledDoseItem(
                                medication = med,
                                scheduledTime = "As needed",
                                timeSlotLabel = "As Needed",
                                isTaken = false,
                                isSkipped = false
                            )
                        )
                    }
                }
                else -> {
                    val times = med.getTimesList()
                    if (times.isEmpty()) {
                        val matchingLog = dayLogs.firstOrNull { it.medicationId == med.id }
                        items.add(
                            ScheduledDoseItem(
                                medication = med,
                                scheduledTime = "08:00",
                                timeSlotLabel = getTimeSlotLabel("08:00"),
                                isTaken = matchingLog?.status == DoseStatus.TAKEN,
                                isSkipped = matchingLog?.status == DoseStatus.SKIPPED,
                                logId = matchingLog?.id,
                                loggedTimestamp = matchingLog?.timestamp
                            )
                        )
                    } else {
                        for (time in times) {
                            val matchingLog = dayLogs.firstOrNull {
                                it.medicationId == med.id && (it.scheduledTime == time || it.scheduledTime.isBlank())
                            }
                            items.add(
                                ScheduledDoseItem(
                                    medication = med,
                                    scheduledTime = time,
                                    timeSlotLabel = getTimeSlotLabel(time),
                                    isTaken = matchingLog?.status == DoseStatus.TAKEN,
                                    isSkipped = matchingLog?.status == DoseStatus.SKIPPED,
                                    logId = matchingLog?.id,
                                    loggedTimestamp = matchingLog?.timestamp
                                )
                            )
                        }
                    }
                }
            }
        }
        // Sort by time
        items.sortedWith(
            compareBy(
                { it.timeSlotLabel == "As Needed" },
                { it.scheduledTime }
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Adherence Calculation for Selected Day
    val adherenceSummary: StateFlow<AdherenceSummary> = scheduledDoses.combine(_selectedDateEpochDay) { doses, _ ->
        val scheduledList = doses.filter { it.timeSlotLabel != "As Needed" || it.isTaken }
        val total = scheduledList.size
        val taken = scheduledList.count { it.isTaken }
        val skipped = scheduledList.count { it.isSkipped }
        val pct = if (total > 0) ((taken.toFloat() / total.toFloat()) * 100).toInt() else 100
        AdherenceSummary(
            totalScheduled = total,
            takenCount = taken,
            skippedCount = skipped,
            percentage = pct
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AdherenceSummary()
    )

    private fun getTimeSlotLabel(time: String): String {
        val hour = time.split(":").firstOrNull()?.toIntOrNull() ?: 8
        return when {
            hour < 12 -> "Morning"
            hour < 17 -> "Afternoon"
            hour < 21 -> "Evening"
            else -> "Bedtime"
        }
    }

    // UI Action Handlers
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: MedicationCategory) {
        _selectedCategory.value = category
    }

    fun toggleFilterLowStock() {
        _filterLowStockOnly.value = !_filterLowStockOnly.value
    }

    fun toggleFilterExpiring() {
        _filterExpiringOnly.value = !_filterExpiringOnly.value
    }

    fun setSelectedDate(epochDay: Long) {
        _selectedDateEpochDay.value = epochDay
    }

    fun markDoseTaken(item: ScheduledDoseItem, notes: String = "") {
        viewModelScope.launch {
            repository.logDose(
                medication = item.medication,
                scheduledTime = item.scheduledTime,
                dateEpochDay = _selectedDateEpochDay.value,
                status = DoseStatus.TAKEN,
                notes = notes
            )
        }
    }

    fun markDoseSkipped(item: ScheduledDoseItem, reason: String = "") {
        viewModelScope.launch {
            repository.logDose(
                medication = item.medication,
                scheduledTime = item.scheduledTime,
                dateEpochDay = _selectedDateEpochDay.value,
                status = DoseStatus.SKIPPED,
                notes = reason.ifBlank { "Skipped dose" }
            )
        }
    }

    fun quickTakeDose(medication: Medication) {
        viewModelScope.launch {
            repository.logDose(
                medication = medication,
                scheduledTime = "Quick Dose",
                dateEpochDay = LocalDate.now().toEpochDay(),
                status = DoseStatus.TAKEN,
                notes = "Quick taken from shelf"
            )
        }
    }

    fun undoLog(logId: Long) {
        viewModelScope.launch {
            val log = allLogs.value.firstOrNull { it.id == logId }
            if (log != null) {
                repository.undoLog(log)
            }
        }
    }

    fun restockMedication(id: Long, addedStock: Int) {
        viewModelScope.launch {
            repository.restock(id, addedStock)
        }
    }

    fun saveMedication(medication: Medication) {
        viewModelScope.launch {
            if (medication.id == 0L) {
                repository.insertMedication(medication)
            } else {
                repository.updateMedication(medication)
            }
        }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch {
            repository.deleteMedication(medication)
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = PillshelfDatabase.getInstance(application)
                    val repository = PillshelfRepository(db.medicationDao(), db.doseLogDao())
                    return PillshelfViewModel(application, repository) as T
                }
            }
    }
}
