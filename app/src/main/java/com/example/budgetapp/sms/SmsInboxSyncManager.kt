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
        var totalBankSmsScanned = 0

        try {
            AppLogger.log(context, "INBOX_SYNC_START", "Scanning up to $maxMessages SMS from inbox...")
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
                        totalBankSmsScanned++
                        val signature = "${parsed.description}_${parsed.amount.toLong()}"

                        AppLogger.log(
                            context,
                            "INBOX_SMS_ITEM",
                            "Bank: ${parsed.bankName} | CardKey: ${parsed.canonicalCardKey} | Amount: ${parsed.amount} | Date: $smsDate | Body: $body"
                        )

                        if (!existingSignatures.contains(signature)) {
                            val transaction = Transaction(
                                title = parsed.description,
                                amount = parsed.amount,
                                category = if (parsed.isIncome) "درآمد بانکی" else "هزینه بانکی",
                                date = smsDate,
                                isIncome = parsed.isIncome,
                                cardRef = parsed.displayCardNumber
                            )
                            dao.insertTransaction(transaction)
                            importedCount++
                        }

                        // Normalize card - update balance only if newer
                        val existingCard = cardDao.getCardByNumber(parsed.canonicalCardKey)
                        if (existingCard == null || (parsed.balance != null && smsDate > existingCard.updatedAt)) {
                            val card = BankCard(
                                cardNumber = parsed.canonicalCardKey,
                                bankName = parsed.bankName,
                                cardHolder = "•••• ${parsed.displayCardNumber}",
                                balance = parsed.balance ?: (if (parsed.isIncome) parsed.amount else 0.0),
                                cardColorHex = parsed.cardColorHex,
                                updatedAt = smsDate
                            )
                            cardDao.insertOrUpdateCard(card)
                        }
                    }
                }
            }
            AppLogger.log(
                context,
                "INBOX_SYNC_COMPLETE",
                "Finished scanning. Bank SMS found: $totalBankSmsScanned | Newly imported: $importedCount"
            )
        } catch (e: Exception) {
            AppLogger.log(context, "INBOX_SYNC_ERR", "Error reading SMS inbox: ${e.message}", e)
        }

        importedCount
    }
}
