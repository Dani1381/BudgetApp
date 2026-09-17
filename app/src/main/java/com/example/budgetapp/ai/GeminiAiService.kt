package com.example.budgetapp.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object GeminiAiService {

    // Pool of working Gemini API Keys with auto-rotation
    private val API_KEYS = listOf(
        "AIzaSy...Jn58",
        "AQ.Ab8RN6Lny38qgQj6x5Y3c1w1K7A4GVRwg",
        "AIzaSy...d5-s",
        "AIzaSy...FoTI",
        "AIzaSy...Hrryi"
    )

    private var currentKeyIndex = 0

    private fun getNextKey(): String {
        val key = API_KEYS[currentKeyIndex % API_KEYS.size]
        currentKeyIndex++
        return key
    }

    /**
     * Ask Gemini to analyze the user's financial spending and provide advice in Persian
     */
    suspend fun getFinancialAdvice(
        balance: Double,
        income: Double,
        expense: Double,
        recentTransactions: List<String>
    ): String = withContext(Dispatchers.IO) {
        val prompt = buildString {
            append("تو یک مشاور مالی فوق‌العاده باهوش، دلسوز و صمیمی به زبان فارسی هستی.\n")
            append("وضعیت مالی کاربر:\n")
            append("- موجودی فعلی: ${balance.toLong()} تومان\n")
            append("- مجموع درآمدها: ${income.toLong()} تومان\n")
            append("- مجموع هزینه‌ها: ${expense.toLong()} تومان\n")
            append("- آخرین تراکنش‌های کاربر:\n")
            recentTransactions.take(8).forEach { append("  • $it\n") }
            append("\nلطفاً در حداکثر ۲ یا ۳ پاراگراف کوتاه، وضعیت خرج‌کرد کاربر رو ارزیابی کن و ۱ یا ۲ راهکار خیلی خلاقانه و کاربردی برای پس‌انداز و کنترل هزینه‌ها بهش پیشنهاد بده.")
        }

        callGemini(prompt)
    }

    /**
     * Parse messy natural Persian text into a structured Transaction JSON
     */
    suspend fun parseExpenseFromText(userInput: String): ParsedAiExpense? = withContext(Dispatchers.IO) {
        val prompt = """
            این متن کاربر درباره یک تراکنش مالی است: "$userInput"
            لطفاً اطلاعات آن را به دقت استخراج کن و فقط و فقط یک JSON با فرمت زیر تحویل بده (بدون هیچ توضیح اضافه یا مارک‌داون):
            {
              "title": "عنوان خلاصه تراکنش",
              "amount": مبلغ به تومان به صورت عدد,
              "category": "دسته‌بندی مثلا خوراک، کرایه، خرید، تفریح یا عمومی",
              "isIncome": false یا true
            }
        """.trimIndent()

        val response = callGemini(prompt)
        try {
            val cleanJson = response.substringAfter("{").substringBeforeLast("}")
            val fullJson = "{$cleanJson}"
            val obj = JSONObject(fullJson)
            ParsedAiExpense(
                title = obj.getString("title"),
                amount = obj.getDouble("amount"),
                category = obj.getString("category"),
                isIncome = obj.getBoolean("isIncome")
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun callGemini(userPrompt: String): String {
        for (attempt in 0 until API_KEYS.size) {
            val key = getNextKey()
            try {
                val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=$key"
                val url = URL(endpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000

                val payload = JSONObject().apply {
                    val partsArray = JSONArray().put(JSONObject().put("text", userPrompt))
                    val contentsArray = JSONArray().put(JSONObject().put("parts", partsArray))
                    put("contents", contentsArray)
                }

                OutputStreamWriter(conn.outputStream).use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    val jsonResponse = JSONObject(response)
                    val candidates = jsonResponse.getJSONArray("candidates")
                    if (candidates.length() > 0) {
                        val firstCandidate = candidates.getJSONObject(0)
                        val content = firstCandidate.getJSONObject("content")
                        val parts = content.getJSONArray("parts")
                        if (parts.length() > 0) {
                            return parts.getJSONObject(0).getString("text")
                        }
                    }
                }
            } catch (e: Exception) {
                // Try next key on failure
            }
        }
        return "خطا در ارتباط با هوش مصنوعی. لطفاً اتصال اینترنت خود را بررسی کنید."
    }
}

data class ParsedAiExpense(
    val title: String,
    val amount: Double,
    val category: String,
    val isIncome: Boolean
)
