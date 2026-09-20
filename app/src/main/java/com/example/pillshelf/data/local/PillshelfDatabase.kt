package com.example.pillshelf.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.pillshelf.data.model.DoseLog
import com.example.pillshelf.data.model.Medication
import com.example.pillshelf.data.model.MedicationCategory
import com.example.pillshelf.data.model.MedicationForm
import com.example.pillshelf.data.model.ScheduleType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

@Database(
    entities = [Medication::class, DoseLog::class],
    version = 1,
    exportSchema = false
)
abstract class PillshelfDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun doseLogDao(): DoseLogDao

    companion object {
        @Volatile
        private var INSTANCE: PillshelfDatabase? = null

        fun getInstance(context: Context): PillshelfDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PillshelfDatabase::class.java,
                    "pillshelf_database"
                )
                    .addCallback(DatabaseCallback(context.applicationContext))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val context: Context
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Seed initial shelf items in background coroutine
                CoroutineScope(Dispatchers.IO).launch {
                    val database = getInstance(context)
                    val dao = database.medicationDao()
                    val today = LocalDate.now().toEpochDay()

                    val initialMeds = listOf(
                        Medication(
                            name = "Ibuprofen",
                            brandOrGeneric = "Advil / Motrin",
                            form = MedicationForm.TABLET,
                            strength = "200 mg",
                            category = MedicationCategory.PAIN_RELIEF,
                            stockQuantity = 28,
                            lowStockThreshold = 10,
                            unit = "tablets",
                            expiryDateEpochDays = today + 420,
                            storageLocation = "Medicine Cabinet",
                            instructions = "Take 1-2 tablets with food every 6-8 hours as needed for headache or muscle pain.",
                            scheduleType = ScheduleType.AS_NEEDED,
                            scheduledTimes = "",
                            colorHex = 0xFFE11D48
                        ),
                        Medication(
                            name = "Vitamin D3",
                            brandOrGeneric = "Nature Made",
                            form = MedicationForm.CAPSULE,
                            strength = "2000 IU",
                            category = MedicationCategory.DAILY_SUPPLEMENT,
                            stockQuantity = 52,
                            lowStockThreshold = 14,
                            unit = "softgels",
                            expiryDateEpochDays = today + 500,
                            storageLocation = "Kitchen Shelf",
                            instructions = "Take 1 softgel daily in the morning with a healthy fat-containing breakfast.",
                            scheduleType = ScheduleType.DAILY,
                            scheduledTimes = "08:00",
                            colorHex = 0xFFF59E0B
                        ),
                        Medication(
                            name = "Cetirizine HCl",
                            brandOrGeneric = "Zyrtec",
                            form = MedicationForm.TABLET,
                            strength = "10 mg",
                            category = MedicationCategory.ALLERGY_SINUS,
                            stockQuantity = 4, // Low stock trigger
                            lowStockThreshold = 8,
                            unit = "tablets",
                            expiryDateEpochDays = today + 260,
                            storageLocation = "Bathroom Shelf",
                            instructions = "Take 1 tablet in the evening. May cause mild drowsiness.",
                            scheduleType = ScheduleType.DAILY,
                            scheduledTimes = "21:00",
                            colorHex = 0xFF0D9488
                        ),
                        Medication(
                            name = "Amoxicillin",
                            brandOrGeneric = "Generic",
                            form = MedicationForm.CAPSULE,
                            strength = "500 mg",
                            category = MedicationCategory.PRESCRIPTION,
                            stockQuantity = 12,
                            lowStockThreshold = 5,
                            unit = "capsules",
                            expiryDateEpochDays = today + 25, // Expiring soon trigger
                            storageLocation = "Medicine Cabinet",
                            instructions = "Complete full course as directed by doctor. Take 1 capsule twice daily with a full glass of water.",
                            scheduleType = ScheduleType.TWICE_DAILY,
                            scheduledTimes = "08:00, 20:00",
                            colorHex = 0xFF3B82F6
                        ),
                        Medication(
                            name = "Omega-3 Fish Oil",
                            brandOrGeneric = "Nordic Naturals",
                            form = MedicationForm.CAPSULE,
                            strength = "1200 mg",
                            category = MedicationCategory.DAILY_SUPPLEMENT,
                            stockQuantity = 40,
                            lowStockThreshold = 10,
                            unit = "capsules",
                            expiryDateEpochDays = today + 320,
                            storageLocation = "Refrigerator",
                            instructions = "Take 1 capsule with noon meal.",
                            scheduleType = ScheduleType.DAILY,
                            scheduledTimes = "13:00",
                            colorHex = 0xFF0284C7
                        ),
                        Medication(
                            name = "Acetaminophen",
                            brandOrGeneric = "Tylenol Extra Strength",
                            form = MedicationForm.TABLET,
                            strength = "500 mg",
                            category = MedicationCategory.PAIN_RELIEF,
                            stockQuantity = 22,
                            lowStockThreshold = 6,
                            unit = "tablets",
                            expiryDateEpochDays = today + 600,
                            storageLocation = "Medicine Cabinet",
                            instructions = "Do not exceed 3000 mg in 24 hours. Avoid alcohol.",
                            scheduleType = ScheduleType.AS_NEEDED,
                            scheduledTimes = "",
                            colorHex = 0xFF8B5CF6
                        )
                    )
                    dao.insertAll(initialMeds)
                }
            }
        }
    }
}
