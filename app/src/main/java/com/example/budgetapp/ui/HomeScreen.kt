package com.example.budgetapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgetapp.ai.GeminiAiService
import com.example.budgetapp.data.BankCard
import com.example.budgetapp.data.Transaction
import com.example.budgetapp.logger.AppLogger
import com.example.budgetapp.viewmodel.BudgetViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: BudgetViewModel,
    onSyncSmsRequested: () -> Unit = {}
) {
    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsState()
    val cards by viewModel.cards.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val totalCardsBalance by viewModel.totalCardsBalance.collectAsState()

    val totalNetWorth = if (cards.isNotEmpty() && totalCardsBalance > 0) totalCardsBalance else (totalIncome - totalExpense)

    val coroutineScope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var showAddCardDialog by remember { mutableStateOf(false) }
    var showAiAdvisorDialog by remember { mutableStateOf(false) }
    var showAiSmartEntryDialog by remember { mutableStateOf(false) }
    var showLogsDialog by remember { mutableStateOf(false) }

    var aiAdviceText by remember { mutableStateOf("") }
    var isAiLoading by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFF080C14), // Ultra Deep Obsidian
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "BUDGET & ASSETS",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "نسخه عیب‌یابی v3.2 • هوشمند و دقیق",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showLogsDialog = true }) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = "لاگ‌های سیستم",
                            tint = Color(0xFF38BDF8)
                        )
                    }
                    IconButton(onClick = onSyncSmsRequested) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "همگام‌سازی پیامک‌ها",
                            tint = Color(0xFF34D399)
                        )
                    }
                    IconButton(onClick = { showAiSmartEntryDialog = true }) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "ثبت با هوش مصنوعی",
                            tint = Color(0xFFA855F7)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExtendedFloatingActionButton(
                    onClick = {
                        showAiAdvisorDialog = true
                        isAiLoading = true
                        aiAdviceText = "در حال ارتباط با موتور هوش مصنوعی Gemini..."
                        coroutineScope.launch {
                            val cardSummaries = cards.map { "${it.bankName} (${it.cardHolder}): ${it.balance.toLong()} تومان" }
                            val recentSummaries = transactions.take(6).map {
                                "${it.title}: ${it.amount.toLong()} تومان (${if (it.isIncome) "درآمد" else "هزینه"})"
                            }
                            val fullContext = cardSummaries + recentSummaries
                            aiAdviceText = GeminiAiService.getFinancialAdvice(
                                context, totalNetWorth, totalIncome, totalExpense, fullContext
                            )
                            isAiLoading = false
                        }
                    },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = null) },
                    text = { Text("مشاور Gemini AI") },
                    containerColor = Color(0xFF7C3AED),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )

                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Color(0xFF0284C7),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "افزودن تراکنش")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Total Net Worth Summary Card
            item {
                TotalNetWorthCard(
                    totalNetWorth = totalNetWorth,
                    income = totalIncome,
                    expense = totalExpense,
                    cardsCount = cards.size
                )
            }

            // 2. Bank Cards Horizontal Carousel
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "کارت‌ها و حساب‌های بانکی",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { showAddCardDialog = true }) {
                        Icon(Icons.Default.AddCard, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("افزودن کارت", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (cards.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAddCardDialog = true },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("هنوز کارتی تفکیک نشده!", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("روی همگام‌سازی 🔄 بالا بزن تا پیامک‌های بانکیت تفکیک بشن", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(cards) { card ->
                            BankCardItem(
                                card = card,
                                onDelete = { viewModel.deleteCard(card.cardNumber) }
                            )
                        }
                    }
                }
            }

            // 3. Transactions Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "تراکنش‌های اخیر و پیامک‌ها",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${transactions.size} تراکنش",
                        color = Color(0xFF34D399),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 4. Transaction Items List
            if (transactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("هیچ تراکنشی هنوز ثبت نشده است", color = Color(0xFF64748B))
                    }
                }
            } else {
                items(transactions) { item ->
                    TransactionItem(
                        transaction = item,
                        onDelete = { viewModel.deleteTransaction(item) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(70.dp)) }
        }

        // Dialogs
        if (showAddDialog) {
            AddTransactionDialog(
                cards = cards,
                onDismiss = { showAddDialog = false },
                onConfirm = { title, amount, category, isIncome, cardRef ->
                    viewModel.addTransaction(title, amount, category, isIncome, cardRef)
                    showAddDialog = false
                }
            )
        }

        if (showAddCardDialog) {
            AddCardDialog(
                onDismiss = { showAddCardDialog = false },
                onConfirm = { num, name, bal, color ->
                    viewModel.addOrUpdateCard(num, name, bal, color)
                    showAddCardDialog = false
                }
            )
        }

        if (showLogsDialog) {
            LogsViewerDialog(
                context = context,
                onDismiss = { showLogsDialog = false }
            )
        }

        if (showAiAdvisorDialog) {
            AiAdvisorDialog(
                isLoading = isAiLoading,
                adviceText = aiAdviceText,
                onDismiss = { showAiAdvisorDialog = false }
            )
        }

        if (showAiSmartEntryDialog) {
            AiSmartEntryDialog(
                context = context,
                onDismiss = { showAiSmartEntryDialog = false },
                onParsed = { parsed ->
                    viewModel.addTransaction(
                        parsed.title,
                        parsed.amount,
                        parsed.category,
                        parsed.isIncome,
                        ""
                    )
                    showAiSmartEntryDialog = false
                }
            )
        }
    }
}

