package com.example.budgetapp.sms

data class ParsedBankSms(
    val bankName: String,
    val amount: Double,
    val isIncome: Boolean,
    val balance: Double? = null,
    val cardOrAccount: String? = null,
    val description: String
)

object BankSmsParser {

    fun parse(smsBody: String, sender: String = ""): ParsedBankSms? {
        val cleanBody = smsBody.replace("،", "").replace(",", "").trim()

        // 1. Identify Bank Name
        val bankName = when {
            cleanBody.contains("بانک ملی") || sender.contains("Melli") -> "بانک ملی"
            cleanBody.contains("بانک ملت") || sender.contains("Mellat") -> "بانک ملت"
            cleanBody.contains("سامان") || sender.contains("Saman") -> "بانک سامان"
            cleanBody.contains("پاسارگاد") || sender.contains("Pasargad") -> "بانک پاسارگاد"
            cleanBody.contains("بلوبانک") || cleanBody.contains("بلو") || sender.contains("blubank") -> "بلوبانک"
            cleanBody.contains("رسالت") -> "بانک رسالت"
            cleanBody.contains("سپه") -> "بانک سپه"
            cleanBody.contains("تجارت") -> "بانک تجارت"
            cleanBody.contains("صادرات") -> "بانک صادرات"
            cleanBody.contains("پارسیان") -> "بانک پارسیان"
            cleanBody.contains("کشاورزی") -> "بانک کشاورزی"
            cleanBody.contains("مسکن") -> "بانک مسکن"
            cleanBody.contains("شهر") -> "بانک شهر"
            cleanBody.contains("رفاه") -> "بانک رفاه"
            cleanBody.contains("آینده") -> "بانک آینده"
            cleanBody.contains("بانک") || cleanBody.contains("واریز") || cleanBody.contains("برداشت") -> "پیامک بانکی"
            else -> null
        } ?: return null

        // 2. Identify Transaction Type (Income vs Expense)
        val isIncome = when {
            cleanBody.contains("واریز") || cleanBody.contains("+") || cleanBody.contains("انتقال از") -> true
            cleanBody.contains("برداشت") || cleanBody.contains("خرید") || cleanBody.contains("-") || cleanBody.contains("انتقال به") -> false
            else -> false
        }

        // 3. Extract Amount (Rials)
        // Look for patterns like: واریز: 500000 یا برداشت: 120000 یا +500000 یا مبلغ 150000 ریال
        val amountRegex = Regex("""(?:مبلغ|واریز|برداشت|خرید|پایا|ساتنا)?[:\s\+\-]*([0-9]{4,13})\s*(?:ریال|تومان)?""")
        val match = amountRegex.find(cleanBody)
        val rawAmount = match?.groupValues?.get(1)?.toDoubleOrNull() ?: return null

        // Convert to Tomans (Iranian standard) if in Rials (most bank SMS are in Rials)
        val amountInTomans = if (cleanBody.contains("تومان")) rawAmount else (rawAmount / 10.0)

        // 4. Extract Card or Account Number if exists
        val cardRegex = Regex("""(?:\*|کارت|حساب)?\s*([0-9]{4})\s*(?:\*|:)?""")
        val cardMatch = cardRegex.find(cleanBody)
        val card = cardMatch?.groupValues?.get(1)

        val desc = buildString {
            append(if (isIncome) "واریز از طریق " else "خرید/برداشت از طریق ")
            append(bankName)
            if (card != null) append(" (کارت $card)")
        }

        return ParsedBankSms(
            bankName = bankName,
            amount = amountInTomans,
            isIncome = isIncome,
            cardOrAccount = card,
            description = desc
        )
    }
}
