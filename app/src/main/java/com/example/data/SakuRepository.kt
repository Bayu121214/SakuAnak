package com.example.data

import kotlinx.coroutines.flow.Flow

class SakuRepository(private val sakuDao: SakuDao) {

    fun getAllBudgets(ownerEmail: String): Flow<List<DailyBudget>> {
        return sakuDao.getAllBudgetsFlow(ownerEmail)
    }

    fun getAllTransactions(ownerEmail: String): Flow<List<TransactionItem>> {
        return sakuDao.getAllTransactionsFlow(ownerEmail)
    }

    suspend fun getBudgetForDate(dateString: String, ownerEmail: String): DailyBudget? {
        return sakuDao.getBudgetForDate(dateString, ownerEmail)
    }

    fun getTransactionsForDate(dateString: String, ownerEmail: String): Flow<List<TransactionItem>> {
        return sakuDao.getTransactionsForDateFlow(dateString, ownerEmail)
    }

    suspend fun saveBudget(budget: DailyBudget) {
        sakuDao.insertBudget(budget)
    }

    suspend fun insertTransaction(transaction: TransactionItem) {
        sakuDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionItem) {
        sakuDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionItem) {
        sakuDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Int) {
        sakuDao.deleteTransactionById(id)
    }

    // User management
    suspend fun insertUser(user: UserAccount) {
        sakuDao.insertUser(user)
    }

    suspend fun getUserByEmail(email: String): UserAccount? {
        return sakuDao.getUserByEmail(email)
    }

    fun getUserByEmailFlow(email: String): Flow<UserAccount?> {
        return sakuDao.getUserByEmailFlow(email)
    }

    fun getAllUsers(): Flow<List<UserAccount>> {
        return sakuDao.getAllUsersFlow()
    }
}
