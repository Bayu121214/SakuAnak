package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "user_accounts")
data class UserAccount(
    @PrimaryKey val email: String,
    val displayName: String,
    val role: String, // "ANAK" or "ORTU"
    val linkedEmail: String? = null,
    val isLinked: Boolean = false,
    val pendingOtp: String? = null,
    val whatsappNumber: String = ""
)

@Entity(tableName = "daily_budgets", primaryKeys = ["dateString", "ownerEmail"])
data class DailyBudget(
    val dateString: String, // Format: YYYY-MM-DD
    val ownerEmail: String,
    val amount: Long
)

@Entity(tableName = "transactions")
data class TransactionItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Long,
    val category: String, // e.g., "Makanan", "Transportasi", "Pendidikan", "Belanja", "Lainnya"
    val dateString: String, // Format: YYYY-MM-DD
    val ownerEmail: String,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Dao
interface SakuDao {
    // Users
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccount)

    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserAccount?

    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    fun getUserByEmailFlow(email: String): Flow<UserAccount?>

    @Query("SELECT * FROM user_accounts")
    fun getAllUsersFlow(): Flow<List<UserAccount>>

    // Budgets
    @Query("SELECT * FROM daily_budgets WHERE dateString = :dateString AND ownerEmail = :ownerEmail LIMIT 1")
    suspend fun getBudgetForDate(dateString: String, ownerEmail: String): DailyBudget?

    @Query("SELECT * FROM daily_budgets WHERE ownerEmail = :ownerEmail")
    fun getAllBudgetsFlow(ownerEmail: String): Flow<List<DailyBudget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: DailyBudget)

    // Transactions
    @Query("SELECT * FROM transactions WHERE dateString = :dateString AND ownerEmail = :ownerEmail ORDER BY timestamp DESC")
    fun getTransactionsForDateFlow(dateString: String, ownerEmail: String): Flow<List<TransactionItem>>

    @Query("SELECT * FROM transactions WHERE ownerEmail = :ownerEmail ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(ownerEmail: String): Flow<List<TransactionItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionItem)

    @Update
    suspend fun updateTransaction(transaction: TransactionItem)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionItem)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Int)
}

@Database(entities = [UserAccount::class, DailyBudget::class, TransactionItem::class], version = 3, exportSchema = false)
abstract class SakuDatabase : RoomDatabase() {
    abstract fun sakuDao(): SakuDao
}
