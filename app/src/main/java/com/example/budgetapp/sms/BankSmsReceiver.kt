package com.example.budgetapp.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.widget.Toast
import com.example.budgetapp.data.AppDatabase
import com.example.budgetapp.data.Transaction
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

                val parsed = BankSmsParser.parse(body, sender)
                if (parsed != null) {
                    val db = AppDatabase.getDatabase(context)
                    val dao = db.transactionDao()

                    CoroutineScope(Dispatchers.IO).launch {
                        val transaction = Transaction(
                            title = parsed.description,
                            amount = parsed.amount,
                            category = if (parsed.isIncome) "درآمد بانکی" else "هزینه بانکی",
                            date = System.currentTimeMillis(),
                            isIncome = parsed.isIncome
                        )
                        dao.insertTransaction(transaction)
                    }

                    val typeStr = if (parsed.isIncome) "واریز" else "برداشت"
                    val amountStr = String.format("%,d", parsed.amount.toLong())
                    Toast.makeText(
                        context,
                        "💳 ثبت خودکار $typeStr $amountStr تومان از ${parsed.bankName}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
