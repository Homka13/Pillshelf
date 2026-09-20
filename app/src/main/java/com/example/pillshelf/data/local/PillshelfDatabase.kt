package com.example.pillshelf.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.pillshelf.data.model.Category
import com.example.pillshelf.data.model.IntakeHistory
import com.example.pillshelf.data.model.Medication
import com.example.pillshelf.data.model.PriceHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

@Database(
    entities = [
        Medication::class,
        IntakeHistory::class,
        PriceHistory::class,
        Category::class
    ],
    version = 2,
    exportSchema = false
)
abstract class PillshelfDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun intakeHistoryDao(): IntakeHistoryDao
    abstract fun priceHistoryDao(): PriceHistoryDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: PillshelfDatabase? = null

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Non-destructive migration ensuring data preservation
            }
        }

        fun getInstance(context: Context): PillshelfDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PillshelfDatabase::class.java,
                    "pillshelf_database"
                )
                    .addCallback(DatabaseCallback(context.applicationContext))
                    .addMigrations(MIGRATION_1_2)
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
                CoroutineScope(Dispatchers.IO).launch {
                    val database = getInstance(context)
                    seedInitialData(database)
                }
            }
        }

        suspend fun seedInitialData(database: PillshelfDatabase) {
            val medDao = database.medicationDao()
            val categoryDao = database.categoryDao()
            val priceDao = database.priceHistoryDao()
            val intakeDao = database.intakeHistoryDao()

            val today = LocalDate.now()
            val todayEpochDay = today.toEpochDay()
            val nowMillis = System.currentTimeMillis()

            // 1. Seed Categories
            val categories = listOf(
                Category(name = "Аптечка", color = "#0D9488", icon = "ic_category_firstaid", sortOrder = 0),
                Category(name = "Знеболювальні", color = "#E11D48", icon = "ic_category_pain", sortOrder = 1),
                Category(name = "Вітаміни", color = "#D97706", icon = "ic_category_vitamins", sortOrder = 2),
                Category(name = "Травлення", color = "#059669", icon = "ic_category_digestive", sortOrder = 3),
                Category(name = "Рецептурні", color = "#7C3AED", icon = "ic_category_rx", sortOrder = 4),
                Category(name = "Протизастудні", color = "#2563EB", icon = "ic_category_cold", sortOrder = 5)
            )
            categoryDao.insertAll(categories)

            // 2. Seed realistic Ukrainian medications from SPEC
            val med1Id = medDao.insert(
                Medication(
                    name = "Парацетамол",
                    activeSubstance = "Paracetamol",
                    dosageForm = "Таблетки 500 мг",
                    manufacturer = "Київмедпрепарат",
                    category = "Аптечка",
                    totalQuantity = 30,
                    remainingQuantity = 14,
                    expiryDate = today.plusMonths(14).toString(),
                    expiryDateEpochDays = today.plusMonths(14).toEpochDay(),
                    scheduleType = "DAILY",
                    timeOfDay = "MORNING,EVENING",
                    takeBeforeMeal = false,
                    notes = "Запити склянкою води після їжі",
                    trackPrices = true,
                    targetPrice = 35.0,
                    colorHex = 0xFF0D9488
                )
            )

            val med2Id = medDao.insert(
                Medication(
                    name = "Ібупрофен 400",
                    activeSubstance = "Ibuprofen",
                    dosageForm = "Капсули 400 мг",
                    manufacturer = "Фармак",
                    category = "Знеболювальні",
                    totalQuantity = 20,
                    remainingQuantity = 4, // low stock! <= 5
                    expiryDate = today.plusMonths(18).toString(),
                    expiryDateEpochDays = today.plusMonths(18).toEpochDay(),
                    scheduleType = "EVERY_N_HOURS",
                    intervalHours = 8,
                    timeOfDay = "MORNING,AFTERNOON,EVENING",
                    takeBeforeMeal = false,
                    notes = "Приймати суворо після їжі. Не комбінувати з аспірином!",
                    trackPrices = true,
                    targetPrice = 75.0,
                    colorHex = 0xFFE11D48
                )
            )

            val med3Id = medDao.insert(
                Medication(
                    name = "Вітамін D3 2000 МО",
                    activeSubstance = "Cholecalciferol",
                    dosageForm = "Капсули",
                    manufacturer = "Олідетрим",
                    category = "Вітаміни",
                    totalQuantity = 60,
                    remainingQuantity = 48,
                    expiryDate = today.plusDays(24).toString(), // expiring soon! < 30 days
                    expiryDateEpochDays = today.plusDays(24).toEpochDay(),
                    scheduleType = "DAILY",
                    timeOfDay = "MORNING",
                    takeBeforeMeal = false,
                    notes = "Приймати під час сніданку з жирною їжею",
                    trackPrices = true,
                    targetPrice = 190.0,
                    colorHex = 0xFFD97706
                )
            )

            val med4Id = medDao.insert(
                Medication(
                    name = "Панкреатин 8000",
                    activeSubstance = "Pancreatin",
                    dosageForm = "Таблетки",
                    manufacturer = "Здоров'я",
                    category = "Травлення",
                    totalQuantity = 50,
                    remainingQuantity = 2, // low stock!
                    expiryDate = today.plusMonths(12).toString(),
                    expiryDateEpochDays = today.plusMonths(12).toEpochDay(),
                    scheduleType = "DAILY",
                    timeOfDay = "AFTERNOON,EVENING",
                    takeBeforeMeal = true,
                    notes = "Приймати безпосередньо перед або під час їжі",
                    trackPrices = true,
                    targetPrice = 60.0,
                    colorHex = 0xFF059669
                )
            )

            val med5Id = medDao.insert(
                Medication(
                    name = "Амоксицилін 500",
                    activeSubstance = "Amoxicillin",
                    dosageForm = "Таблетки 500 мг",
                    manufacturer = "Дарниця",
                    category = "Рецептурні",
                    totalQuantity = 20,
                    remainingQuantity = 16,
                    expiryDate = today.plusMonths(10).toString(),
                    expiryDateEpochDays = today.plusMonths(10).toEpochDay(),
                    scheduleType = "COURSE",
                    courseStartDate = today.toString(),
                    courseDurationDays = 7,
                    intervalHours = 8,
                    timeOfDay = "MORNING,AFTERNOON,EVENING",
                    takeBeforeMeal = false,
                    notes = "Приймати через рівні проміжки (кожні 8 год). Не переривати курс!",
                    trackPrices = false,
                    targetPrice = 0.0,
                    colorHex = 0xFF7C3AED
                )
            )

            // 3. Seed initial Intake history (no fabricated pharmacy prices)
            val intakes = listOf(
                IntakeHistory(
                    medicationId = med1Id,
                    medicationName = "Парацетамол",
                    dosageForm = "Таблетки 500 мг",
                    intakeTime = nowMillis - 3600000 * 3,
                    actualTime = nowMillis - 3600000 * 3,
                    taken = true,
                    notes = "Ранковий прийом після їжі"
                ),
                IntakeHistory(
                    medicationId = med3Id,
                    medicationName = "Вітамін D3 2000 МО",
                    dosageForm = "Капсули",
                    intakeTime = nowMillis - 3600000 * 2,
                    actualTime = nowMillis - 3600000 * 2,
                    taken = true,
                    notes = "Разом зі сніданком"
                )
            )
            for (intake in intakes) {
                intakeDao.insert(intake)
            }
        }
    }
}
