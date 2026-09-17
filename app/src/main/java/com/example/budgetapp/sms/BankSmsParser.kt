package com.example.budgetapp.sms

data class ParsedBankSms(
    val bankName: String,
    val canonicalCardKey: String, // Normalized unique ID for the card (e.g. "BLU_5022", "MELLI_6037", "SAMAN_DEFAULT")
    val displayCardNumber: String, // e.g. "5022" or "اصلی"
    val amount: Double,
    val isIncome: Boolean,
    val balance: Double? = null,
    val description: String,
    val cardColorHex: String
)

object BankSmsParser {

    fun parse(smsBody: String, sender: String = ""): ParsedBankSms? {
        val cleanBody = smsBody.replace("،", "").replace(",", "").trim()

        // 1. Bank Identification & Themes
        val (bankName, defaultColor, bankTag) = when {
            cleanBody.contains("بلوبانک") || cleanBody.contains("بلو بانک") || cleanBody.contains("بلو") || sender.contains("blubank", ignoreCase = true) -> 
                Triple("بلو بانک", "#0284C7", "BLUBANK")
            cleanBody.contains("بانک ملی") || sender.contains("Melli", ignoreCase = true) -> 
                Triple("بانک ملی", "#B45309", "MELLI")
            cleanBody.contains("بانک ملت") || sender.contains("Mellat", ignoreCase = true) -> 
                Triple("بانک ملت", "#DC2626", "MELLAT")
            cleanBody.contains("سامان") || sender.contains("Saman", ignoreCase = true) -> 
                Triple("بانک سامان", "#0D9488", "SAMAN")
            cleanBody.contains("پاسارگاد") || sender.contains("Pasargad", ignoreCase = true) -> 
                Triple("بانک پاسارگاد", "#D97706", "PASARGAD")
            cleanBody.contains("رسالت") -> 
                Triple("بانک رسالت", "#059669", "RESALAT")
            cleanBody.contains("سپه") -> 
                Triple("بانک سپه", "#3B82F6", "SEPAH")
            cleanBody.contains("تجارت") -> 
                Triple("بانک تجارت", "#4F46E5", "TEJARAT")
            cleanBody.contains("صادرات") -> 
                Triple("بانک صادرات", "#7C3AED", "SADERAT")
            cleanBody.contains("پارسیان") -> 
                Triple("بانک پارسیان", "#9333EA", "PARSIAN")
            cleanBody.contains("کشاورزی") -> 
                Triple("بانک کشاورزی", "#16A34A", "KESHAVARZI")
            cleanBody.contains("مسکن") -> 
                Triple("بانک مسکن", "#EA580C", "MASKAN")
            cleanBody.contains("شهر") -> 
                Triple("بانک شهر", "#E11D48", "SHAHR")
            cleanBody.contains("رفاه") -> 
                Triple("بانک رفاه", "#2563EB", "REFAH")
            cleanBody.contains("آینده") -> 
                Triple("بانک آینده", "#991B1B", "AYANDEH")
            cleanBody.contains("واریز") || cleanBody.contains("برداشت") || cleanBody.contains("مانده") -> 
                Triple("کارت بانکی", "#334155", "GENERIC")
            else -> return null
        }

        // 2. Transaction Type
        val isIncome = when {
            cleanBody.contains("واریز") || cleanBody.contains("+") || cleanBody.contains("انتقال از") -> true
            cleanBody.contains("برداشت") || cleanBody.contains("خرید") || cleanBody.contains("-") || cleanBody.contains("انتقال به") -> false
            else -> false
        }

        // 3. Amount Extraction (Rials or Tomans)
        val amountRegex = Regex("""(?:مبلغ|واریز|برداشت|خرید|پایا|ساتنا)?[:\s\+\-]*([0-9]{4,13})\s*(?:ریال|تومان)?""")
        val match = amountRegex.find(cleanBody)
        val rawAmount = match?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
        val amountInTomans = if (cleanBody.contains("تومان")) rawAmount else (rawAmount / 10.0)

        // 4. Exact Card Number (4 digits) or Account Normalization
        val cardRegex = Regex("""(?:\*|کارت|حساب)?\s*([0-9]{4})\s*(?:\*|:|به|از)?""")
        val cardMatch = cardRegex.find(cleanBody)
        val cardDigits = cardMatch?.groupValues?.get(1)

        val (canonicalKey, displayNum) = if (cardDigits != null) {
            Pair("${bankTag}_$cardDigits", cardDigits)
        } else {
            Pair("${bankTag}_MAIN", "اصلی")
        }

        // 5. Card Balance (e.g. موجودی: 12500000)
        val balanceRegex = Regex("""(?:موجودی|مانده)[:\s]*([0-9]{4,13})\s*(?:ریال|تومان)?""")
        val balanceMatch = balanceRegex.find(cleanBody)
        val rawBalance = balanceMatch?.groupValues?.get(1)?.toDoubleOrNull()
        val balanceInTomans = if (rawBalance != null) {
            if (cleanBody.contains("تومان")) rawBalance else (rawBalance / 10.0)
        } else null

        val desc = buildString {
            append(if (isIncome) "واریز " else "خرید/برداشت ")
            append(bankName)
            if (cardDigits != null) append(" (••$cardDigits)")
        }

        return ParsedBankSms(
            bankName = bankName,
            canonicalCardKey = canonicalKey,
            displayCardNumber = displayNum,
            amount = amountInTomans,
            isIncome = isIncome,
            balance = balanceInTomans,
            description = desc,
            cardColorHex = defaultColor
        )
    }
}
