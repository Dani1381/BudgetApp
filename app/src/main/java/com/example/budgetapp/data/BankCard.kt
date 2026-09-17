package com.example.budgetapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bank_cards")
data class BankCard(
    @PrimaryKey val cardNumber: String, // last 4 digits e.g. "5022" or bank name
    val bankName: String,
    val cardHolder: String = "سید دانیال علوی",
    val balance: Double = 0.0,
    val cardColorHex: String = "#1E293B",
    val updatedAt: Long = System.currentTimeMillis()
)
