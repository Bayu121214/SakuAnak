package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class SakuViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("saku_prefs", Context.MODE_PRIVATE)

    private val database = Room.databaseBuilder(
        application,
        SakuDatabase::class.java,
        "saku_pelajar_db"
    ).fallbackToDestructiveMigration().build()

    private val repository = SakuRepository(database.sakuDao())

    // 1. Current Logged-in User Session Flow
    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    // 2. Active view target email (whose data we are showing)
    val targetEmail: StateFlow<String> = _currentUser.flatMapLatest { user ->
        if (user == null) {
            flowOf("")
        } else if (user.role == "ORTU" && user.isLinked && user.linkedEmail != null) {
            flowOf(user.linkedEmail)
        } else {
            flowOf(user.email)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Real-time user account database flow to keep linking status synced
    val currentUserDb: StateFlow<UserAccount?> = _currentUser.flatMapLatest { user ->
        if (user != null) {
            repository.getUserByEmailFlow(user.email)
        } else {
            flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private val _selectedDateString = MutableStateFlow(getTodayDateString())
    val selectedDateString: StateFlow<String> = _selectedDateString.asStateFlow()

    // Trigger Flow for refreshing budget/transactions
    private val _refreshTrigger = MutableStateFlow(0)

    // Selected budget reacts to date changes, refresh triggers, and target email changes
    val selectedDateBudget: StateFlow<Long> = combine(_selectedDateString, targetEmail, _refreshTrigger) { date, email, _ ->
        if (email.isBlank()) 0L else repository.getBudgetForDate(date, email)?.amount ?: 0L
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Transactions for the selected date and target email
    val selectedDateTransactions: StateFlow<List<TransactionItem>> = combine(_selectedDateString, targetEmail) { date, email ->
        Pair(date, email)
    }.flatMapLatest { (date, email) ->
        if (email.isBlank()) {
            flowOf(emptyList())
        } else {
            repository.getTransactionsForDate(date, email)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All transactions for target email
    val allTransactions: StateFlow<List<TransactionItem>> = targetEmail.flatMapLatest { email ->
        if (email.isBlank()) {
            flowOf(emptyList())
        } else {
            repository.getAllTransactions(email)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All budgets for target email
    val allBudgets: StateFlow<List<DailyBudget>> = targetEmail.flatMapLatest { email ->
        if (email.isBlank()) {
            flowOf(emptyList())
        } else {
            repository.getAllBudgets(email)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Simulated Inbox/OTP Notification Flow
    private val _simulatedOtpNotification = MutableStateFlow<String?>(null)
    val simulatedOtpNotification: StateFlow<String?> = _simulatedOtpNotification.asStateFlow()

    init {
        // Auto-login if previously saved
        val savedEmail = sharedPrefs.getString("logged_in_email", null)
        if (savedEmail != null) {
            viewModelScope.launch {
                val user = repository.getUserByEmail(savedEmail)
                if (user != null) {
                    _currentUser.value = user
                }
            }
        }
    }

    // Google Sign-in Simulation
    fun loginWithGoogle(email: String, name: String, role: String, whatsappNumber: String) {
        viewModelScope.launch {
            // Check if user exists, else insert
            val existing = repository.getUserByEmail(email)
            val user = existing?.copy(
                displayName = name,
                role = role,
                whatsappNumber = whatsappNumber
            ) ?: UserAccount(
                email = email,
                displayName = name,
                role = role,
                whatsappNumber = whatsappNumber,
                isLinked = false
            )
            repository.insertUser(user)
            // Save state
            sharedPrefs.edit().putString("logged_in_email", email).apply()
            _currentUser.value = user
        }
    }

    fun logout() {
        sharedPrefs.edit().remove("logged_in_email").apply()
        _currentUser.value = null
    }

    fun getUserByEmail(email: String): Flow<UserAccount?> {
        return repository.getUserByEmailFlow(email)
    }

    // Budget Operations
    fun setBudgetForDate(date: String, amount: Long) {
        val email = targetEmail.value
        if (email.isBlank()) return
        viewModelScope.launch {
            repository.saveBudget(DailyBudget(date, email, amount))
            _refreshTrigger.value += 1
        }
    }

    // Transaction Operations
    fun addTransaction(title: String, amount: Long, category: String, date: String, notes: String) {
        val email = targetEmail.value
        if (email.isBlank()) return
        viewModelScope.launch {
            val transaction = TransactionItem(
                title = title,
                amount = amount,
                category = category,
                dateString = date,
                ownerEmail = email,
                notes = notes,
                timestamp = System.currentTimeMillis()
            )
            repository.insertTransaction(transaction)
        }
    }

    fun updateTransaction(transaction: TransactionItem) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: TransactionItem) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun selectDate(date: String) {
        _selectedDateString.value = date
    }

    // OTP and Account Linking Core Features
    // 1. Ortu requests OTP to be sent to Child's Email
    fun requestOtpForChild(childEmail: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val parent = _currentUser.value ?: return@launch

            // Auto-generate or check child account
            var child = repository.getUserByEmail(childEmail)
            if (child == null) {
                // To make testing extremely seamless and user-friendly, we register the child if not found!
                child = UserAccount(
                    email = childEmail,
                    displayName = childEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                    role = "ANAK"
                )
                repository.insertUser(child)
            } else if (child.role != "ANAK") {
                onResult(false, "Email tersebut sudah terdaftar sebagai akun Orang Tua!")
                return@launch
            }

            // Generate 4-digit OTP code
            val otpCode = (1000..9999).random().toString()

            // Save OTP code in child's database row
            val updatedChild = child.copy(pendingOtp = otpCode)
            repository.insertUser(updatedChild)

            // Trigger simulated OTP Notification/Alert
            _simulatedOtpNotification.value = "📧 [Simulasi Email SakuPelajar]\nDikirim ke: $childEmail\nSubjek: Kode OTP Verifikasi SakuPelajar\n\nKode OTP Anda adalah: $otpCode"

            onResult(true, "Kode OTP berhasil dikirim ke email Anak!")
        }
    }

    // 2. Ortu verifies the OTP child entered
    fun verifyOtpAndLink(childEmail: String, enteredOtp: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val parent = _currentUser.value ?: return@launch

            val child = repository.getUserByEmail(childEmail)
            if (child == null) {
                onResult(false, "Akun Anak tidak ditemukan!")
                return@launch
            }

            if (child.pendingOtp == enteredOtp) {
                // Check if child has already linked 5 parents
                val currentParents = child.linkedEmail?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                if (currentParents.size >= 5 && !currentParents.contains(parent.email)) {
                    onResult(false, "Batas maksimal 5 akun Orang Tua telah tercapai untuk anak ini!")
                    return@launch
                }

                // Append the parent to child's linked parents list
                val newParents = (currentParents + parent.email).distinct()
                val updatedParentsStr = newParents.joinToString(",")

                // Linking success! Update both rows
                val updatedParent = parent.copy(
                    linkedEmail = childEmail,
                    isLinked = true,
                    pendingOtp = null
                )
                val updatedChild = child.copy(
                    linkedEmail = updatedParentsStr,
                    isLinked = true,
                    pendingOtp = null
                )

                repository.insertUser(updatedParent)
                repository.insertUser(updatedChild)

                // Refresh current parent state session
                _currentUser.value = updatedParent
                _simulatedOtpNotification.value = null // Clear OTP simulated modal
                onResult(true, "Berhasil mengaitkan akun dengan Anak!")
            } else {
                onResult(false, "Kode OTP salah atau tidak valid!")
            }
        }
    }

    // 3. Unlink account
    fun unlinkAccounts() {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            val otherEmail = user.linkedEmail ?: return@launch

            if (user.role == "ORTU") {
                // ORTU links to 1 child (otherEmail is childEmail)
                val child = repository.getUserByEmail(otherEmail)
                
                // Update parent row
                val updatedParent = user.copy(linkedEmail = null, isLinked = false, pendingOtp = null)
                repository.insertUser(updatedParent)
                _currentUser.value = updatedParent

                if (child != null) {
                    val currentParents = child.linkedEmail?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                    val remainingParents = currentParents.filter { it != user.email }
                    val updatedChild = if (remainingParents.isEmpty()) {
                        child.copy(linkedEmail = null, isLinked = false, pendingOtp = null)
                    } else {
                        child.copy(linkedEmail = remainingParents.joinToString(","), isLinked = true)
                    }
                    repository.insertUser(updatedChild)
                }
            } else {
                // ANAK links to multiple parents (otherEmail is comma-separated parent emails)
                val parentEmails = otherEmail.split(",").filter { it.isNotBlank() }
                parentEmails.forEach { parentEmail ->
                    val parent = repository.getUserByEmail(parentEmail)
                    if (parent != null) {
                        val updatedParent = parent.copy(linkedEmail = null, isLinked = false, pendingOtp = null)
                        repository.insertUser(updatedParent)
                    }
                }

                // Update child row
                val updatedChild = user.copy(linkedEmail = null, isLinked = false, pendingOtp = null)
                repository.insertUser(updatedChild)
                _currentUser.value = updatedChild
            }
        }
    }

    fun dismissOtpNotification() {
        _simulatedOtpNotification.value = null
    }
}
