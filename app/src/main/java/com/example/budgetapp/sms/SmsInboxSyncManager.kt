package com.example.budgetapp.sms

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import com.example.budgetapp.data.AppDatabase
import com.example.budgetapp.data.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

object SmsInboxSyncManager {

    /**
     * Reads existing bank SMS messages from the inbox and imports any unimported transactions.
     * Returns the count of newly imported transactions.
     */
    suspend fun syncHistoricalBankSms(context: Context, maxMessages: Int = 100): Int = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("_id", "address", "body", "date")

        val db = AppDatabase.getDatabase(context)
        val dao = db.transactionDao()

        // Fetch existing transaction titles & dates to avoid duplicating transactions
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
                        if (!existingSignatures.contains(signature)) {
                            val transaction = Transaction(
                                title = parsed.description,
                                amount = parsed.amount,
                                category = if (parsed.isIncome) "درآمد بانکی" else "هزینه بانکی",
                                date = smsDate,
                                isIncome = parsed.isIncome
                            )
                            dao.insertTransaction(transaction)
                            importedCount++
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        importedCount
    }
}
