package com.example.budgetapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgetapp.ai.GeminiAiService
import com.example.budgetapp.data.Transaction
import com.example.budgetapp.viewmodel.BudgetViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: BudgetViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val balance = totalIncome - totalExpense
    val coroutineScope = rememberCoroutineScope()

    var showDialog by remember { mutableStateOf(false) }
    var showAiAdvisorDialog by remember { mutableStateOf(false) }
    var showAiSmartEntryDialog by remember { mutableStateOf(false) }

    var aiAdviceText by remember { mutableStateOf("") }
    var isAiLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مدیریت بودجه هوشمند") },
                actions = {
                    IconButton(onClick = { showAiSmartEntryDialog = true }) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "ثبت هوشمند با جمینای",
                            tint = Color(0xFF673AB7)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExtendedFloatingActionButton(
                    onClick = {
                        showAiAdvisorDialog = true
                        isAiLoading = true
                        aiAdviceText = "در حال تحلیل حساب‌ها توسط جمینای..."
                        coroutineScope.launch {
                            val recentSummaries = transactions.take(6).map {
                                "${it.title}: ${it.amount.toLong()} تومان (${if (it.isIncome) "درآمد" else "هزینه"})"
                            }
                            aiAdviceText = GeminiAiService.getFinancialAdvice(
                                balance, totalIncome, totalExpense, recentSummaries
                            )
                            isAiLoading = false
                        }
                    },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = null) },
                    text = { Text("مشاور هوش مصنوعی") },
                    containerColor = Color(0xFF673AB7),
                    contentColor = Color.White
                )

                FloatingActionButton(
                    onClick = { showDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "افزودن تراکنش")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "موجودی کل حساب", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${formatAmount(balance)} تومان",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (balance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "کل دریافتی", color = Color(0xFF2E7D32))
                            Text(
                                text = "+${formatAmount(totalIncome)}",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "کل مخارج", color = Color(0xFFC62828))
                            Text(
                                text = "-${formatAmount(totalExpense)}",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC62828)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تراکنش‌های اخیر و پیامک‌ها",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ثبت خودکار فعال ⚡",
                    fontSize = 12.sp,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "هیچ تراکنشی ثبت نشده است", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(transactions) { item ->
                        TransactionItem(
                            transaction = item,
                            onDelete = { viewModel.deleteTransaction(item) }
                        )
                    }
                }
            }
        }

        if (showDialog) {
            AddTransactionDialog(
                onDismiss = { showDialog = false },
                onConfirm = { title, amount, category, isIncome ->
                    viewModel.addTransaction(title, amount, category, isIncome)
                    showDialog = false
                }
            )
        }

        if (showAiAdvisorDialog) {
            AlertDialog(
                onDismissRequest = { showAiAdvisorDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = Color(0xFF673AB7))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تحلیل هوشمند جمینای AI")
                    }
                },
                text = {
                    if (isAiLoading) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color(0xFF673AB7))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(aiAdviceText, fontSize = 14.sp)
                        }
                    } else {
                        Text(aiAdviceText, fontSize = 14.sp, lineHeight = 22.sp)
                    }
                },
                confirmButton = {
                    Button(onClick = { showAiAdvisorDialog = false }) {
                        Text("متشکرم")
                    }
                }
            )
        }

        if (showAiSmartEntryDialog) {
            AiSmartEntryDialog(
                onDismiss = { showAiSmartEntryDialog = false },
                onParsed = { parsed ->
                    viewModel.addTransaction(
                        parsed.title,
                        parsed.amount,
                        parsed.category,
                        parsed.isIncome
                    )
                    showAiSmartEntryDialog = false
                }
            )
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = transaction.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    text = "${transaction.category} • ${formatDate(transaction.date)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${if (transaction.isIncome) "+" else "-"}${formatAmount(transaction.amount)}",
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.isIncome) Color(0xFF2E7D32) else Color(0xFFC62828),
                    fontSize = 16.sp
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "حذف",
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSmartEntryDialog(
    onDismiss: () -> Unit,
    onParsed: (com.example.budgetapp.ai.ParsedAiExpense) -> Unit
) {
    var naturalText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF673AB7))
                Spacer(modifier = Modifier.width(8.dp))
                Text("ثبت با زبان عامیانه (Gemini AI)")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "هرچی خرج کردی رو همینجوری راحت بنویس؛ جمینای خودش مبالغ و دسته‌بندی رو درمیاره!",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                OutlinedTextField(
                    value = naturalText,
                    onValueChange = { naturalText = it },
                    placeholder = { Text("مثلا: امروز ۱۸۰ تومن پول اسنپ دادم") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF673AB7))
                }
                if (errorMessage.isNotBlank()) {
                    Text(errorMessage, color = Color.Red, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (naturalText.isNotBlank()) {
                        isLoading = true
                        errorMessage = ""
                        coroutineScope.launch {
                            val parsed = GeminiAiService.parseExpenseFromText(naturalText)
                            isLoading = false
                            if (parsed != null) {
                                onParsed(parsed)
                            } else {
                                errorMessage = "نتونستم مبلغ رو تشخیص بدم، لطفاً متن رو واضح‌تر بنویسید."
                            }
                        }
                    }
                }
            ) {
                Text("ثبت هوشمند")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, category: String, isIncome: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "افزودن دستی تراکنش") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    FilterChip(
                        selected = !isIncome,
                        onClick = { isIncome = false },
                        label = { Text("هزینه") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = isIncome,
                        onClick = { isIncome = true },
                        label = { Text("درآمد") }
                    )
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("مبلغ (تومان)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("دسته‌بندی") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amount > 0) {
                        onConfirm(title, amount, category.ifBlank { "عمومی" }, isIncome)
                    }
                }
            ) {
                Text("ثبت")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

fun formatAmount(amount: Double): String {
    return NumberFormat.getNumberInstance(Locale.US).format(amount)
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
