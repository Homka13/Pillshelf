package com.example.pillshelf.domain.usecase

import com.example.pillshelf.data.model.Medication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckMedicationInteractionsUseCaseTest {

    private val useCase = CheckMedicationInteractionsUseCase()

    private fun createMed(id: Long, name: String, activeSubstance: String): Medication {
        return Medication(
            id = id,
            name = name,
            activeSubstance = activeSubstance,
            dosageForm = "Таблетки 500 мг",
            category = "Аптечка",
            expiryDate = "2026-12-31",
            totalQuantity = 20,
            remainingQuantity = 10,
            scheduleType = "AS_NEEDED",
            timeOfDay = "MORNING",
            intervalHours = 8,
            courseDurationDays = 7,
            courseStartDate = "",
            takeBeforeMeal = false,
            notes = "",
            trackPrices = false,
            targetPrice = 0.0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    @Test
    fun testEmptyMedicationsProducesNoWarnings() {
        val warnings = useCase.checkInteractions(emptyList())
        assertTrue(warnings.isEmpty())
    }

    @Test
    fun testDuplicateParacetamolDetection() {
        val meds = listOf(
            createMed(1, "Парацетамол-Дарниця", "Paracetamol"),
            createMed(2, "Терафлю", "Paracetamol, Phenylephrine")
        )
        val warnings = useCase.checkInteractions(meds)
        assertEquals(1, warnings.size)
        assertTrue(warnings.first().title.contains("парацетамол", ignoreCase = true))
    }

    @Test
    fun testDuplicateNsaidDetection() {
        val meds = listOf(
            createMed(1, "Ібупрофен", "Ibuprofen"),
            createMed(2, "Диклофенак", "Diclofenac sodium")
        )
        val warnings = useCase.checkInteractions(meds)
        assertEquals(1, warnings.size)
        assertTrue(warnings.first().title.contains("НПЗП", ignoreCase = true))
    }

    @Test
    fun testUnrelatedMedicationsProduceNoWarnings() {
        val meds = listOf(
            createMed(1, "Вітамін C", "Ascorbic acid"),
            createMed(2, "Панкреатин", "Pancreatin")
        )
        val warnings = useCase.checkInteractions(meds)
        assertTrue(warnings.isEmpty())
    }
}
