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

                AppLogger.log(context, "SMS_RAW", "From: $sender | Body: $body")

                val parsed = BankSmsParser.parse(body, sender)
                if (parsed != null) {
                    AppLogger.log(context, "SMS_PARSED", "Bank: ${parsed.bankName}, Amount: ${parsed.amount}, Card: ${parsed.cardOrAccount}, Balance: ${parsed.balance}")

                    val db = AppDatabase.getDatabase(context)
                    val dao = db.transactionDao()
                    val cardDao = db.bankCardDao()

                    CoroutineScope(Dispatchers.IO).launch {
                        // 1. Insert Transaction
                        val cardKey = parsed.cardOrAccount ?: parsed.bankName
                        val transaction = Transaction(
                            title = parsed.description,
                            amount = parsed.amount,
                            category = if (parsed.isIncome) "درآمد بانکی" else "هزینه بانکی",
                            date = System.currentTimeMillis(),
                            isIncome = parsed.isIncome,
                            cardRef = cardKey
                        )
                        dao.insertTransaction(transaction)

                        // 2. Update or create the BankCard with its live balance
                        val existingCard = cardDao.getCardByNumber(cardKey)
                        val updatedBalance = if (parsed.balance != null) {
                            parsed.balance
                        } else {
                            val current = existingCard?.balance ?: 0.0
                            if (parsed.isIncome) current + parsed.amount else current - parsed.amount
                        }

                        val card = BankCard(
                            cardNumber = cardKey,
                            bankName = parsed.bankName,
                            balance = updatedBalance,
                            cardColorHex = parsed.cardColorHex,
                            updatedAt = System.currentTimeMillis()
                        )
                        cardDao.insertOrUpdateCard(card)
                    }

                    val typeStr = if (parsed.isIncome) "واریز" else "برداشت"
                    val amountStr = String.format("%,d", parsed.amount.toLong())
                    Toast.makeText(
                        context,
                        "💳 $typeStr $amountStr تومان • ${parsed.bankName}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
