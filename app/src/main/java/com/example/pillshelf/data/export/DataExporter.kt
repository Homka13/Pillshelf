package com.example.pillshelf.data.export

import android.content.Context
import android.net.Uri
import com.example.pillshelf.data.model.IntakeHistory
import com.example.pillshelf.data.local.PillshelfDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Експорт даних у файл.
 *
 * Контекст: allowBackup="false" — хмарної копії немає, тож це ЄДИНИЙ
 * спосіб для користувача не втратити історію при зміні/втраті телефону.
 * Формат: JSON (структурований, придатний для майбутнього імпорту) —
 * CSV обмежений одним масивом, а в нас дві сутності різної форми.
 */
class DataExporter(private val database: PillshelfDatabase) {

    companion object {
        const val FORMAT_VERSION = 1
        const val SCHEMA_NAME = "pillshelf_export"
    }

    /** Повний експорт: аптечка + журнал прийомів + історія цін. */
    suspend fun buildExportJson(): String = withContext(Dispatchers.IO) {
        val medications = database.medicationDao().getAllMedicationsSync()
        val intakes = database.intakeHistoryDao().getAllHistorySync()

        val root = JSONObject()
        root.put("schema", SCHEMA_NAME)
        root.put("formatVersion", FORMAT_VERSION)
        root.put("exportedAt", Instant.now().toString())
        root.put("medications", JSONArray().apply {
            medications.forEach { med ->
                put(
                    JSONObject().apply {
                        put("id", med.id)
                        put("name", med.name)
                        put("activeSubstance", med.activeSubstance)
                        put("dosageForm", med.dosageForm)
                        put("manufacturer", med.manufacturer)
                        put("category", med.category)
                        put("totalQuantity", med.totalQuantity)
                        put("remainingQuantity", med.remainingQuantity)
                        put("expiryDate", med.expiryDate)
                        put("scheduleType", med.scheduleType)
                        put("intervalHours", med.intervalHours)
                        put("timeOfDay", med.timeOfDay)
                        put("courseStartDate", med.courseStartDate)
                        put("courseDurationDays", med.courseDurationDays)
                        put("takeBeforeMeal", med.takeBeforeMeal)
                        put("notes", med.notes)
                        put("trackPrices", med.trackPrices)
                        put("targetPrice", med.targetPrice)
                        put("colorHex", med.colorHex)
                        put("createdAt", med.createdAt)
                        put("updatedAt", med.updatedAt)
                    }
                )
            }
        })
        root.put("intakeHistory", JSONArray().apply {
            intakes.forEach { intake ->
                put(
                    JSONObject().apply {
                        put("id", intake.id)
                        put("medicationName", intake.medicationName)
                        put("dosageForm", intake.dosageForm)
                        put("intakeTime", intake.intakeTime)
                        put("actualTime", intake.actualTime)
                        put("taken", intake.taken)
                        put("notes", intake.notes)
                    }
                )
            }
        })
        root.toString(2)
    }

    /**
     * Записує експорт у вибраний користувачем файл (SAF).
     * @return кількість записаних байтів.
     * @throws IOException якщо запис не вдався.
     */
    @Throws(IOException::class)
    suspend fun exportToUri(uri: Uri, context: Context): Int = withContext(Dispatchers.IO) {
        val json = buildExportJson()
        context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
            stream.write(json.toByteArray(Charsets.UTF_8))
            stream.flush()
        } ?: throw IOException("Не вдалося відкрити файл для запису")
        json.toByteArray(Charsets.UTF_8).size
    }

    /** Ім'я файлу за замовчуванням для CreateDocument. */
    fun defaultFileName(): String {
        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")
            .withZone(ZoneId.systemDefault())
            .format(Instant.now())
        return "pillshelf_export_$stamp.json"
    }

    /** Кількість записів для показу в UI після експорту. */
    suspend fun exportSummary(): String = withContext(Dispatchers.IO) {
        val meds = database.medicationDao().getAllMedicationsSync().size
        val intakes = database.intakeHistoryDao().getAllHistorySync().size
        "$meds препаратів, $intakes записів журналу"
    }
}
