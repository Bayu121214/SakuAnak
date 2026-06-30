package com.example.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DailyBudget
import com.example.data.TransactionItem
import com.example.data.UserAccount
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

enum class SakuCategory(val label: String, val icon: ImageVector, val color: Color) {
    MAKANAN("Makanan & Minuman", Icons.Default.Restaurant, Color(0xFFFF9800)),
    TRANSPORT("Transportasi", Icons.Default.DirectionsBus, Color(0xFF03A9F4)),
    SEKOLAH("Sekolah & Buku", Icons.Default.School, Color(0xFF9C27B0)),
    JAJAN("Jajan & Hiburan", Icons.Default.LocalPlay, Color(0xFFE91E63)),
    BELANJA("Belanja & Kebutuhan", Icons.Default.ShoppingBag, Color(0xFF4CAF50)),
    LAIN_LAIN("Lain-lain", Icons.Default.Category, Color(0xFF607D8B));

    companion object {
        fun fromString(value: String): SakuCategory {
            return entries.firstOrNull { it.label == value } ?: LAIN_LAIN
        }
    }
}

fun formatRupiah(amount: Long): String {
    val localeID = Locale("id", "ID")
    val numberFormat = NumberFormat.getNumberInstance(localeID)
    return "Rp ${numberFormat.format(amount)}"
}

fun formatDateForDisplay(dateString: String): String {
    try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = parser.parse(dateString) ?: return dateString
        val formatter = SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID"))
        return formatter.format(date)
    } catch (e: Exception) {
        return dateString
    }
}

fun formatShortDate(dateString: String): String {
    try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = parser.parse(dateString) ?: return dateString
        val formatter = SimpleDateFormat("dd MMM", Locale("id", "ID"))
        return formatter.format(date)
    } catch (e: Exception) {
        return dateString
    }
}

fun formatDayOfWeek(dateString: String): String {
    try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = parser.parse(dateString) ?: return dateString
        val formatter = SimpleDateFormat("EEE", Locale("id", "ID"))
        return formatter.format(date)
    } catch (e: Exception) {
        return dateString
    }
}

enum class SakuScreen {
    BERANDA, HISTORI, STATISTIK, PROFIL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SakuApp(
    viewModel: SakuViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUserDb.collectAsStateWithLifecycle()
    val sessionUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val selectedDate by viewModel.selectedDateString.collectAsStateWithLifecycle()
    val budget by viewModel.selectedDateBudget.collectAsStateWithLifecycle()
    val transactions by viewModel.selectedDateTransactions.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val allBudgets by viewModel.allBudgets.collectAsStateWithLifecycle()
    val simulatedNotification by viewModel.simulatedOtpNotification.collectAsStateWithLifecycle()

    var currentScreen by remember { mutableStateOf(SakuScreen.BERANDA) }
    var showAddDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<TransactionItem?>(null) }
    var showBudgetDialog by remember { mutableStateOf(false) }

    // If no user is logged in, show the Google Sign-In Screen
    if (sessionUser == null) {
        GoogleSignInScreen(onSignIn = { email, name, role, whatsappNumber ->
            viewModel.loginWithGoogle(email, name, role, whatsappNumber)
        })
    } else {
        val user = currentUser ?: sessionUser!!

        // Redirect parent if they somehow end up in Statistik screen
        if (user.role == "ORTU" && currentScreen == SakuScreen.STATISTIK) {
            currentScreen = SakuScreen.BERANDA
        }

        Scaffold(
            modifier = modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_navigation")
                ) {
                    NavigationBarItem(
                        icon = { Icon(if (currentScreen == SakuScreen.BERANDA) Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "Beranda") },
                        label = { Text("Beranda") },
                        selected = currentScreen == SakuScreen.BERANDA,
                        onClick = { currentScreen = SakuScreen.BERANDA },
                        modifier = Modifier.testTag("nav_beranda")
                    )
                    NavigationBarItem(
                        icon = { Icon(if (currentScreen == SakuScreen.HISTORI) Icons.Filled.ListAlt else Icons.Outlined.ListAlt, contentDescription = "Histori") },
                        label = { Text("Histori") },
                        selected = currentScreen == SakuScreen.HISTORI,
                        onClick = { currentScreen = SakuScreen.HISTORI },
                        modifier = Modifier.testTag("nav_histori")
                    )
                    if (user.role != "ORTU") {
                        NavigationBarItem(
                            icon = { Icon(if (currentScreen == SakuScreen.STATISTIK) Icons.Filled.PieChart else Icons.Outlined.PieChart, contentDescription = "Statistik") },
                            label = { Text("Statistik") },
                            selected = currentScreen == SakuScreen.STATISTIK,
                            onClick = { currentScreen = SakuScreen.STATISTIK },
                            modifier = Modifier.testTag("nav_statistik")
                        )
                    }
                    NavigationBarItem(
                        icon = { Icon(if (currentScreen == SakuScreen.PROFIL) Icons.Filled.Person else Icons.Outlined.Person, contentDescription = "Profil") },
                        label = { Text("Profil") },
                        selected = currentScreen == SakuScreen.PROFIL,
                        onClick = { currentScreen = SakuScreen.PROFIL },
                        modifier = Modifier.testTag("nav_profil")
                    )
                }
            },
            floatingActionButton = {
                // Show Add Transaction button only if user is ANAK
                if ((currentScreen == SakuScreen.BERANDA || currentScreen == SakuScreen.HISTORI) && user.role == "ANAK") {
                    FloatingActionButton(
                        onClick = {
                            transactionToEdit = null
                            showAddDialog = true
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .testTag("add_transaction_fab")
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Tambah")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Catat Jajan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentScreen) {
                        SakuScreen.BERANDA -> {
                            HomeScreen(
                                viewModel = viewModel,
                                user = user,
                                selectedDate = selectedDate,
                                budget = budget,
                                transactions = transactions,
                                onSetBudgetClick = { showBudgetDialog = true },
                                onEditTransaction = { transaction ->
                                    transactionToEdit = transaction
                                    showAddDialog = true
                                }
                            )
                        }
                        SakuScreen.HISTORI -> {
                            HistoryScreen(
                                viewModel = viewModel,
                                user = user,
                                allTransactions = allTransactions,
                                onEditTransaction = { transaction ->
                                    transactionToEdit = transaction
                                    showAddDialog = true
                                }
                            )
                        }
                        SakuScreen.STATISTIK -> {
                            StatisticsScreen(
                                user = user,
                                allTransactions = allTransactions,
                                allBudgets = allBudgets
                            )
                        }
                        SakuScreen.PROFIL -> {
                            ProfileScreen(
                                viewModel = viewModel,
                                user = user
                            )
                        }
                    }
                }

                // Global Simulated OTP Notification Pop-up Dialog
                simulatedNotification?.let { notificationContent ->
                    Dialog(onDismissRequest = { viewModel.dismissOtpNotification() }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Email, contentDescription = "Email", tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Simulasi Inbox Anak", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Untuk mempermudah simulasi di 1 perangkat, silakan gunakan OTP di bawah:",
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = notificationContent,
                                        fontSize = 13.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = { viewModel.dismissOtpNotification() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Salin & Tutup")
                                }
                            }
                        }
                    }
                }

