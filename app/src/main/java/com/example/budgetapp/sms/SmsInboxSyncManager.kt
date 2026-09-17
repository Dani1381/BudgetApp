package com.example.budgetapp.sms

import android.content.Context
import android.net.Uri
import com.example.budgetapp.data.AppDatabase
import com.example.budgetapp.data.BankCard
import com.example.budgetapp.data.Transaction
import com.example.budgetapp.logger.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

object SmsInboxSyncManager {

    suspend fun syncHistoricalBankSms(context: Context, maxMessages: Int = 100): Int = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("_id", "address", "body", "date")

        val db = AppDatabase.getDatabase(context)
        val dao = db.transactionDao()
        val cardDao = db.bankCardDao()

        val existingTransactions = dao.getAllTransactions().first()
        val existingSignatures = existingTransactions.map { "${it.title}_${it.amount.toLong()}" }.toSet()

        var importedCount = 0

        try {
            val cursor = contentResolver.query(
                uri,
                projection,
                null,
                null,
                "date DESC LIMIT $maxMessages"
            )

            cursor?.use {
                val bodyIndex = it.getColumnIndexOrThrow("body")
                val addressIndex = it.getColumnIndexOrThrow("address")
                val dateIndex = it.getColumnIndexOrThrow("date")

                while (it.moveToNext()) {
                    val body = it.getString(bodyIndex) ?: continue
                    val address = it.getString(addressIndex) ?: ""
                    val smsDate = it.getLong(dateIndex)

                    val parsed = BankSmsParser.parse(body, address)
                    if (parsed != null) {
                        val signature = "${parsed.description}_${parsed.amount.toLong()}"
                        val cardKey = parsed.cardOrAccount ?: parsed.bankName

                        if (!existingSignatures.contains(signature)) {
                            val transaction = Transaction(
                                title = parsed.description,
                                amount = parsed.amount,
                                category = if (parsed.isIncome) "درآمد بانکی" else "هزینه بانکی",
                                date = smsDate,
                                isIncome = parsed.isIncome,
                                cardRef = cardKey
                            )
                            dao.insertTransaction(transaction)
                            importedCount++
                        }

                        // Also initialize or update card if newer
                        val existingCard = cardDao.getCardByNumber(cardKey)
                        if (existingCard == null || (parsed.balance != null && smsDate > existingCard.updatedAt)) {
                            val card = BankCard(
                                cardNumber = cardKey,
                                bankName = parsed.bankName,
                                balance = parsed.balance ?: (if (parsed.isIncome) parsed.amount else 0.0),
                                cardColorHex = parsed.cardColorHex,
                                updatedAt = smsDate
                            )
                            cardDao.insertOrUpdateCard(card)
                        }
                    }
                }
            }
            AppLogger.log(context, "INBOX_SYNC", "Successfully scanned inbox. Newly imported: $importedCount")
        } catch (e: Exception) {
            AppLogger.log(context, "INBOX_SYNC_ERROR", "Error scanning SMS inbox: ${e.message}")
        }

        importedCount
    }
}
