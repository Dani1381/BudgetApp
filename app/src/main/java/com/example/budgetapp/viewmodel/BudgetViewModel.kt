package com.example.budgetapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetapp.data.AppDatabase
import com.example.budgetapp.data.Transaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BudgetViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).transactionDao()

    val transactions: StateFlow<List<Transaction>> = dao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalIncome: StateFlow<Double> = dao.getTotalIncome()
        .combine(dao.getAllTransactions()) { income, _ -> income ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = dao.getTotalExpense()
        .combine(dao.getAllTransactions()) { expense, _ -> expense ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun addTransaction(title: String, amount: Double, category: String, isIncome: Boolean) {
        viewModelScope.launch {
            val transaction = Transaction(
                title = title,
                amount = amount,
                category = category,
                date = System.currentTimeMillis(),
                isIncome = isIncome
            )
            dao.insertTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            dao.deleteTransaction(transaction)
        }
    }
}
