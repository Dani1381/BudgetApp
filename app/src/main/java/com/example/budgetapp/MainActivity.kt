package com.example.budgetapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetapp.sms.SmsInboxSyncManager
import com.example.budgetapp.ui.HomeScreen
import com.example.budgetapp.ui.theme.BudgetAppTheme
import com.example.budgetapp.viewmodel.BudgetViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestSmsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            Toast.makeText(this, "✅ مجوز دسترسی پیامک فعال شد! در حال خواندن پیامک‌های بانکی...", Toast.LENGTH_SHORT).show()
            triggerInboxSync()
        } else {
            Toast.makeText(this, "برای ثبت خودکار تراکنش‌ها، نیاز به دسترسی پیامک است", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestSmsPermissions()

        setContent {
            BudgetAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: BudgetViewModel = viewModel()
                    HomeScreen(
                        viewModel = viewModel,
                        onSyncSmsRequested = { triggerInboxSync() }
                    )
                }
            }
        }
    }

    private fun checkAndRequestSmsPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            requestSmsPermissionLauncher.launch(missing.toTypedArray())
        } else {
            // Permissions already granted, sync inbox immediately on startup
            triggerInboxSync()
        }
    }

    private fun triggerInboxSync() {
        lifecycleScope.launch {
            val imported = SmsInboxSyncManager.syncHistoricalBankSms(this@MainActivity)
            if (imported > 0) {
                Toast.makeText(
                    this@MainActivity,
                    "🎉 $imported تراکنش از پیامک‌های قبلی بانکی خوانده و اضافه شد!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