@Composable
fun TotalNetWorthCard(
    totalNetWorth: Double,
    income: Double,
    expense: Double,
    cardsCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF090D16))
                    )
                )
                .padding(22.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "موجودی و ارزش خالص کل",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "$cardsCount کارت تفکیک‌شده",
                            color = Color(0xFF10B981),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "${formatAmount(totalNetWorth)} تومان",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF131D31))
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("کل ورودی / درآمد", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text(
                            "+${formatAmount(income)}",
                            color = Color(0xFF34D399),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Divider(
                        color = Color(0xFF24385C),
                        modifier = Modifier
                            .height(32.dp)
                            .width(1.dp)
                    )
                    Column {
                        Text("کل مخارج و پرداختی", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text(
                            "-${formatAmount(expense)}",
                            color = Color(0xFFF43F5E),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BankCardItem(card: BankCard, onDelete: () -> Unit) {
    val bgGradient = try {
        val baseColor = Color(android.graphics.Color.parseColor(card.cardColorHex))
        Brush.linearGradient(listOf(baseColor, baseColor.copy(alpha = 0.65f), Color(0xFF080C14)))
    } catch (e: Exception) {
        Brush.linearGradient(listOf(Color(0xFF1E3A8A), Color(0xFF0F172A)))
    }

    Card(
        modifier = Modifier
            .width(260.dp)
            .height(150.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        card.bankName,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                    Icon(
                        Icons.Default.Nfc,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text("موجودی حساب / کارت", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    Text(
                        "${formatAmount(card.balance)} تومان",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        card.cardHolder,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "شاپرک",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (transaction.isIncome) Color(0xFF10B981).copy(alpha = 0.15f)
                            else Color(0xFFF43F5E).copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (transaction.isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (transaction.isIncome) Color(0xFF34D399) else Color(0xFFF43F5E),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = transaction.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${transaction.category} • ${formatDate(transaction.date)}" +
                                if (transaction.cardRef.isNotBlank()) " • [${transaction.cardRef}]" else "",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${if (transaction.isIncome) "+" else "-"}${formatAmount(transaction.amount)}",
                    fontWeight = FontWeight.Black,
                    color = if (transaction.isIncome) Color(0xFF34D399) else Color(0xFFF43F5E),
                    fontSize = 15.sp
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "حذف",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LogsViewerDialog(context: android.content.Context, onDismiss: () -> Unit) {
    var logsText by remember { mutableStateOf(AppLogger.getLogs(context)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Terminal, contentDescription = null, tint = Color(0xFF38BDF8))
                Spacer(modifier = Modifier.width(8.dp))
                Text("لاگ‌های عیب‌یابی جامع (Deep Logs)")
            }
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 380.dp)) {
                Text(
                    "این لاگ شامل متن کامل پیامک‌ها، کارت‌های ساخته شده و درخواست‌های شبکه جمینای است:",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .padding(10.dp)
                ) {
                    LazyColumn {
                        item {
                            Text(
                                text = logsText,
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { AppLogger.shareLogs(context) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("اشتراک‌گذاری لاگ")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                AppLogger.clearLogs(context)
                logsText = "لاگ‌ها پاکسازی شدند."
            }) {
                Text("پاکسازی لاگ")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardDialog(
    onDismiss: () -> Unit,
    onConfirm: (number: String, bankName: String, balance: Double, colorHex: String) -> Unit
) {
    var cardNumber by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var balanceText by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#0284C7") }

    val presetColors = listOf("#0284C7", "#DC2626", "#B45309", "#0D9488", "#7C3AED", "#16A34A")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("افزودن کارت بانکی جدید") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("نام بانک (مثلا بلو بانک، ملی یا سامان)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { cardNumber = it },
                    label = { Text("۴ رقم آخر کارت (مثلا ۵۰۲۲)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text("موجودی اولیه (تومان)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("انتخاب تم رنگی کارت:", fontSize = 12.sp, color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presetColors.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .clickable { selectedColor = hex }
                                .padding(2.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val bal = balanceText.toDoubleOrNull() ?: 0.0
                if (bankName.isNotBlank()) {
                    onConfirm(cardNumber.ifBlank { "عمومی" }, bankName, bal, selectedColor)
                }
            }) {
                Text("ایجاد کارت")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    cards: List<BankCard>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, category: String, isIncome: Boolean, cardRef: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    var selectedCardRef by remember { mutableStateOf("") }

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
                    label = { Text("عنوان تراکنش") },
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
                    label = { Text("دسته‌بندی (خوراک، بنزین، حقوق...)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amount > 0) {
                        onConfirm(title, amount, category.ifBlank { "عمومی" }, isIncome, selectedCardRef)
                    }
                }
            ) {
                Text("ثبت تراکنش")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
fun AiAdvisorDialog(isLoading: Boolean, adviceText: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Psychology, contentDescription = null, tint = Color(0xFF7C3AED))
                Spacer(modifier = Modifier.width(8.dp))
                Text("مشاور Gemini AI")
            }
        },
        text = {
            if (isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color(0xFF7C3AED))
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(adviceText, fontSize = 14.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    item {
                        Text(adviceText, fontSize = 14.sp, lineHeight = 24.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))) {
                Text("بستن")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSmartEntryDialog(
    context: android.content.Context,
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
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFA855F7))
                Spacer(modifier = Modifier.width(8.dp))
                Text("ثبت با زبان عامیانه (Gemini AI)")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "با زبان خودت بنویس؛ مثلاً «امروز ۶۵ تومن پول قهوه دادم» یا «۳ میلیون حقوق گرفتم»:",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                OutlinedTextField(
                    value = naturalText,
                    onValueChange = { naturalText = it },
                    placeholder = { Text("متن خرج یا دخل خود را بنویسید...") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFFA855F7))
                }
                if (errorMessage.isNotBlank()) {
                    Text(errorMessage, color = Color(0xFFF43F5E), fontSize = 12.sp)
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
                            val parsed = GeminiAiService.parseExpenseFromText(context, naturalText)
                            isLoading = false
                            if (parsed != null) {
                                onParsed(parsed)
                            } else {
                                errorMessage = "پاسخی دریافت نشد. لاگ‌ها را بررسی کنید."
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7))
            ) {
                Text("ثبت هوشمند")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
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
