package com.example.pillshelf.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pillshelf.data.local.PillshelfDatabase
import com.example.pillshelf.data.model.Category
import com.example.pillshelf.data.model.IntakeHistory
import com.example.pillshelf.data.model.Medication
import com.example.pillshelf.data.model.PriceHistory
import com.example.pillshelf.data.repository.IntakeRepository
import com.example.pillshelf.data.repository.MedicationRepository
import com.example.pillshelf.data.repository.PriceRepository
import com.example.pillshelf.domain.model.InteractionWarning
import com.example.pillshelf.domain.model.PriceInfo
import com.example.pillshelf.domain.model.PriceTrend
import com.example.pillshelf.domain.usecase.AnalyzePriceTrendUseCase
import com.example.pillshelf.domain.usecase.CalculateNextIntakeUseCase
import com.example.pillshelf.domain.usecase.CheckMedicationInteractionsUseCase
import com.example.pillshelf.service.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ScheduledDoseItem(
    val medication: Medication,
    val scheduledTime: String,
    val timeSlotLabel: String, // "Ранок (08:00)", "День (13:00)", "Вечір (20:00)", "Інтервал (кожні N год)"
    val isTaken: Boolean = false,
    val isSkipped: Boolean = false,
    val isOverdue: Boolean = false,
    val historyId: Long? = null,
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

data class TrackedMedicationPrice(
    val medication: Medication,
    val latestPrices: List<PriceHistory>,
    val trendResult: AnalyzePriceTrendUseCase.TrendResult,
    val bestPrice: Double,
    val bestPharmacy: String
)

class PillshelfViewModel(
    application: Application,
    private val medicationRepository: MedicationRepository,
    private val intakeRepository: IntakeRepository,
    private val priceRepository: PriceRepository
) : AndroidViewModel(application) {

    private val calculateNextIntakeUseCase = CalculateNextIntakeUseCase()
    private val analyzePriceTrendUseCase = AnalyzePriceTrendUseCase()
    private val checkMedicationInteractionsUseCase = CheckMedicationInteractionsUseCase()

    init {
        NotificationHelper.createNotificationChannels(application)
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Всі")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _filterLowStockOnly = MutableStateFlow(false)
    val filterLowStockOnly: StateFlow<Boolean> = _filterLowStockOnly.asStateFlow()

    private val _filterExpiringOnly = MutableStateFlow(false)
    val filterExpiringOnly: StateFlow<Boolean> = _filterExpiringOnly.asStateFlow()

    private val _isRefreshingPrices = MutableStateFlow(false)
    val isRefreshingPrices: StateFlow<Boolean> = _isRefreshingPrices.asStateFlow()

    val allMedications: StateFlow<List<Medication>> = medicationRepository.allMedications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allCategories: StateFlow<List<Category>> = medicationRepository.allCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allHistory: StateFlow<List<IntakeHistory>> = intakeRepository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentEpochDay: Long
        get() = LocalDate.now().toEpochDay()

    // Filtered Medications for Cabinet / Shelf Screen
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
                    med.activeSubstance.contains(query, ignoreCase = true) ||
                    med.manufacturer.contains(query, ignoreCase = true) ||
                    med.category.contains(query, ignoreCase = true) ||
                    med.notes.contains(query, ignoreCase = true)

            val matchesCategory = category == "Всі" || med.category.equals(category, ignoreCase = true)
            val matchesLowStock = !lowStock || med.isLowStock() || med.isOutOfStock()
            val matchesExpiring = !expiring || med.isExpiringSoon(today) || med.isExpired(today)

            matchesQuery && matchesCategory && matchesLowStock && matchesExpiring
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Shelf Overview Statistics
    val shelfStats: StateFlow<ShelfStats> = allMedications.combine(_searchQuery) { meds, _ ->
        val today = LocalDate.now().toEpochDay()
        ShelfStats(
            totalCount = meds.size,
            lowStockCount = meds.count { it.isLowStock() || it.isOutOfStock() },
            expiringCount = meds.count { it.isExpiringSoon(today) },
            expiredCount = meds.count { it.isExpired(today) }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ShelfStats()
    )

    // Drug Interaction Warnings
    val interactionWarnings: StateFlow<List<InteractionWarning>> = allMedications.combine(_searchQuery) { meds, _ ->
        checkMedicationInteractionsUseCase.checkInteractions(meds)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Scheduled Doses for Today
    val scheduledDoses: StateFlow<List<ScheduledDoseItem>> = combine(
        allMedications,
        allHistory
    ) { meds, history ->
        val todayLocalDate = LocalDate.now()
        val startOfTodayMillis = todayLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfTodayMillis = todayLocalDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val todayIntakes = history.filter { it.intakeTime in startOfTodayMillis until endOfTodayMillis }
        val items = mutableListOf<ScheduledDoseItem>()
        val currentTime = java.time.LocalTime.now()

        for (med in meds) {
            when (med.scheduleType.uppercase()) {
                "AS_NEEDED" -> {
                    // As-needed (PRN) medications do not produce strict scheduled daily alarms
                }
                "DAILY" -> {
                    val timeSlots = med.getTimeOfDayList()
                    for (slot in timeSlots) {
                        val (timeStr, slotTime) = when (slot.uppercase()) {
                            "MORNING" -> Pair("08:00", java.time.LocalTime.of(8, 0))
                            "AFTERNOON" -> Pair("13:00", java.time.LocalTime.of(13, 0))
                            "EVENING" -> Pair("20:00", java.time.LocalTime.of(20, 0))
                            "BEDTIME" -> Pair("22:00", java.time.LocalTime.of(22, 0))
                            else -> Pair("08:00", java.time.LocalTime.of(8, 0))
                        }
                        val slotLabel = when (slot.uppercase()) {
                            "MORNING" -> "Ранок (08:00)"
                            "AFTERNOON" -> "День (13:00)"
                            "EVENING" -> "Вечір (20:00)"
                            "BEDTIME" -> "Перед сном (22:00)"
                            else -> "Ранок (08:00)"
                        }

                        // Check if an intake was already logged for this med today
                        val match = todayIntakes.firstOrNull {
                            it.medicationId == med.id &&
                            (it.notes.contains(slot, ignoreCase = true) || it.notes.contains(timeStr) || todayIntakes.size == 1)
                        }

                        val isTaken = match?.taken == true
                        val isSkipped = match?.taken == false
                        val isOverdue = !isTaken && !isSkipped && currentTime.isAfter(slotTime)

                        items.add(
                            ScheduledDoseItem(
                                medication = med,
                                scheduledTime = timeStr,
                                timeSlotLabel = slotLabel,
                                isTaken = isTaken,
                                isSkipped = isSkipped,
                                isOverdue = isOverdue,
                                historyId = match?.id,
                                loggedTimestamp = match?.actualTime
                            )
                        )
                    }
                }
                "EVERY_N_HOURS" -> {
                    val interval = if (medicationIntervalHours(med) > 0) medicationIntervalHours(med) else 8
                    val slotLabel = "Кожні $interval год."
                    val match = todayIntakes.firstOrNull { it.medicationId == med.id }
                    val isTaken = match?.taken == true
                    val isSkipped = match?.taken == false
                    val isOverdue = !isTaken && !isSkipped && currentTime.isAfter(java.time.LocalTime.of(14, 0))

                    items.add(
                        ScheduledDoseItem(
                            medication = med,
                            scheduledTime = "Кожні ${interval}г",
                            timeSlotLabel = slotLabel,
                            isTaken = isTaken,
                            isSkipped = isSkipped,
                            isOverdue = isOverdue,
                            historyId = match?.id,
                            loggedTimestamp = match?.actualTime
                        )
                    )
                }
                "COURSE" -> {
                    // Check if course has not ended
                    val startDate = try {
                        if (med.courseStartDate.isNotBlank()) LocalDate.parse(med.courseStartDate) else LocalDate.now()
                    } catch (e: Exception) {
                        LocalDate.now()
                    }
                    val courseEnd = startDate.plusDays(med.courseDurationDays.toLong())
                    if (!LocalDate.now().isAfter(courseEnd)) {
                        val match = todayIntakes.firstOrNull { it.medicationId == med.id }
                        val isTaken = match?.taken == true
                        val isSkipped = match?.taken == false
                        val isOverdue = !isTaken && !isSkipped && currentTime.isAfter(java.time.LocalTime.of(12, 0))

                        items.add(
                            ScheduledDoseItem(
                                medication = med,
                                scheduledTime = "08:00, 20:00",
                                timeSlotLabel = "Курс лікування (${med.courseDurationDays} дн.)",
                                isTaken = isTaken,
                                isSkipped = isSkipped,
                                isOverdue = isOverdue,
                                historyId = match?.id,
                                loggedTimestamp = match?.actualTime
                            )
                        )
                    }
                }
                else -> {
                    val match = todayIntakes.firstOrNull { it.medicationId == med.id }
                    val isTaken = match?.taken == true
                    val isSkipped = match?.taken == false
                    val isOverdue = !isTaken && !isSkipped && currentTime.isAfter(java.time.LocalTime.of(12, 0))

                    items.add(
                        ScheduledDoseItem(
                            medication = med,
                            scheduledTime = "08:00",
                            timeSlotLabel = "За розкладом",
                            isTaken = isTaken,
                            isSkipped = isSkipped,
                            isOverdue = isOverdue,
                            historyId = match?.id,
                            loggedTimestamp = match?.actualTime
                        )
                    )
                }
            }
        }

        // Sort items by scheduled time
        items.sortedBy { it.scheduledTime }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Adherence Summary
    val adherenceSummary: StateFlow<AdherenceSummary> = scheduledDoses.combine(allHistory) { scheduled, _ ->
        val total = scheduled.size
        val taken = scheduled.count { it.isTaken }
        val skipped = scheduled.count { it.isSkipped }
        val pct = if (total > 0) (taken * 100) / total else 0
        AdherenceSummary(totalScheduled = total, takenCount = taken, skippedCount = skipped, percentage = pct)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AdherenceSummary()
    )

    // Tracked Medications with Price Trends
    private val _trackedPrices = MutableStateFlow<List<TrackedMedicationPrice>>(emptyList())
    val trackedPrices: StateFlow<List<TrackedMedicationPrice>> = _trackedPrices.asStateFlow()

    init {
        // Load initial tracked prices when medications change
        viewModelScope.launch(Dispatchers.IO) {
            allMedications.collect { meds ->
                val trackedMeds = meds.filter { it.trackPrices }
                val list = mutableListOf<TrackedMedicationPrice>()
                val db = PillshelfDatabase.getInstance(getApplication())

                for (med in trackedMeds) {
                    val history = db.priceHistoryDao().getHistoryForMedicationSync(med.id)
                    val trend = analyzePriceTrendUseCase.analyze(history)
                    val best = history.minByOrNull { it.price }
                    list.add(
                        TrackedMedicationPrice(
                            medication = med,
                            latestPrices = history.take(6),
                            trendResult = trend,
                            bestPrice = best?.price ?: 0.0,
                            bestPharmacy = best?.pharmacyName ?: "Аптека"
                        )
                    )
                }
                _trackedPrices.value = list
            }
        }
    }

    private fun medicationIntervalHours(med: Medication): Int = med.intervalHours

    // User Actions
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun toggleFilterLowStock() {
        _filterLowStockOnly.value = !_filterLowStockOnly.value
    }

    fun toggleFilterExpiring() {
        _filterExpiringOnly.value = !_filterExpiringOnly.value
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = "Всі"
        _filterLowStockOnly.value = false
        _filterExpiringOnly.value = false
    }

    fun addMedication(medication: Medication) {
        viewModelScope.launch(Dispatchers.IO) {
            // AC-1: Remaining quantity is set equal to totalQuantity on creation
            val initialStock = if (medication.remainingQuantity > 0) medication.remainingQuantity else medication.totalQuantity
            val prepared = medication.copy(
                remainingQuantity = initialStock,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val newId = medicationRepository.insertMedication(prepared)
            val savedMed = prepared.copy(id = newId)

            // Schedule exact alarm for the newly added medication
            com.example.pillshelf.service.ReminderScheduler.scheduleMedication(getApplication(), savedMed)

            // If price tracking is explicitly enabled by user, fetch initial prices
            if (prepared.trackPrices) {
                try {
                    priceRepository.fetchAndStorePrices(newId, prepared.name)
                } catch (e: Exception) {
                    // Handled gracefully
                }
            }
        }
    }

    fun updateMedication(medication: Medication) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = medication.copy(updatedAt = System.currentTimeMillis())
            medicationRepository.updateMedication(updated)
            com.example.pillshelf.service.ReminderScheduler.scheduleMedication(getApplication(), updated)
        }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch(Dispatchers.IO) {
            com.example.pillshelf.service.ReminderScheduler.cancelAlarm(getApplication(), medication.id)
            medicationRepository.deleteMedication(medication)
        }
    }

    fun recordIntake(medication: Medication, taken: Boolean, slotNotes: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            intakeRepository.recordIntake(
                medication = medication,
                taken = taken,
                notes = if (slotNotes.isNotBlank()) slotNotes else if (taken) "Прийнято" else "Пропущено"
            )

            // Reschedule alarm for next planned intake
            com.example.pillshelf.service.ReminderScheduler.scheduleMedication(
                getApplication(),
                medication,
                java.time.LocalDateTime.now()
            )

            // If stock reaches low threshold, show notification per AC-3
            val newStock = medication.remainingQuantity - 1
            if (taken && newStock <= 5) {
                val title = if (newStock <= 0) "Закінчилися ліки!" else "Закінчується запас!"
                val msg = if (newStock <= 0) "Препарат ${medication.name} закінчився. Будь ласка, купіть нову упаковку."
                          else "Препарат ${medication.name}: залишилось лише $newStock шт."
                NotificationHelper.showNotification(
                    getApplication(),
                    title,
                    msg,
                    medication.id + 5000,
                    NotificationHelper.CHANNEL_REMINDERS
                )
            }
        }
    }

    fun undoIntake(historyId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val db = PillshelfDatabase.getInstance(getApplication())
            val item = db.intakeHistoryDao().getById(historyId)
            if (item != null) {
                intakeRepository.undoIntake(item)
            }
        }
    }

    fun restockMedication(medication: Medication, addedAmount: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            medicationRepository.incrementRemainingQuantity(medication.id, addedAmount)
            // If was out of stock, reschedule alarms
            val restocked = medication.copy(remainingQuantity = medication.remainingQuantity + addedAmount)
            com.example.pillshelf.service.ReminderScheduler.scheduleMedication(getApplication(), restocked)
        }
    }

    fun updateRemainingQuantity(medication: Medication, newQuantity: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            medicationRepository.updateRemainingQuantity(medication.id, newQuantity)
        }
    }

    fun togglePriceTracking(medication: Medication) {
        viewModelScope.launch(Dispatchers.IO) {
            val newTrack = !medication.trackPrices
            val updated = medication.copy(trackPrices = newTrack, updatedAt = System.currentTimeMillis())
            medicationRepository.updateMedication(updated)

            if (newTrack) {
                priceRepository.fetchAndStorePrices(medication.id, medication.name)
            }
        }
    }

    fun refreshAllTrackedPrices() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshingPrices.value = true
            try {
                val tracked = allMedications.value.filter { it.trackPrices }
                for (med in tracked) {
                    priceRepository.fetchAndStorePrices(med.id, med.name)
                }
                // Reload
                val db = PillshelfDatabase.getInstance(getApplication())
                val list = mutableListOf<TrackedMedicationPrice>()
                for (med in tracked) {
                    val history = db.priceHistoryDao().getHistoryForMedicationSync(med.id)
                    val trend = analyzePriceTrendUseCase.analyze(history)
                    val best = history.minByOrNull { it.price }
                    list.add(
                        TrackedMedicationPrice(
                            medication = med,
                            latestPrices = history.take(6),
                            trendResult = trend,
                            bestPrice = best?.price ?: 0.0,
                            bestPharmacy = best?.pharmacyName ?: "Аптека"
                        )
                    )
                }
                _trackedPrices.value = list
            } catch (e: Exception) {
                // Ignore network error
            } finally {
                _isRefreshingPrices.value = false
            }
        }
    }

    fun sendTestReminder() {
        NotificationHelper.showNotification(
            getApplication(),
            "Час прийняти ліки (Тест)",
            "Парацетамол - Таблетки 500 мг (після їжі)",
            9999L,
            NotificationHelper.CHANNEL_REMINDERS
        )
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val database = PillshelfDatabase.getInstance(application)
                    val medRepo = MedicationRepository(database.medicationDao(), database.categoryDao())
                    val intakeRepo = IntakeRepository(database.intakeHistoryDao(), database.medicationDao())
                    val priceRepo = PriceRepository(database.priceHistoryDao())
                    return PillshelfViewModel(application, medRepo, intakeRepo, priceRepo) as T
                }
            }
    }
}
