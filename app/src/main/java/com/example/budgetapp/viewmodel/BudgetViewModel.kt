package com.example.budgetapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetapp.data.AppDatabase
import com.example.budgetapp.data.BankCard
import com.example.budgetapp.data.Transaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val transactionDao = db.transactionDao()
    private val bankCardDao = db.bankCardDao()

    val transactions: StateFlow<List<Transaction>> = transactionDao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cards: StateFlow<List<BankCard>> = bankCardDao.getAllCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalIncome: StateFlow<Double> = transactionDao.getTotalIncome()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = transactionDao.getTotalExpense()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalCardsBalance: StateFlow<Double> = bankCardDao.getTotalCardsBalance()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun addTransaction(title: String, amount: Double, category: String, isIncome: Boolean, cardRef: String = "") {
        viewModelScope.launch {
            val transaction = Transaction(
                title = title,
                amount = amount,
                category = category,
                date = System.currentTimeMillis(),
                isIncome = isIncome,
                cardRef = cardRef
            )
            transactionDao.insertTransaction(transaction)

            // If a specific card was selected, update its balance
            if (cardRef.isNotBlank()) {
                val existingCard = bankCardDao.getCardByNumber(cardRef)
                if (existingCard != null) {
                    val newBal = if (isIncome) existingCard.balance + amount else existingCard.balance - amount
                    bankCardDao.insertOrUpdateCard(existingCard.copy(balance = newBal, updatedAt = System.currentTimeMillis()))
                }
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionDao.deleteTransaction(transaction)
        }
    }

    fun addOrUpdateCard(cardNumber: String, bankName: String, balance: Double, colorHex: String) {
        viewModelScope.launch {
            val card = BankCard(
                cardNumber = cardNumber,
                bankName = bankName,
                balance = balance,
                cardColorHex = colorHex,
                updatedAt = System.currentTimeMillis()
            )
            bankCardDao.insertOrUpdateCard(card)
        }
    }

    fun deleteCard(cardNumber: String) {
        viewModelScope.launch {
            bankCardDao.deleteCard(cardNumber)
        }
    }
}
