package com.example.budgetapp.sms

data class ParsedBankSms(
    val bankName: String,
    val amount: Double,
    val isIncome: Boolean,
    val balance: Double? = null,
    val cardOrAccount: String? = null,
    val description: String,
    val cardColorHex: String = "#1E293B"
)

object BankSmsParser {

    fun parse(smsBody: String, sender: String = ""): ParsedBankSms? {
        val cleanBody = smsBody.replace("،", "").replace(",", "").trim()

        // 1. Identify Bank Name & Accent Colors
        val (bankName, colorHex) = when {
            cleanBody.contains("بلوبانک") || cleanBody.contains("بلو") || sender.contains("blubank", ignoreCase = true) -> 
                Pair("بلو بانک (Saman)", "#0284C7") // Blu Electric Blue
            cleanBody.contains("بانک ملی") || sender.contains("Melli", ignoreCase = true) -> 
                Pair("بانک ملی", "#B45309") // Melli Warm Amber
            cleanBody.contains("بانک ملت") || sender.contains("Mellat", ignoreCase = true) -> 
                Pair("بانک ملت", "#DC2626") // Mellat Crimson
            cleanBody.contains("سامان") || sender.contains("Saman", ignoreCase = true) -> 
                Pair("بانک سامان", "#0D9488") // Saman Teal
            cleanBody.contains("پاسارگاد") || sender.contains("Pasargad", ignoreCase = true) -> 
                Pair("بانک پاسارگاد", "#D97706") // Pasargad Gold
            cleanBody.contains("رسالت") -> 
                Pair("بانک رسالت", "#059669") // Resalat Emerald
            cleanBody.contains("سپه") -> 
                Pair("بانک سپه", "#3B82F6") // Sepah Blue
            cleanBody.contains("تجارت") -> 
                Pair("بانک تجارت", "#4F46E5") // Tejarat Indigo
            cleanBody.contains("صادرات") -> 
                Pair("بانک صادرات", "#7C3AED") // Saderat Purple
            cleanBody.contains("پارسیان") -> 
                Pair("بانک پارسیان", "#9333EA") // Parsian Violet
            cleanBody.contains("کشاورزی") -> 
                Pair("بانک کشاورزی", "#16A34A") // Keshavarzi Green
            cleanBody.contains("مسکن") -> 
                Pair("بانک مسکن", "#EA580C") // Maskan Orange
            cleanBody.contains("شهر") -> 
                Pair("بانک شهر", "#E11D48") // Shahr Rose
            cleanBody.contains("رفاه") -> 
                Pair("بانک رفاه", "#2563EB") // Refah Royal Blue
            cleanBody.contains("آینده") -> 
                Pair("بانک آینده", "#991B1B") // Ayandeh Maroon
            cleanBody.contains("بانک") || cleanBody.contains("واریز") || cleanBody.contains("برداشت") -> 
                Pair("کارت بانکی", "#334155")
            else -> return null
        }

        // 2. Identify Transaction Type (Income vs Expense)
        val isIncome = when {
            cleanBody.contains("واریز") || cleanBody.contains("+") || cleanBody.contains("انتقال از") -> true
            cleanBody.contains("برداشت") || cleanBody.contains("خرید") || cleanBody.contains("-") || cleanBody.contains("انتقال به") -> false
            else -> false
        }

        // 3. Extract Amount (Rials or Tomans)
        val amountRegex = Regex("""(?:مبلغ|واریز|برداشت|خرید|پایا|ساتنا)?[:\s\+\-]*([0-9]{4,13})\s*(?:ریال|تومان)?""")
        val match = amountRegex.find(cleanBody)
        val rawAmount = match?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
        val amountInTomans = if (cleanBody.contains("تومان")) rawAmount else (rawAmount / 10.0)

        // 4. Extract Card or Account Number if exists
        val cardRegex = Regex("""(?:\*|کارت|حساب)?\s*([0-9]{4})\s*(?:\*|:)?""")
        val cardMatch = cardRegex.find(cleanBody)
        val card = cardMatch?.groupValues?.get(1) ?: (bankName.split(" ").lastOrNull() ?: "عمومی")

        // 5. Extract Card Balance if provided in SMS (e.g. موجودی: 12500000)
        val balanceRegex = Regex("""(?:موجودی|مانده)[:\s]*([0-9]{4,13})\s*(?:ریال|تومان)?""")
        val balanceMatch = balanceRegex.find(cleanBody)
        val rawBalance = balanceMatch?.groupValues?.get(1)?.toDoubleOrNull()
        val balanceInTomans = if (rawBalance != null) {
            if (cleanBody.contains("تومان")) rawBalance else (rawBalance / 10.0)
        } else null

        val desc = buildString {
            append(if (isIncome) "واریز " else "خرید/برداشت ")
            append(bankName)
            if (card.matches(Regex("[0-9]{4}"))) append(" ••$card")
        }

        return ParsedBankSms(
            bankName = bankName,
            amount = amountInTomans,
            isIncome = isIncome,
            balance = balanceInTomans,
            cardOrAccount = card,
            description = desc,
            cardColorHex = colorHex
        )
    }
}
