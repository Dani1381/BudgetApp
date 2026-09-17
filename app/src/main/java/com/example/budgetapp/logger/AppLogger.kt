package com.example.budgetapp.logger

import android.content.Context
import android.content.Intent
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {

    private const val LOG_FILE_NAME = "budget_app_full_diagnostics.txt"
    private const val MAX_LOG_SIZE_BYTES = 500 * 1024 // 500 KB cap

    @Synchronized
    fun log(context: Context, tag: String, message: String, throwable: Throwable? = null) {
        try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists() && file.length() > MAX_LOG_SIZE_BYTES) {
                // Keep the last 100KB to avoid file bloat
                val lines = file.readLines()
                file.writeText(lines.takeLast(300).joinToString("\n") + "\n")
            }

            val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            val sb = StringBuilder()
            sb.append("[$time] [$tag] $message\n")
            if (throwable != null) {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                sb.append(">>> EXCEPTION: ").append(sw.toString()).append("\n")
            }
            file.appendText(sb.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLogs(context: Context): String {
        return try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists()) file.readText() else "هنوز لاگی ثبت نشده است."
        } catch (e: Exception) {
            "خطا در خواندن فایل لاگ: ${e.message}"
        }
    }

    fun clearLogs(context: Context) {
        try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shareLogs(context: Context) {
        try {
            val logText = getLogs(context)
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "=== BUDGETAPP DEEP DIAGNOSTIC LOGS ===\n\n$logText")
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "ارسال لاگ عیب‌یابی به تلگرام")
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
