package com.example.budgetapp.logger

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {

    private const val LOG_FILE_NAME = "budget_app_debug_logs.txt"

    fun log(context: Context, tag: String, message: String) {
        try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val logLine = "[$time] [$tag] $message\n"
            file.appendText(logLine)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLogs(context: Context): String {
        return try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists()) file.readText() else "هیچ لاگی هنوز ثبت نشده است."
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
                putExtra(Intent.EXTRA_TEXT, "--- BudgetApp Diagnostic Logs ---\n\n$logText")
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "ارسال و بررسی لاگ‌های برنامه")
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
