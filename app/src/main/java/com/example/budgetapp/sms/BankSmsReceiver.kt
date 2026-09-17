package com.example.budgetapp.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.widget.Toast
import com.example.budgetapp.data.AppDatabase
import com.example.budgetapp.data.BankCard
import com.example.budgetapp.data.Transaction
import com.example.budgetapp.logger.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BankSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val body = sms.displayMessageBody ?: continue
                val sender = sms.displayOriginatingAddress ?: ""

                AppLogger.log(context, "SMS_INCOMING", "From: $sender | Length: ${body.length}\n$body")

                val parsed = BankSmsParser.parse(body, sender)
                if (parsed != null) {
                    AppLogger.log(context, "SMS_PARSED", "Bank: ${parsed.bankName}, Key: ${parsed.canonicalCardKey}, Amount: ${parsed.amount}, Bal: ${parsed.balance}")

                    val db = AppDatabase.getDatabase(context)
                    val dao = db.transactionDao()
                    val cardDao = db.bankCardDao()

                    CoroutineScope(Dispatchers.IO).launch {
                        // 1. Insert Transaction
                        val transaction = Transaction(
                            title = parsed.description,
                            amount = parsed.amount,
                            category = if (parsed.isIncome) "درآمد بانکی" else "هزینه بانکی",
                            date = System.currentTimeMillis(),
                            isIncome = parsed.isIncome,
                            cardRef = parsed.displayCardNumber
                        )
                        dao.insertTransaction(transaction)

                        // 2. Update/Create normalized BankCard
                        val existingCard = cardDao.getCardByNumber(parsed.canonicalCardKey)
                        val updatedBalance = if (parsed.balance != null) {
                            parsed.balance
                        } else {
                            val cur = existingCard?.balance ?: 0.0
                            if (parsed.isIncome) cur + parsed.amount else cur - parsed.amount
                        }

                        val card = BankCard(
                            cardNumber = parsed.canonicalCardKey,
                            bankName = parsed.bankName,
                            cardHolder = "•••• ${parsed.displayCardNumber}",
                            balance = updatedBalance,
                            cardColorHex = parsed.cardColorHex,
                            updatedAt = System.currentTimeMillis()
                        )
                        cardDao.insertOrUpdateCard(card)
                        AppLogger.log(context, "CARD_UPDATED", "Card ${parsed.canonicalCardKey} balance updated to: $updatedBalance")
                    }

                    val typeStr = if (parsed.isIncome) "واریز" else "برداشت"
                    val amountStr = String.format("%,d", parsed.amount.toLong())
                    Toast.makeText(
                        context,
                        "💳 $typeStr $amountStr تومان • ${parsed.bankName}",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    AppLogger.log(context, "SMS_IGNORED", "Not recognized as bank SMS: $body")
                }
            }
        }
    }
}