                // Add / Edit Transaction Dialog
                if (showAddDialog) {
                    AddEditTransactionDialog(
                        selectedDate = selectedDate,
                        editingTransaction = transactionToEdit,
                        onDismiss = { showAddDialog = false },
                        onSave = { title, amount, category, date, notes ->
                            if (transactionToEdit == null) {
                                viewModel.addTransaction(title, amount, category, date, notes)
                            } else {
                                viewModel.updateTransaction(
                                    transactionToEdit!!.copy(
                                        title = title,
                                        amount = amount,
                                        category = category,
                                        dateString = date,
                                        notes = notes
                                    )
                                )
                            }
                            showAddDialog = false
                        }
                    )
                }

                // Set Budget Dialog
                if (showBudgetDialog) {
                    SetBudgetDialog(
                        selectedDate = selectedDate,
                        currentBudget = budget,
                        onDismiss = { showBudgetDialog = false },
                        onSave = { newBudget ->
                            viewModel.setBudgetForDate(selectedDate, newBudget)
                            showBudgetDialog = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GoogleSignInScreen(
    onSignIn: (String, String, String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var whatsappNumber by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("ANAK") } // ANAK or ORTU
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo / Mascot
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text("🎒", fontSize = 56.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Selamat Datang!",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Kelola & Pantau uang jajan dengan mudah dan menyenangkan di SakuPelajar",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Role Selector Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Masuk Sebagai:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { selectedRole = "ANAK" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedRole == "ANAK") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (selectedRole == "ANAK") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Anak Sekolah")
                    }
                    Button(
                        onClick = { selectedRole = "ORTU" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedRole == "ORTU") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (selectedRole == "ORTU") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Orang Tua")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Form Fields
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nama Lengkap") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Nama") },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Google") },
            leadingIcon = { Icon(Icons.Default.Mail, contentDescription = "Email") },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = whatsappNumber,
            onValueChange = { whatsappNumber = it },
            label = { Text("Nomor HP (WhatsApp)") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "WhatsApp") },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("login_whatsapp_input"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (name.isBlank() || email.isBlank() || whatsappNumber.isBlank()) {
                    Toast.makeText(context, "Silakan lengkapi Nama, Email, dan Nomor WhatsApp Anda!", Toast.LENGTH_SHORT).show()
                } else if (!email.contains("@")) {
                    Toast.makeText(context, "Harap masukkan format Email Google yang valid!", Toast.LENGTH_SHORT).show()
                } else {
                    onSignIn(email.trim().lowercase(), name.trim(), selectedRole, whatsappNumber.trim())
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(2.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Login, contentDescription = "Masuk")
                Spacer(modifier = Modifier.width(12.dp))
                Text("Masuk dengan Google", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun HomeScreen(
    viewModel: SakuViewModel,
    user: UserAccount,
    selectedDate: String,
    budget: Long,
    transactions: List<TransactionItem>,
    onSetBudgetClick: () -> Unit,
    onEditTransaction: (TransactionItem) -> Unit
) {
    val context = LocalContext.current
    val totalSpent = transactions.sumOf { it.amount }
    val remaining = budget - totalSpent

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // App Title Section with active monitoring indicator if Ortu
        if (user.role == "ORTU") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = "Pantau",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Mode Pantau Orang Tua 👨‍👩‍👦",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = if (user.isLinked) "Memantau: ${user.linkedEmail}" else "Belum terhubung ke anak",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (user.role == "ORTU" && user.isLinked) "Kantong Anak Sekolah 🎒" else "SakuPelajar 🎒",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (user.role == "ORTU") "Porsi jajan anak terpantau aman terkontrol." else "Hemat pangkal kaya, yuk catat jajanmu!",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            IconButton(
                onClick = {
                    val tips = listOf(
                        "Bawa botol minum sendiri ke sekolah bisa hemat Rp 5.000 sehari lho!",
                        "Yuk kurangi jajan boba demi tabungan masa depan!",
                        "Bikin bekal dari rumah selain lebih hemat juga sehat!",
                        "Coba tabung uang kembalian jajan di celengan fisikmu juga!",
                        "Belanja buku bekas atau pinjam di perpustakaan sekolah untuk hemat!"
                    )
                    Toast.makeText(context, tips.random(), Toast.LENGTH_LONG).show()
                },
                modifier = Modifier.testTag("tips_button")
            ) {
                Icon(Icons.Filled.TipsAndUpdates, contentDescription = "Tips Hari Ini", tint = Color(0xFFFFC107))
            }
        }

        // Parent onboarding to link child
        if (user.role == "ORTU" && !user.isLinked) {
            ParentOnboardingCard(viewModel = viewModel)
            Spacer(modifier = Modifier.height(24.dp))
        } else {
            // Date Picker Bar
            DateNavigationSection(viewModel = viewModel, selectedDate = selectedDate)

            Spacer(modifier = Modifier.height(16.dp))

            // Pocket Money Status Card (Dashboard)
            PocketMoneyStatusCard(
                budget = budget,
                totalSpent = totalSpent,
                remaining = remaining,
                onSetBudgetClick = onSetBudgetClick,
                isEditable = user.role == "ANAK"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Transactions Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (user.role == "ORTU") "Catatan Jajan Anak Hari Ini" else "Catatan Belanja Hari Ini",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${transactions.size} Pengeluaran",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Today's Transactions List
            if (transactions.isEmpty()) {
                EmptyTransactionsState(onSetBudgetClick)
            } else {
                transactions.forEach { transaction ->
                    TransactionListItem(
                        transaction = transaction,
                        onClick = { onEditTransaction(transaction) },
                        onDelete = { viewModel.deleteTransaction(transaction) },
                        isEditable = user.role == "ANAK"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun ParentOnboardingCard(
    viewModel: SakuViewModel
) {
    var childEmail by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var hasSentOtp by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.FamilyRestroom,
                    contentDescription = "Ortu",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Koneksi Akun Anak 👦👧",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Pantau uang jajan harian dan riwayat belanja anak Anda secara real-time. Masukkan email Google anak Anda di bawah.",
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            if (!hasSentOtp) {
                OutlinedTextField(
                    value = childEmail,
                    onValueChange = { childEmail = it },
                    label = { Text("Email Google Anak") },
                    leadingIcon = { Icon(Icons.Default.Mail, contentDescription = "Email Anak") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (childEmail.isBlank() || !childEmail.contains("@")) {
                            Toast.makeText(context, "Harap masukkan email anak yang valid!", Toast.LENGTH_SHORT).show()
                        } else {
                            isLoading = true
                            viewModel.requestOtpForChild(childEmail.trim().lowercase()) { success, msg ->
                                isLoading = false
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                if (success) {
                                    hasSentOtp = true
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text("Kirim Kode OTP Verifikasi")
                    }
                }
            } else {
                Text(
                    text = "Kode OTP verifikasi telah dikirim ke: $childEmail",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = otpCode,
                    onValueChange = { otpCode = it },
                    label = { Text("Masukkan 4 Digit Kode OTP") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "OTP") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { hasSentOtp = false; otpCode = "" },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Kembali")
                    }
                    Button(
                        onClick = {
                            if (otpCode.length != 4) {
                                Toast.makeText(context, "OTP harus terdiri dari 4 digit angka!", Toast.LENGTH_SHORT).show()
                            } else {
                                isLoading = true
                                viewModel.verifyOtpAndLink(childEmail.trim().lowercase(), otpCode.trim()) { success, msg ->
                                    isLoading = false
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            Text("Verifikasi")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DateNavigationSection(
    viewModel: SakuViewModel,
    selectedDate: String
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val calendar = Calendar.getInstance()
    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val currentDateObj = try { parser.parse(selectedDate) ?: Date() } catch (e: Exception) { Date() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = {
                calendar.time = currentDateObj
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                viewModel.selectDate(sdf.format(calendar.time))
            },
            modifier = Modifier.testTag("prev_day_button")
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Sebelumnya")
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .clickable {
                    viewModel.selectDate(sdf.format(Date()))
                }
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = "Kalender",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedDate == sdf.format(Date())) "Hari Ini" else formatDateForDisplay(selectedDate),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }

        IconButton(
            onClick = {
                calendar.time = currentDateObj
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                viewModel.selectDate(sdf.format(calendar.time))
            },
            modifier = Modifier.testTag("next_day_button")
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Selanjutnya")
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Weekly Horizontal Date Bar
    val todayStr = sdf.format(Date())
    val dates = remember(selectedDate) {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        cal.time = currentDateObj
        cal.add(Calendar.DAY_OF_YEAR, -3)
        for (i in 0 until 7) {
            list.add(sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        dates.forEach { dateStr ->
            val isSelected = dateStr == selectedDate
            val isToday = dateStr == todayStr
            val parsedDate = try { parser.parse(dateStr) ?: Date() } catch (e: Exception) { Date() }
            val cal = Calendar.getInstance().apply { time = parsedDate }
            val dayNum = cal.get(Calendar.DAY_OF_MONTH).toString()
            val dayName = formatDayOfWeek(dateStr)

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else -> Color.Transparent
                        }
                    )
                    .clickable {
                        viewModel.selectDate(dateStr)
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .width(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dayName,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dayNum,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
fun PocketMoneyStatusCard(
    budget: Long,
    totalSpent: Long,
    remaining: Long,
    onSetBudgetClick: () -> Unit,
    isEditable: Boolean
) {
    val progress = if (budget > 0) (totalSpent.toFloat() / budget.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800),
        label = "Progress"
    )

    val progressColor = when {
        progress >= 1.0f -> Color(0xFFF44336)
        progress >= 0.8f -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Uang Saku",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatRupiah(budget),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (isEditable) {
                    Button(
                        onClick = onSetBudgetClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("set_budget_button")
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Atur", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = if (budget > 0) "Atur" else "Set Saku", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gauge Bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Summary Bottom Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE57373))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Terpakai", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    Text(
                        text = formatRupiah(totalSpent),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (remaining >= 0) Color(0xFF81C784) else Color(0xFFE57373))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (remaining >= 0) "Sisa Saku" else "Tekor / Minus", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    Text(
                        text = formatRupiah(remaining),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (remaining >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }

            // Warnings/Motivations based on remaining status
            if (budget > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))

                val statusText = when {
                    remaining < 0 -> "Oops! Pengeluaran sudah tekor ${formatRupiah(-remaining)}."
                    remaining == 0L -> "Waduh, uang saku pas-pasan habis! Hati-hati."
                    totalSpent.toFloat() / budget.toFloat() > 0.8f -> "Sisa sedikit lagi! Hemat-hemat belanja hari ini."
                    totalSpent == 0L -> "Mantap! Belum jajan sama sekali hari ini. Pertahankan!"
                    else -> "Bagus! Pengeluaran terkontrol dengan aman hari ini."
                }
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (remaining < 0) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "💡 Uang saku hari ini belum diatur.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun EmptyTransactionsState(
    onSetBudgetClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(120.dp)
                .padding(8.dp)
        ) {
            // Draw a cute wallet shape
            drawRoundRect(
                color = Color(0xFFE1BEE7),
                topLeft = Offset(size.width * 0.15f, size.height * 0.25f),
                size = Size(size.width * 0.7f, size.height * 0.55f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
            )
            drawRoundRect(
                color = Color(0xFFBA68C8),
                topLeft = Offset(size.width * 0.5f, size.height * 0.35f),
                size = Size(size.width * 0.38f, size.height * 0.25f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
            )
            drawCircle(
                color = Color(0xFFFFD54F),
                radius = 14f,
                center = Offset(size.width * 0.45f, size.height * 0.18f)
            )
            drawCircle(
                color = Color(0xFFFFB300),
                radius = 7f,
                center = Offset(size.width * 0.45f, size.height * 0.18f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Belum Ada Pengeluaran",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Klik tombol \"Catat Jajan\" di kanan bawah untuk mencatat jajan pertama!",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionListItem(
    transaction: TransactionItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    isEditable: Boolean = true
) {
    val category = SakuCategory.fromString(transaction.category)
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                enabled = isEditable,
                onClick = onClick,
                onLongClick = { if (isEditable) showMenu = true }
            )
            .testTag("transaction_item_${transaction.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(category.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = category.label,
                    tint = category.color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transaction.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = category.label,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                if (transaction.notes.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Notes,
                            contentDescription = "Catatan",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = transaction.notes,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "- " + formatRupiah(transaction.amount),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = Color(0xFFD32F2F)
                )
                if (isEditable) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp).testTag("delete_transaction_${transaction.id}")
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Hapus",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    if (showMenu) {
        AlertDialog(
            onDismissRequest = { showMenu = false },
            title = { Text("Pilihan Transaksi") },
            text = { Text("Apakah kamu ingin menghapus pengeluaran \"${transaction.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showMenu = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD32F2F))
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMenu = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

fun getAvailableMonths(transactions: List<TransactionItem>, budgets: List<DailyBudget>): List<String> {
    val months = mutableSetOf<String>()
    
    transactions.forEach { tx ->
        if (tx.dateString.length >= 7) {
            months.add(tx.dateString.substring(0, 7))
        }
    }
    
    budgets.forEach { bg ->
        if (bg.dateString.length >= 7) {
            months.add(bg.dateString.substring(0, 7))
        }
    }
    
    if (months.isEmpty()) {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        months.add(sdf.format(Date()))
    }
    
    return months.toList().sortedDescending()
}

fun formatYearMonth(yearMonth: String): String {
    try {
        val parser = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val date = parser.parse(yearMonth) ?: return yearMonth
        val formatter = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
        return formatter.format(date)
    } catch (e: Exception) {
        return yearMonth
    }
}

fun generateTextReport(
    periodStr: String,
    selectedYearMonth: String?,
    user: UserAccount,
    transactions: List<TransactionItem>,
    budgets: List<DailyBudget>
): String {
    val filteredTx = if (selectedYearMonth != null) {
        transactions.filter { it.dateString.startsWith(selectedYearMonth) }
    } else {
        transactions
    }
    
    val filteredBudgets = if (selectedYearMonth != null) {
        budgets.filter { it.dateString.startsWith(selectedYearMonth) }
    } else {
        budgets
    }
    
    val totalBudget = filteredBudgets.sumOf { it.amount }
    val totalSpent = filteredTx.sumOf { it.amount }
    val remaining = totalBudget - totalSpent
    val remainingStatus = if (remaining >= 0) "HEMAT" else "MINUS/TEKOR"
    
    val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm", Locale("id", "ID"))
    val printDate = sdf.format(Date())
    
    val sb = StringBuilder()
    sb.append("=========================================\n")
    sb.append("      LAPORAN BULANAN SAKUPELAJAR 🎒     \n")
    sb.append("=========================================\n")
    sb.append("Periode       : $periodStr\n")
    sb.append("Nama Pembuat  : ${user.displayName}\n")
    sb.append("Email Google  : ${user.email}\n")
    sb.append("Peran Akun    : ${if (user.role == "ORTU") "Orang Tua" else "Anak Sekolah"}\n")
    if (user.isLinked && user.linkedEmail != null) {
        sb.append("Terkoneksi dg : ${user.linkedEmail}\n")
    }
    sb.append("Waktu Ekspor  : $printDate WIB\n")
    sb.append("-----------------------------------------\n")
    sb.append("         RINGKASAN STATUS SAKU           \n")
    sb.append("-----------------------------------------\n")
    sb.append("Total Saku    : ${formatRupiah(totalBudget)}\n")
    sb.append("Total Belanja : ${formatRupiah(totalSpent)}\n")
    sb.append("Sisa Saku     : ${formatRupiah(remaining)} ($remainingStatus)\n")
    sb.append("-----------------------------------------\n")
    sb.append("        RIWAYAT DETAIL BELANJA           \n")
    sb.append("-----------------------------------------\n")
    
    if (filteredTx.isEmpty()) {
        sb.append("(Tidak ada catatan pengeluaran)\n")
    } else {
        filteredTx.sortedBy { it.dateString }.forEachIndexed { index, tx ->
            sb.append("${index + 1}. [${tx.dateString}] - ${tx.category}\n")
            sb.append("   Judul  : ${tx.title}\n")
            sb.append("   Jumlah : ${formatRupiah(tx.amount)}\n")
            if (tx.notes.isNotBlank()) {
                sb.append("   Catatn : ${tx.notes}\n")
            }
            sb.append("\n")
        }
    }
    
    sb.append("=========================================\n")
    sb.append("      Di-generate oleh SakuPelajar App   \n")
    sb.append("        - Cerdas Mengelola Saku -        \n")
    sb.append("=========================================\n")
    return sb.toString()
}

fun generateCsvReport(
    selectedYearMonth: String?,
    transactions: List<TransactionItem>,
    budgets: List<DailyBudget>
): String {
    val filteredTx = if (selectedYearMonth != null) {
        transactions.filter { it.dateString.startsWith(selectedYearMonth) }
    } else {
        transactions
    }
    
    val filteredBudgets = if (selectedYearMonth != null) {
        budgets.filter { it.dateString.startsWith(selectedYearMonth) }
    } else {
        budgets
    }
    
    val sb = StringBuilder()
    sb.append("Tanggal,Judul,Kategori,Nominal,Catatan,Saku Harian\n")
    
    filteredTx.sortedBy { it.dateString }.forEach { tx ->
        val dailyBudgetObj = filteredBudgets.firstOrNull { it.dateString == tx.dateString }
        val budgetVal = dailyBudgetObj?.amount?.toString() ?: ""
        
        val titleEscaped = tx.title.replace("\"", "\"\"")
        val notesEscaped = tx.notes.replace("\"", "\"\"")
        val categoryEscaped = tx.category.replace("\"", "\"\"")
        
        sb.append("\"${tx.dateString}\",\"$titleEscaped\",\"$categoryEscaped\",${tx.amount},\"$notesEscaped\",$budgetVal\n")
    }
    
    val txDates = filteredTx.map { it.dateString }.toSet()
    filteredBudgets.filter { it.dateString !in txDates }.sortedBy { it.dateString }.forEach { bg ->
        sb.append("\"${bg.dateString}\",\"\",\"\",0,\"\",${bg.amount}\n")
    }
    
    return sb.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportReportDialog(
    user: UserAccount,
    transactions: List<TransactionItem>,
    budgets: List<DailyBudget>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    val availableMonths = remember(transactions, budgets) {
        getAvailableMonths(transactions, budgets)
    }
    
    var selectedPeriodKey by remember { mutableStateOf<String?>("all") }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    
    val selectedPeriodLabel = remember(selectedPeriodKey, availableMonths) {
        if (selectedPeriodKey == "all") {
            "Semua Riwayat"
        } else {
            formatYearMonth(selectedPeriodKey!!)
        }
    }
    
    val filteredTx = remember(selectedPeriodKey, transactions) {
        if (selectedPeriodKey == "all" || selectedPeriodKey == null) {
            transactions
        } else {
            transactions.filter { it.dateString.startsWith(selectedPeriodKey!!) }
        }
    }
    
    val filteredBudgets = remember(selectedPeriodKey, budgets) {
        if (selectedPeriodKey == "all" || selectedPeriodKey == null) {
            budgets
        } else {
            budgets.filter { it.dateString.startsWith(selectedPeriodKey!!) }
        }
    }
    
    val totalBudget = filteredBudgets.sumOf { it.amount }
    val totalSpent = filteredTx.sumOf { it.amount }
    val remaining = totalBudget - totalSpent
    val remainingColor = if (remaining >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(8.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = "Ekspor Laporan",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Ekspor Laporan Saku 📈",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "Ekspor ringkasan saku harian dan catatan pengeluaran ke dalam format teks atau spreadsheet.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )
                
                Text(
                    text = "Pilih Periode:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.align(Alignment.Start)
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { isDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedPeriodLabel,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                        }
                    }
                    
                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Semua Riwayat") },
                            onClick = {
                                selectedPeriodKey = "all"
                                isDropdownExpanded = false
                            }
                        )
                        availableMonths.forEach { yearMonth ->
                            DropdownMenuItem(
                                text = { Text(formatYearMonth(yearMonth)) },
                                onClick = {
                                    selectedPeriodKey = yearMonth
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Ringkasan Laporan (${selectedPeriodLabel})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Saku:", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                            Text(formatRupiah(totalBudget), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Belanja:", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                            Text(formatRupiah(totalSpent), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF44336))
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Sisa Saku:", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = formatRupiah(remaining),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = remainingColor
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Jumlah Transaksi: ${filteredTx.size} Catatan",
                            fontSize = 11.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        val reportText = generateTextReport(
                            periodStr = selectedPeriodLabel,
                            selectedYearMonth = if (selectedPeriodKey == "all") null else selectedPeriodKey,
                            user = user,
                            transactions = transactions,
                            budgets = budgets
                        )
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Laporan Keuangan SakuPelajar - $selectedPeriodLabel")
                            putExtra(Intent.EXTRA_TEXT, reportText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Bagikan Laporan"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bagikan Laporan (Teks)", fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val reportText = generateTextReport(
                                periodStr = selectedPeriodLabel,
                                selectedYearMonth = if (selectedPeriodKey == "all") null else selectedPeriodKey,
                                user = user,
                                transactions = transactions,
                                budgets = budgets
                            )
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(reportText))
                            Toast.makeText(context, "Laporan Teks disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Salin Teks", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salin Teks", fontSize = 11.sp, maxLines = 1)
                    }
                    
                    OutlinedButton(
                        onClick = {
                            val csvText = generateCsvReport(
                                selectedYearMonth = if (selectedPeriodKey == "all") null else selectedPeriodKey,
                                transactions = transactions,
                                budgets = budgets
                            )
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(csvText))
                            Toast.makeText(context, "Format CSV disalin ke clipboard! Siap di-paste ke Excel / Sheets.", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = "Salin CSV", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salin CSV", fontSize = 11.sp, maxLines = 1)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tutup", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    viewModel: SakuViewModel,
    user: UserAccount,
    allTransactions: List<TransactionItem>,
    onEditTransaction: (TransactionItem) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterCategory by remember { mutableStateOf<String?>(null) }
    var sortBy by remember { mutableStateOf("terbaru") }

    // Period filter state
    var filterType by remember { mutableStateOf("all") } // "all", "date", "month", "year"
    var selectedDateFilter by remember { mutableStateOf("") } // YYYY-MM-DD
    var selectedMonthFilter by remember { mutableStateOf("") } // YYYY-MM
    var selectedYearFilter by remember { mutableStateOf("") } // YYYY

    if (user.role == "ORTU" && !user.isLinked) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Harap tautkan akun Anak terlebih dahulu di Beranda!", color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
        }
        return
    }

    val processedTransactions = remember(allTransactions, searchQuery, selectedFilterCategory, sortBy, filterType, selectedDateFilter, selectedMonthFilter, selectedYearFilter) {
        var list = allTransactions

        // Apply period filter
        list = when (filterType) {
            "date" -> {
                if (selectedDateFilter.isNotBlank()) {
                    list.filter { it.dateString == selectedDateFilter }
                } else list
            }
            "month" -> {
                if (selectedMonthFilter.isNotBlank()) {
                    list.filter { it.dateString.startsWith(selectedMonthFilter) }
                } else list
            }
            "year" -> {
                if (selectedYearFilter.isNotBlank()) {
                    list.filter { it.dateString.startsWith(selectedYearFilter) }
                } else list
            }
            else -> list
        }

        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.notes.contains(searchQuery, ignoreCase = true)
            }
        }
        if (selectedFilterCategory != null) {
            list = list.filter { it.category == selectedFilterCategory }
        }
        when (sortBy) {
            "terbaru" -> list.sortedByDescending { it.timestamp }
            "terlama" -> list.sortedBy { it.timestamp }
            "termahal" -> list.sortedByDescending { it.amount }
            "termurah" -> list.sortedBy { it.amount }
            else -> list
        }
    }

    val groupedTransactions = remember(processedTransactions) {
        processedTransactions.groupBy { it.dateString }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (user.role == "ORTU") "Histori Jajan Anak 📜" else "Histori Belanjaku 📜",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            var showExportDialog by remember { mutableStateOf(false) }

            Button(
                onClick = { showExportDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("export_report_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Ekspor Laporan",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ekspor", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            if (showExportDialog) {
                val budgets by viewModel.allBudgets.collectAsStateWithLifecycle()
                ExportReportDialog(
                    user = user,
                    transactions = allTransactions,
                    budgets = budgets,
                    onDismiss = { showExportDialog = false }
                )
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari jajanan...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_input"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Period Filter Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Saring Berdasarkan Waktu:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filterTypes = listOf(
                        Triple("all", "Semua", Icons.Default.DoneAll),
                        Triple("date", "Tanggal", Icons.Default.CalendarToday),
                        Triple("month", "Bulan", Icons.Default.Event),
                        Triple("year", "Tahun", Icons.Default.DateRange)
                    )
                    
                    filterTypes.forEach { (type, label, icon) ->
                        val isSelected = filterType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { 
                                filterType = type 
                            },
                            label = { Text(label, fontSize = 11.sp) },
                            leadingIcon = { Icon(icon, contentDescription = label, modifier = Modifier.size(12.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                if (filterType != "all") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        when (filterType) {
                            "date" -> {
                                val displayDate = if (selectedDateFilter.isNotBlank()) {
                                    formatDateForDisplay(selectedDateFilter)
                                } else {
                                    "Belum memilih tanggal"
                                }
                                Text(displayDate, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                Button(
                                    onClick = {
                                        val calendar = Calendar.getInstance()
                                        if (selectedDateFilter.isNotBlank()) {
                                            try {
                                                val parts = selectedDateFilter.split("-")
                                                calendar.set(Calendar.YEAR, parts[0].toInt())
                                                calendar.set(Calendar.MONTH, parts[1].toInt() - 1)
                                                calendar.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                                            } catch (e: Exception) {}
                                        }
                                        android.app.DatePickerDialog(
                                            context,
                                            { _, year, month, dayOfMonth ->
                                                selectedDateFilter = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                            },
                                            calendar.get(Calendar.YEAR),
                                            calendar.get(Calendar.MONTH),
                                            calendar.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp).testTag("select_date_filter_button")
                                ) {
                                    Text("Pilih Tanggal", fontSize = 11.sp)
                                }
                            }
                            "month" -> {
                                val displayMonth = if (selectedMonthFilter.isNotBlank()) {
                                    formatYearMonth(selectedMonthFilter)
                                } else {
                                    "Belum memilih bulan"
                                }
                                Text(displayMonth, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                
                                val uniqueMonths = remember(allTransactions) {
                                    allTransactions.map { it.dateString.take(7) }.distinct().sortedDescending()
                                }
                                
                                var monthDropdownExpanded by remember { mutableStateOf(false) }
                                Box {
                                    Button(
                                        onClick = { monthDropdownExpanded = true },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp).testTag("select_month_filter_button")
                                    ) {
                                        Text("Pilih Bulan", fontSize = 11.sp)
                                    }
                                    DropdownMenu(
                                        expanded = monthDropdownExpanded,
                                        onDismissRequest = { monthDropdownExpanded = false }
                                    ) {
                                        if (uniqueMonths.isEmpty()) {
                                            DropdownMenuItem(text = { Text("Tidak ada data") }, onClick = {})
                                        } else {
                                            uniqueMonths.forEach { ym ->
                                                DropdownMenuItem(
                                                    text = { Text(formatYearMonth(ym)) },
                                                    onClick = {
                                                        selectedMonthFilter = ym
                                                        monthDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            "year" -> {
                                val displayYear = if (selectedYearFilter.isNotBlank()) {
                                    selectedYearFilter
                                } else {
                                    "Belum memilih tahun"
                                }
                                Text(displayYear, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                
                                val uniqueYears = remember(allTransactions) {
                                    allTransactions.map { it.dateString.take(4) }.distinct().sortedDescending()
                                }
                                
                                var yearDropdownExpanded by remember { mutableStateOf(false) }
                                Box {
                                    Button(
                                        onClick = { yearDropdownExpanded = true },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp).testTag("select_year_filter_button")
                                    ) {
                                        Text("Pilih Tahun", fontSize = 11.sp)
                                    }
                                    DropdownMenu(
                                        expanded = yearDropdownExpanded,
                                        onDismissRequest = { yearDropdownExpanded = false }
                                    ) {
                                        if (uniqueYears.isEmpty()) {
                                            DropdownMenuItem(text = { Text("Tidak ada data") }, onClick = {})
                                        } else {
                                            uniqueYears.forEach { yr ->
                                                DropdownMenuItem(
                                                    text = { Text(yr) },
                                                    onClick = {
                                                        selectedYearFilter = yr
                                                        yearDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedFilterCategory == null,
                    onClick = { selectedFilterCategory = null },
                    label = { Text("Semua") }
                )
            }
            items(SakuCategory.entries.toList()) { category ->
                FilterChip(
                    selected = selectedFilterCategory == category.label,
                    onClick = { selectedFilterCategory = category.label },
                    label = { Text(category.label) },
                    leadingIcon = {
                        Icon(
                            category.icon,
                            contentDescription = category.label,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total: ${formatRupiah(processedTransactions.sumOf { it.amount })}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Urutan: ", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                Box {
                    var showDropdown by remember { mutableStateOf(false) }
                    TextButton(
                        onClick = { showDropdown = true },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = when (sortBy) {
                                "terbaru" -> "Terbaru"
                                "terlama" -> "Terlama"
                                "termahal" -> "Termahal"
                                "termurah" -> "Termurah"
                                else -> "Terbaru"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Pilih")
                    }
                    DropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Terbaru") },
                            onClick = { sortBy = "terbaru"; showDropdown = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Terlama") },
                            onClick = { sortBy = "terlama"; showDropdown = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Termahal") },
                            onClick = { sortBy = "termahal"; showDropdown = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Termurah") },
                            onClick = { sortBy = "termurah"; showDropdown = false }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (groupedTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.SearchOff,
                        contentDescription = "Tidak ditemukan",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Transaksi tidak ditemukan!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                groupedTransactions.forEach { (dateStr, list) ->
                    stickyHeader {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(vertical = 6.dp)
                        ) {
                            Text(
                                text = formatDateForDisplay(dateStr),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                        }
                    }

                    items(list) { transaction ->
                        TransactionListItem(
                            transaction = transaction,
                            onClick = { onEditTransaction(transaction) },
                            onDelete = { viewModel.deleteTransaction(transaction) },
                            isEditable = user.role == "ANAK"
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticsScreen(
    user: UserAccount,
    allTransactions: List<TransactionItem>,
    allBudgets: List<DailyBudget>
) {
    if (user.role == "ORTU" && !user.isLinked) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Harap tautkan akun Anak terlebih dahulu di Beranda!", color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
        }
        return
    }

    val scrollState = rememberScrollState()
    val totalAllSpent = allTransactions.sumOf { it.amount }
    val totalBudgets = allBudgets.sumOf { it.amount }

    val categoryTotals = remember(allTransactions) {
        val map = mutableMapOf<String, Long>()
        allTransactions.forEach { tx ->
            map[tx.category] = (map[tx.category] ?: 0L) + tx.amount
        }
        map.toList().sortedByDescending { it.second }
    }

    val last7DaysStats = remember(allTransactions) {
        val list = mutableListOf<Pair<String, Long>>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        for (i in 6 downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -i)
            val dStr = sdf.format(c.time)
            val totalForDay = allTransactions.filter { it.dateString == dStr }.sumOf { it.amount }
            list.add(Pair(formatShortDate(dStr), totalForDay))
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = if (user.role == "ORTU") "Analisis Kantong Anak 📊" else "Analisis Kantongku 📊",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Icon(Icons.Default.Savings, contentDescription = "Tabungan", tint = Color(0xFF4CAF50))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Total Set Saku", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    Text(formatRupiah(totalBudgets), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Icon(Icons.Default.TrendingDown, contentDescription = "Spent", tint = Color(0xFFF44336))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Total Belanja", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    Text(formatRupiah(totalAllSpent), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (allTransactions.isNotEmpty()) {
            Text(
                text = "Pembagian Belanja Kategori",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var startAngle = 0f
                            categoryTotals.forEach { (categoryLabel, amt) ->
                                val category = SakuCategory.fromString(categoryLabel)
                                val sweepAngle = (amt.toFloat() / totalAllSpent.toFloat()) * 360f
                                drawArc(
                                    color = category.color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = 24f, cap = StrokeCap.Round)
                                )
                                startAngle += sweepAngle
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Banyak Jajan", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = allTransactions.size.toString(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categoryTotals.take(4).forEach { (categoryLabel, amt) ->
                            val category = SakuCategory.fromString(categoryLabel)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(category.color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = category.label,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = formatRupiah(amt),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Tren Pengeluaran 7 Hari Terakhir",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    val maxSpent = (last7DaysStats.maxOfOrNull { it.second } ?: 1L).coerceAtLeast(1L)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        last7DaysStats.forEach { (dayLabel, spent) ->
                            val barHeightFraction = (spent.toFloat() / maxSpent.toFloat()).coerceIn(0.05f, 1f)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (spent > 0) formatRupiah(spent).substringAfter(" ") else "",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight(barHeightFraction)
                                        .width(16.dp)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.primaryContainer
                                                )
                                            )
                                        )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = dayLabel,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.BarChart, contentDescription = "Tidak ada data", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Belum Ada Data Statistik",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Catat pengeluaran terlebih dahulu agar grafik tren dapat muncul di sini.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(
    viewModel: SakuViewModel,
    user: UserAccount
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(if (user.role == "ORTU") "👨‍👩‍👦" else "🎒", fontSize = 48.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = user.displayName,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = user.email,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Badge indicating role
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (user.role == "ORTU") "AKUN ORANG TUA" else "AKUN ANAK SEKOLAH",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Account Link Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Hubungan Akun Keluarga",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (user.isLinked) {
                    var selectedProfileEmailForDialog by remember { mutableStateOf<String?>(null) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Linked",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (user.role == "ORTU") "Terhubung dengan Anak (Tap untuk Profil):" else "Terhubung dengan Ortu (Tap untuk Profil):",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // List out the linked emails as clickable cards
                    val linkedEmails = remember(user.linkedEmail) {
                        user.linkedEmail?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
                    }

                    linkedEmails.forEach { email ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedProfileEmailForDialog = email },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (user.role == "ORTU") Icons.Default.Person else Icons.Default.FamilyRestroom,
                                        contentDescription = "User",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = email,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Lihat Profil",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Detail",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    selectedProfileEmailForDialog?.let { targetEmail ->
                        LinkedUserProfileDialog(
                            email = targetEmail,
                            viewModel = viewModel,
                            onDismiss = { selectedProfileEmailForDialog = null }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            viewModel.unlinkAccounts()
                            Toast.makeText(context, "Koneksi akun diputuskan!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Putuskan Hubungan Akun")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Unlinked",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Belum Terhubung dengan Akun " + if (user.role == "ORTU") "Anak" else "Orang Tua",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    if (user.role == "ANAK") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "💡 Minta Orang Tua mengaitkan akun dengan memasukkan email kamu (${user.email}) di aplikasi SakuPelajar mereka.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(24.dp))

        // Log out button
        Button(
            onClick = {
                viewModel.logout()
                Toast.makeText(context, "Berhasil Keluar Akun!", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Logout, contentDescription = "Keluar")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keluar Akun", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LinkedUserProfileDialog(
    email: String,
    viewModel: SakuViewModel,
    onDismiss: () -> Unit
) {
    val userFlow = remember(email) { viewModel.getUserByEmail(email) }
    val targetUser by userFlow.collectAsStateWithLifecycle(initialValue = null)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = "Profil Keluarga Terhubung",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                if (targetUser == null) {
                    // Loading or user not logged in yet / not created
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 24.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Memuat data profil untuk $email...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val u = targetUser!!
                    // Mascot Avatar
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (u.role == "ORTU") "👨‍👩‍👦" else "🎒", fontSize = 36.sp)
                    }

                    // User Info Rows
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileDetailRow(label = "Nama Lengkap", value = u.displayName, icon = Icons.Default.Person)
                        ProfileDetailRow(label = "Alamat Email", value = u.email, icon = Icons.Default.Mail)
                        ProfileDetailRow(
                            label = "Nomor WhatsApp", 
                            value = if (u.whatsappNumber.isNotBlank()) u.whatsappNumber else "Belum diisi", 
                            icon = Icons.Default.Phone
                        )
                        ProfileDetailRow(
                            label = "Peran", 
                            value = if (u.role == "ORTU") "Orang Tua" else "Anak Sekolah", 
                            icon = Icons.Default.FamilyRestroom
                        )
                    }
                }

                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Tutup")
                }
            }
        }
    }
}

@Composable
fun ProfileDetailRow(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun AddEditTransactionDialog(
    selectedDate: String,
    editingTransaction: TransactionItem?,
    onDismiss: () -> Unit,
    onSave: (String, Long, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf(editingTransaction?.title ?: "") }
    var amountString by remember { mutableStateOf(editingTransaction?.amount?.toString() ?: "") }
    var selectedCategory by remember { mutableStateOf(editingTransaction?.category ?: SakuCategory.MAKANAN.label) }
    var notes by remember { mutableStateOf(editingTransaction?.notes ?: "") }
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (editingTransaction == null) "Catat Pengeluaran Baru" else "Edit Catatan Jajan",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nama Barang / Jajanan") },
                    placeholder = { Text("Misal: Bakso, Es Teh, Buku Tulis") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amountString,
                    onValueChange = { amountString = it },
                    label = { Text("Jumlah Uang (Rupiah)") },
                    placeholder = { Text("Misal: 15000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Category dropdown selector
                var expanded by remember { mutableStateOf(false) }
                val currentCategoryObj = SakuCategory.fromString(selectedCategory)

                Column {
                    Text("Pilih Kategori:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = currentCategoryObj.icon,
                                    contentDescription = currentCategoryObj.label,
                                    tint = currentCategoryObj.color,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = "Pilih Kategori",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Invisible overlay to reliably capture clicks on the dropdown box
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { expanded = !expanded }
                        )

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            SakuCategory.entries.forEach { category ->
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = category.icon,
                                            contentDescription = category.label,
                                            tint = category.color,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = category.label,
                                            fontWeight = if (selectedCategory == category.label) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedCategory == category.label) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        selectedCategory = category.label
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Tambahan (Opsional)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            val amount = amountString.toLongOrNull()
                            if (title.isBlank() || amount == null || amount <= 0L) {
                                Toast.makeText(context, "Judul & jumlah pengeluaran harus valid!", Toast.LENGTH_SHORT).show()
                            } else {
                                onSave(title, amount, selectedCategory, selectedDate, notes)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

@Composable
fun SetBudgetDialog(
    selectedDate: String,
    currentBudget: Long,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    var budgetString by remember { mutableStateOf(if (currentBudget > 0L) currentBudget.toString() else "") }
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Atur Uang Saku Hari Ini",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Masukkan nominal batas uang saku harian untuk tanggal ${formatShortDate(selectedDate)}.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                OutlinedTextField(
                    value = budgetString,
                    onValueChange = { budgetString = it },
                    label = { Text("Batas Uang Saku (Rupiah)") },
                    placeholder = { Text("Misal: 30000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            val budget = budgetString.toLongOrNull()
                            if (budget == null || budget < 0L) {
                                Toast.makeText(context, "Batas jajan harus diisi angka valid!", Toast.LENGTH_SHORT).show()
                            } else {
                                onSave(budget)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}
