package com.example.budgetapp.ai

import android.content.Context
import com.example.budgetapp.logger.AppLogger
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

    // Valid Gemini API Keys with rotation
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

    suspend fun getFinancialAdvice(
        context: Context,
        balance: Double,
        income: Double,
        expense: Double,
        recentTransactions: List<String>
    ): String = withContext(Dispatchers.IO) {
        val prompt = buildString {
            append("تو یک مشاور مالی شخصی بسیار باهوش و دلسوز به زبان فارسی هستی.\n")
            append("اطلاعات حساب کاربر:\n")
            append("- مجموع موجودی: ${balance.toLong()} تومان\n")
            append("- مجموع درآمدها: ${income.toLong()} تومان\n")
            append("- مجموع مخارج: ${expense.toLong()} تومان\n")
            append("- تراکنش‌ها و حساب‌ها:\n")
            recentTransactions.take(8).forEach { append("  • $it\n") }
            append("\nلطفاً در ۲ پاراگراف کوتاه وضعیت مالی را بررسی کن و ۲ توصیه کاربردی و هوشمندانه بده.")
        }

        callGemini(context, prompt)
    }

    suspend fun parseExpenseFromText(context: Context, userInput: String): ParsedAiExpense? = withContext(Dispatchers.IO) {
        val prompt = """
            متن تراکنش کاربر: "$userInput"
            فقط و فقط یک JSON با کلیدهای زیر برگردان:
            {
              "title": "عنوان تراکنش",
              "amount": مبلغ به تومان به صورت عدد,
              "category": "دسته‌بندی",
              "isIncome": true یا false
            }
        """.trimIndent()

        val response = callGemini(context, prompt)
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
            AppLogger.log(context, "GEMINI_PARSE_ERR", "Failed parsing JSON from Gemini response: $response", e)
            null
        }
    }

    private fun callGemini(context: Context, userPrompt: String): String {
        var lastError = ""

        // Test with v1beta gemini-3.6-flash and fallback to gemini-2.5-flash
        val models = listOf("gemini-3.6-flash", "gemini-2.5-flash", "gemini-flash-latest")

        for (model in models) {
            for (key in API_KEYS) {
                try {
                    val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key"
                    AppLogger.log(context, "GEMINI_REQ", "Calling model: $model with key: ${key.take(8)}...")

                    val url = URL(endpoint)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true
                    conn.connectTimeout = 12000
                    conn.readTimeout = 12000

                    val payload = JSONObject().apply {
                        val partsArray = JSONArray().put(JSONObject().put("text", userPrompt))
                        val contentsArray = JSONArray().put(JSONObject().put("parts", partsArray))
                        put("contents", contentsArray)
                    }

                    OutputStreamWriter(conn.outputStream).use { writer ->
                        writer.write(payload.toString())
                        writer.flush()
                    }

                    val code = conn.responseCode
                    if (code == HttpURLConnection.HTTP_OK) {
                        val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                        val jsonResponse = JSONObject(response)
                        val candidates = jsonResponse.getJSONArray("candidates")
                        if (candidates.length() > 0) {
                            val text = candidates.getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text")
                            AppLogger.log(context, "GEMINI_SUCCESS", "Model $model responded (${text.length} chars)")
                            return text
                        }
                    } else {
                        val errResponse = try {
                            BufferedReader(InputStreamReader(conn.errorStream)).use { it.readText() }
                        } catch (e: Exception) { "No error body" }
                        lastError = "HTTP $code from $model: $errResponse"
                        AppLogger.log(context, "GEMINI_HTTP_ERR", lastError)
                    }
                } catch (e: Exception) {
                    lastError = "Network/DNS Error: ${e.message}"
                    AppLogger.log(context, "GEMINI_NET_ERR", "Model $model connection error", e)
                }
            }
        }

        return "خطا در برقراری ارتباط با Gemini AI ($lastError). لاگ‌های برنامه را برای مشاهده جزئیات بررسی کنید."
    }
}

data class ParsedAiExpense(
    val title: String,
    val amount: Double,
    val category: String,
    val isIncome: Boolean
)
