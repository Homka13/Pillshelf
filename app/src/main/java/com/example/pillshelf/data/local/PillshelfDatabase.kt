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

@Database(
    entities = [
        Medication::class,
        IntakeHistory::class,
        PriceHistory::class,
        Category::class
    ],
    version = 2,
    exportSchema = true
)
abstract class PillshelfDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun intakeHistoryDao(): IntakeHistoryDao
    abstract fun priceHistoryDao(): PriceHistoryDao
    abstract fun categoryDao(): CategoryDao

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
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Перше відкриття: сідимо ЛИШЕ стандартні категорії.
                            CoroutineScope(Dispatchers.IO).launch {
                                seedInitialData(getInstance(context.applicationContext))
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Початкові дані: ЛИШЕ стандартні категорії — жодних вигаданих
         * препаратів, цін чи прийомів. У медичному застосунку демо-дані,
         * що виглядають як реальні, неприпустимі: користувач має бачити
         * порожню аптечку і додати свої ліки сам.
         */
        suspend fun seedInitialData(database: PillshelfDatabase) {
            val categories = listOf(
                Category(name = "Аптечка", color = "#0D9488", icon = "ic_category_firstaid", sortOrder = 0),
                Category(name = "Знеболювальні", color = "#E11D48", icon = "ic_category_pain", sortOrder = 1),
                Category(name = "Вітаміни", color = "#D97706", icon = "ic_category_vitamins", sortOrder = 2),
                Category(name = "Травлення", color = "#059669", icon = "ic_category_digestive", sortOrder = 3),
                Category(name = "Рецептурні", color = "#7C3AED", icon = "ic_category_rx", sortOrder = 4),
                Category(name = "Протизастудні", color = "#2563EB", icon = "ic_category_cold", sortOrder = 5)
            )
            database.categoryDao().insertAll(categories)
        }
    }
}
