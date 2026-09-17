package com.example.budgetapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BankCardDao {
    @Query("SELECT * FROM bank_cards ORDER BY updatedAt DESC")
    fun getAllCards(): Flow<List<BankCard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCard(card: BankCard)

    @Query("SELECT * FROM bank_cards WHERE cardNumber = :cardNum LIMIT 1")
    suspend fun getCardByNumber(cardNum: String): BankCard?

    @Query("DELETE FROM bank_cards WHERE cardNumber = :cardNum")
    suspend fun deleteCard(cardNum: String)

    @Query("SELECT SUM(balance) FROM bank_cards")
    fun getTotalCardsBalance(): Flow<Double?>
}
