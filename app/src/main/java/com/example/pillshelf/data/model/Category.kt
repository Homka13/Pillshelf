package com.example.pillshelf.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,             // "Аптечка", "Вітаміни", "Знеболювальні"

    val color: String = "#0D9488",// hex color

    val icon: String = "ic_category_medicine",

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0        // 0, 1, 2...
)
