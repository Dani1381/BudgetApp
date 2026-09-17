package com.example.budgetapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val date: Long, // timestamp
    val isIncome: Boolean,
    val cardRef: String = "" // e.g. "5022" or "بلو بانک"
)
