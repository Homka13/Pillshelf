package com.example.pillshelf.domain.usecase

import com.example.pillshelf.data.model.Medication
import com.example.pillshelf.domain.model.InteractionWarning

class CheckMedicationInteractionsUseCase {

    fun checkInteractions(medications: List<Medication>): List<InteractionWarning> {
        val warnings = mutableListOf<InteractionWarning>()

        // 1. Check duplicate active substances (e.g., Paracetamol in multiple remedies)
        val paracetamolMeds = medications.filter {
            it.activeSubstance.contains("paracetamol", ignoreCase = true) ||
            it.name.contains("парацетамол", ignoreCase = true) ||
            it.name.contains("фервекс", ignoreCase = true) ||
            it.name.contains("терафлю", ignoreCase = true) ||
            it.name.contains("цитрамон", ignoreCase = true)
        }
        if (paracetamolMeds.size > 1) {
            warnings.add(
                InteractionWarning(
                    title = "Підказка: можливе дублювання парацетамолу",
                    description = "Кілька препаратів містять парацетамол (${paracetamolMeds.joinToString { it.name }}). Перевищення добової норми 4000 мг небезпечне для печінки.",
                    severity = InteractionWarning.Severity.HIGH,
                    conflictingMeds = paracetamolMeds.map { it.name }
                )
            )
        }

        // 2. Check multiple NSAIDs (НПЗП)
        val nsaidMeds = medications.filter {
            val s = (it.activeSubstance + " " + it.name).lowercase()
            s.contains("ibuprofen") || s.contains("ібупрофен") ||
            s.contains("aspirin") || s.contains("аспірин") ||
            s.contains("diclofenac") || s.contains("диклофенак") ||
            s.contains("ketoprofen") || s.contains("кетопрофен") ||
            s.contains("мелоксикам") || s.contains("німесулід")
        }
        if (nsaidMeds.size > 1) {
            warnings.add(
                InteractionWarning(
                    title = "Підказка: кілька НПЗП одночасно",
                    description = "Одночасний прийом кількох протизапальних засобів (${nsaidMeds.joinToString { it.name }}) значно підвищує ризик подразнення шлунка та кровотеч.",
                    severity = InteractionWarning.Severity.HIGH,
                    conflictingMeds = nsaidMeds.map { it.name }
                )
            )
        }

        // 3. Antibiotics and dairy / supplements reminder
        val antibiotics = medications.filter {
            val s = (it.activeSubstance + " " + it.name).lowercase()
            s.contains("amoxicillin") || s.contains("амоксицилін") ||
            s.contains("ciprofloxacin") || s.contains("ципрофлоксацин") ||
            s.contains("azithromycin") || s.contains("азитроміцин")
        }
        if (antibiotics.isNotEmpty()) {
            warnings.add(
                InteractionWarning(
                    title = "Підказка: правила прийому антибіотика",
                    description = "Для препарату ${antibiotics.first().name}: приймайте за розкладом через рівні проміжки часу та завершіть повний призначений курс лікаря.",
                    severity = InteractionWarning.Severity.INFO,
                    conflictingMeds = antibiotics.map { it.name }
                )
            )
        }

        return warnings
    }
}
