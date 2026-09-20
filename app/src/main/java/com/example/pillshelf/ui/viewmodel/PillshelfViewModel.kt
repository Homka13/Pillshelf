package com.example.pillshelf.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pillshelf.data.export.DataExporter
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
import com.example.pillshelf.service.ReminderScheduler
import com.example.pillshelf.service.SettingsRepository
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
    val historyId: Long? = null,
    val loggedTimestamp: Long? = null,
    // Час слота сьогодні (для DAILY) — основа для «прострочено» і відмітки заднім числом
    val scheduledAtMillis: Long? = null,
    // Слот минув, а прийом так і не відмітили — показуємо «Прострочено»
    val isOverdue: Boolean = false
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
    // null = реальних записів немає: жодних вигаданих трендів
    val trendResult: AnalyzePriceTrendUseCase.TrendResult?,
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

        for (med in meds) {
            when (med.scheduleType) {
                "DAILY" -> {
                    val timeSlots = med.getTimeOfDayList()
                    for (slot in timeSlots) {
                        val timeStr = when (slot.uppercase()) {
                            "MORNING" -> "08:00"
                            "AFTERNOON" -> "13:00"
                            "EVENING" -> "20:00"
                            "BEDTIME" -> "22:00"
                            else -> "08:00"
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

                        // Пропущена доза: слот минув більше ніж GRACE_MINUTES тому,
                        // а прийом так і не відмітили — не даємо їй зникнути мовчки.
                        val hour = timeStr.substringBefore(":").toInt()
                        val minute = timeStr.substringAfter(":").toInt()
                        val slotMillis = todayLocalDate
                            .atTime(hour, minute)
                            .atZone(ZoneId.systemDefault())
                            .toInstant().toEpochMilli()
                        val graceMillis = com.example.pillshelf.service.ReminderScheduler.GRACE_MINUTES * 60_000
                        val overdue = match == null &&
                            slotMillis + graceMillis < System.currentTimeMillis()

                        items.add(
                            ScheduledDoseItem(
                                medication = med,
                                scheduledTime = timeStr,
                                timeSlotLabel = slotLabel,
                                isTaken = match?.taken == true,
                                isSkipped = match?.taken == false,
                                historyId = match?.id,
                                loggedTimestamp = match?.actualTime,
                                scheduledAtMillis = slotMillis,
                                isOverdue = overdue
                            )
                        )
                    }
                }
                "EVERY_N_HOURS" -> {
                    val interval = if (medicationIntervalHours(med) > 0) medicationIntervalHours(med) else 8
                    val slotLabel = "Кожні $interval год."
                    val match = todayIntakes.firstOrNull { it.medicationId == med.id }

                    items.add(
                        ScheduledDoseItem(
                            medication = med,
                            scheduledTime = "Кожні ${interval}г",
                            timeSlotLabel = slotLabel,
                            isTaken = match?.taken == true,
                            isSkipped = match?.taken == false,
                            historyId = match?.id,
                            loggedTimestamp = match?.actualTime
                        )
                    )
                }
                "COURSE" -> {
                    val match = todayIntakes.firstOrNull { it.medicationId == med.id }
                    items.add(
                        ScheduledDoseItem(
                            medication = med,
                            scheduledTime = "08:00, 20:00",
                            timeSlotLabel = "Курс лікування (${med.courseDurationDays} дн.)",
                            isTaken = match?.taken == true,
                            isSkipped = match?.taken == false,
                            historyId = match?.id,
                            loggedTimestamp = match?.actualTime
                        )
                    )
                }
                else -> {
                    val match = todayIntakes.firstOrNull { it.medicationId == med.id }
                    items.add(
                        ScheduledDoseItem(
                            medication = med,
                            scheduledTime = "08:00",
                            timeSlotLabel = "За розкладом",
                            isTaken = match?.taken == true,
                            isSkipped = match?.taken == false,
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
        // Історія цін: показуємо ТІЛЬКИ реально отримані записи.
        // Застосунок не синтезує ціни й тренди: якщо даних немає —
        // UI чесно показує «ціни недоступні» (див. PricesScreen).
        viewModelScope.launch(Dispatchers.IO) {
            allMedications.collect { meds ->
                val trackedMeds = meds.filter { it.trackPrices }
                val db = PillshelfDatabase.getInstance(getApplication())

                val list = mutableListOf<TrackedMedicationPrice>()
                for (med in trackedMeds) {
                    val history = db.priceHistoryDao().getHistoryForMedicationSync(med.id)
                    val best = history.minByOrNull { it.price }
                    list.add(
                        TrackedMedicationPrice(
                            medication = med,
                            latestPrices = history.take(6),
                            trendResult = if (history.isNotEmpty()) analyzePriceTrendUseCase.analyze(history)
                            else null,
                            bestPrice = best?.price ?: 0.0,
                            bestPharmacy = best?.pharmacyName ?: ""
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

            // If price tracking is enabled, immediately fetch initial prices
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
            medicationRepository.updateMedication(medication.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch(Dispatchers.IO) {
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

            // Плануємо наступний точний будильник (графік рухається від факту прийому)
            ReminderScheduler.scheduleNextFor(getApplication(), medication, lastIntake = java.time.LocalDateTime.now())

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

    /**
     * «Відкласти» на +15 хвилин: точний будильник на запланований час + 15 хв.
     */
    fun snoozeDose(medication: Medication) {
        viewModelScope.launch(Dispatchers.IO) {
            val next = calculateNextIntakeUseCase.calculateNextIntake(medication) ?: return@launch
            val triggerAt = next.plusMinutes(SNOOZE_MINUTES)
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli()
            ReminderScheduler.schedule(getApplication(), medication.id, triggerAt)
            com.example.pillshelf.widget.DoseWidget.updateAll(getApplication())
        }
    }

    /**
     * Відмітка прийому заднім числом: людина випила ліки раніше,
     * а кнопку натиснула пізніше. Запис у журналі отримує реальний час прийому.
     */
    fun recordIntakeRetroactive(medication: Medication, taken: Boolean, intakeTimeMillis: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            intakeRepository.recordIntakeAt(
                medication = medication,
                taken = taken,
                intakeTimeMillis = intakeTimeMillis,
                notes = if (taken) "Прийнято (відмічено заднім числом)" else "Пропущено (відмічено заднім числом)"
            )
        }
    }

    /** Чи дозволені точні будильники (стан для налаштувань/графіку). */
    fun isExactAlarmsAllowed(): Boolean =
        ReminderScheduler.canScheduleExact(getApplication())

    /** Інтент на системний екран запиту точних будильників; null — не потрібен. */
    fun exactAlarmSettingsIntent(): Intent? =
        ReminderScheduler.exactAlarmSettingsIntent(getApplication())

    /** Моніторинг цін: за замовчуванням вимкнений, вмикається лише вручну. */
    private val _priceMonitoringEnabled =
        MutableStateFlow(SettingsRepository.isPriceMonitoringEnabled(application))
    val priceMonitoringEnabled: StateFlow<Boolean> = _priceMonitoringEnabled.asStateFlow()

    fun setPriceMonitoring(enabled: Boolean) {
        SettingsRepository.setPriceMonitoringEnabled(getApplication(), enabled)
        _priceMonitoringEnabled.value = enabled
    }

    // ── Експорт даних (єдиний бекап: allowBackup="false") ───────────────────

    private val dataExporter by lazy { DataExporter(PillshelfDatabase.getInstance(getApplication())) }

    fun exportFileName(): String = dataExporter.defaultFileName()

    fun exportSummary(onReady: (String) -> Unit) {
        viewModelScope.launch {
            onReady(dataExporter.exportSummary())
        }
    }

    fun exportDataTo(uri: Uri, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            try {
                dataExporter.exportToUri(uri, getApplication())
                onResult(Result.success(dataExporter.exportSummary()))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun restockMedication(medication: Medication, addedAmount: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            medicationRepository.incrementRemainingQuantity(medication.id, addedAmount)
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
                    val best = history.minByOrNull { it.price }
                    list.add(
                        TrackedMedicationPrice(
                            medication = med,
                            latestPrices = history.take(6),
                            trendResult = if (history.isNotEmpty()) analyzePriceTrendUseCase.analyze(history)
                            else null,
                            bestPrice = best?.price ?: 0.0,
                            bestPharmacy = best?.pharmacyName ?: ""
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
        private const val SNOOZE_MINUTES = 15L

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
