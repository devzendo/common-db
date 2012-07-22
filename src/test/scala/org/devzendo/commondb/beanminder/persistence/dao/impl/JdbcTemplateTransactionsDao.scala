/**
 * Copyright (C) 2008-2012 Matt Gumbley, DevZendo.org <http://devzendo.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.devzendo.commondb.beanminder.persistence.dao.impl

import org.devzendo.commondb.beanminder.persistence.dao.TransactionsDao
import org.devzendo.commondb.beanminder.persistence.domain._
import org.springframework.jdbc.core.simple.{SimpleJdbcTemplate, ParameterizedRowMapper}
import java.sql.{Connection, SQLException, ResultSet}
import collection.JavaConverters._
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.jdbc.core.PreparedStatementCreator
import org.devzendo.commondb.beanminder.persistence.domain.Amount
import org.devzendo.commondb.beanminder.persistence.domain.AccountBalance
import org.devzendo.commondb.beanminder.persistence.domain.Index
import org.devzendo.commondb.beanminder.persistence.domain.CurrentBalance
import org.springframework.dao.DataIntegrityViolationException

class JdbcTemplateTransactionsDao(jdbcTemplate: SimpleJdbcTemplate) extends TransactionsDao {
    var accountsDao: JdbcTemplateAccountsDao = null

    def findTransactionsForAccount(account: Account): List[Transaction] = {
        accountsDao.ensureAccountSaved(account)
        val sql = "SELECT id, accountId, index, amount, isCredit, isReconciled, transactionDate, accountBalance " +
            "FROM Transactions WHERE accountId = ?" +
            "ORDER BY index ASC"
        jdbcTemplate.query(sql, createTransactionMapper(), account.id: java.lang.Integer).asScala.toList
    }

    def findTransactionsForAccountByIndexRange(account: Account, fromIndex: Int, toIndex: Int) = null // TODO

    def saveTransaction(account: Account, transaction: Transaction): (Account, Transaction) = {
        accountsDao.ensureAccountSaved(account)
        if (transaction.id == -1) {
            insertTransaction(account, transaction)
        } else {
            updateTransaction(account, transaction)
        }
    }

    def deleteTransaction(account: Account, transaction: Transaction): Account = {
        accountsDao.ensureAccountSaved(account)
        ensureTransactionSaved(transaction)
        // Always reload the account to get the correct balance.
        val reloadedAccount = accountsDao.loadAccount(account)
//        LOGGER.debug("the reloaded account is " + account);

        // Calculate a delta to apply to the account and all subsequent transactions
        val reloadedTransaction = loadTransaction(transaction)
//        LOGGER.debug("the reloaded transaction is " + reloadedTransaction);
        val transactionAmountSigned = getSignedAmount(reloadedTransaction)
//        LOGGER.debug("will subtract " + transactionAmountSigned + " from account and subsequent transactions");

        // Update the account with the new balance
        val newBalanceAccount = new Account(
            reloadedAccount.id, reloadedAccount.name, reloadedAccount.withBank, reloadedAccount.accountCode,
            reloadedAccount.initialBalance,
            CurrentBalance(reloadedAccount.currentBalance.toRepresentation - transactionAmountSigned))
        val updatedAccount = accountsDao.updateAccount(newBalanceAccount)
//        LOGGER.debug("The updated account is " + updatedAccount);

        // Update all subsequent transactions, accountBalance -= transactionAmountSigned, index--
        val numberOfTransactions = getNumberOfTransactions(reloadedAccount)
        deleteTransactionById(reloadedTransaction)
        for (index <- reloadedTransaction.index.toRepresentation + 1 until numberOfTransactions) {
            subtractDeltaFromTransactionAccountBalanceAndDecrementIndexByIndex(updatedAccount, index, transactionAmountSigned)
        }

        updatedAccount
    }

    def getNumberOfTransactions(account: Account) = {
        jdbcTemplate.queryForInt("SELECT COUNT(*) FROM Transactions WHERE accountId = ?", account.id: java.lang.Integer)
    }

    private def subtractDeltaFromTransactionAccountBalanceAndDecrementIndexByIndex(account: Account, transactionIndex: Int, deltaToSubtract: Int) {
        // TODO this seems overkill, and could be replaced by:
        // SELECT accountBalance FROM Transactions WHERE accountId = ? AND index = ?
        // UPDATE Transactions SET accountBalance = ?, index = ? WHERE accountId = ? AND id = ?
        val transaction = loadTransaction(account, transactionIndex)
//        LOGGER.debug("tx#" + transactionIndex + " to apply delta of " + deltaToSubtract + " to is " + transaction);
        val updatedTransaction = new Transaction(
            transaction.id,
            transaction.accountId,
            Index(transactionIndex - 1),
            transaction.amount,
            transaction.isCredit,
            transaction.isReconciled,
            transaction.transactionDate,
            AccountBalance(transaction.accountBalance.toRepresentation - deltaToSubtract))
//        LOGGER.debug("saved, that's: " + updatedTransaction);
        updateTransaction(updatedTransaction)
    }

    private def deleteTransactionById(transaction: Transaction) {
        jdbcTemplate.update("DELETE FROM Transactions WHERE id = ?", transaction.id: java.lang.Integer)
    }

    private def ensureTransactionSaved(transaction: Transaction) {
        if (transaction.id == -1) {
            throw new DataIntegrityViolationException("Cannot process an unsaved transaction")
        }
    }

    /**
     * For use by the DAO layer, reload the transaction given its data that may be old
     * @param transaction the transaction to reload
     * @return the loaded transaction
     */
    private def loadTransaction(transaction: Transaction): Transaction = {
        val sql = "SELECT id, accountId, index, amount, isCredit, isReconciled, transactionDate, accountBalance " +
            "FROM Transactions WHERE id = ?"
        jdbcTemplate.queryForObject(sql, createTransactionMapper(), transaction.id: java.lang.Integer)
    }

    /**
     * For use by the DAO layer, reload the transaction given its Account and index
     * @param account the Account for which the transaction should be loaded
     * @param index the index of the transaction within the account
     * @return the loaded transaction
     */
    private def loadTransaction(account: Account, index: Int): Transaction = {
        val sql = "SELECT id, accountId, index, amount, isCredit, isReconciled, transactionDate, accountBalance " +
            "FROM Transactions WHERE accountId = ? AND index = ?"
        jdbcTemplate.queryForObject(sql, createTransactionMapper(), account.id: java.lang.Integer, index: java.lang.Integer)
    }

    /**
     * Update a transaction with new data; does not change the account FK.
     * @param updatedTransaction the transaction to update
     * @return the updated transaction
     */
    private def updateTransaction(updatedTransaction: Transaction): Transaction = {
        jdbcTemplate.update(
            "UPDATE Transactions SET " +
                "index = ?, amount = ?, isCredit = ?, isReconciled = ?, transactionDate = ?, accountBalance = ? " +
                "WHERE id = ?",
            Array[Any](
                updatedTransaction.index.toRepresentation,
                updatedTransaction.amount.toRepresentation,
                updatedTransaction.isCredit == CreditDebit.Credit,
                updatedTransaction.isReconciled == Reconciled.Reconciled,
                updatedTransaction.transactionDate.toRepresentation,
                updatedTransaction.accountBalance.toRepresentation,
                updatedTransaction.id: java.lang.Integer))
        updatedTransaction
    }

    private def updateTransaction(account: Account, updatedTransaction: Transaction): (Account, Transaction) = {
        // Always reload the account to get the correct balance.
        val reloadedAccount = accountsDao.loadAccount(account)
//        LOGGER.debug("the reloaded account is " + account)

        // Calculate a delta to apply to the account and all subsequent transactions
        val committedTransaction = loadTransaction(updatedTransaction)
//        LOGGER.debug("the committed transaction is " + committedTransaction);
//        LOGGER.debug("the updated transaction is   " + updatedTransaction);
        val committedAmountSigned = getSignedAmount(committedTransaction)
        val updatedAmountSigned = getSignedAmount(updatedTransaction)
        val deltaToAdd = updatedAmountSigned - committedAmountSigned
//        LOGGER.debug("the delta to add (updated - committed) is " + deltaToAdd);

        // Update the account with the new balance
        val newBalanceAccount = new Account(
            reloadedAccount.id, reloadedAccount.name, reloadedAccount.withBank, reloadedAccount.accountCode,
            reloadedAccount.initialBalance,
            CurrentBalance(reloadedAccount.currentBalance.toRepresentation + deltaToAdd))
        val updatedAccount = accountsDao.updateAccount(newBalanceAccount)
//        LOGGER.debug("The updated account is " + updatedAccount);

        // Update this transaction, by creating a new version of it from the committed version, not changing
        // any id or index fields.
        val newUpdatedTransaction = new Transaction(
            committedTransaction.id,
            committedTransaction.accountId,
            committedTransaction.index,
            updatedTransaction.amount,
            updatedTransaction.isCredit,
            updatedTransaction.isReconciled,
            updatedTransaction.transactionDate,
            AccountBalance(committedTransaction.accountBalance.toRepresentation + deltaToAdd))
        val savedUpdatedTransaction = updateTransaction(newUpdatedTransaction)
//        LOGGER.debug("the saved updated transaction is " + savedUpdatedTransaction);

        // Update all subsequent transactions
        val numberOfTransactions = getNumberOfTransactions(reloadedAccount)
        for (index <- savedUpdatedTransaction.index.toRepresentation + 1 until numberOfTransactions) {
            addDeltaToTransactionAccountBalanceByIndex(updatedAccount, index, deltaToAdd)
        }

        (updatedAccount, savedUpdatedTransaction)
    }

    private def addDeltaToTransactionAccountBalanceByIndex(account: Account, index: Int, deltaToAdd: Int) {
        // TODO this seems overkill, and could be replaced by:
        // SELECT accountBalance FROM Transactions WHERE accountId = ? AND index = ?
        // UPDATE Transactions SET accountBalance = ? WHERE accountId = ? AND index = ?
        val transaction = loadTransaction(account, index)
//        LOGGER.debug("tx#" + index + " to apply delta of " + deltaToAdd + " to is " + transaction);
        val updatedTransaction = new Transaction(
            transaction.id,
            transaction.accountId,
            transaction.index,
            transaction.amount,
            transaction.isCredit,
            transaction.isReconciled,
            transaction.transactionDate,
            AccountBalance(transaction.accountBalance.toRepresentation + deltaToAdd))
//        LOGGER.debug("saved, that's: " + updatedTransaction);
        updateTransaction(updatedTransaction)
    }

    private def insertTransaction(account: Account, transaction: Transaction): (Account, Transaction) = {
        // Always reload the account to get the correct balance.
        val reloadedAccount = accountsDao.loadAccount(account)
        // Save the transaction
        val keyHolder = new GeneratedKeyHolder()
        val transactionIndex = getNumberOfTransactions(reloadedAccount)
        val newBalance = reloadedAccount.currentBalance.toRepresentation + getSignedAmount(transaction)
        jdbcTemplate.getJdbcOperations.update(new PreparedStatementCreator() {
            @throws(classOf[SQLException])
            def createPreparedStatement(conn: Connection) = {
                val sql = "INSERT INTO Transactions " +
                    "(accountId, index, amount, isCredit, isReconciled, " +
                    "transactionDate, accountBalance) VALUES (?, ?, ?, ?, ?, ?, ?)"
                val ps = conn.prepareStatement(sql, Array[String]("id"))
                ps.setInt(1, reloadedAccount.id)
                ps.setInt(2, transactionIndex)
                ps.setInt(3, transaction.amount.toRepresentation)
                ps.setBoolean(4, transaction.isCredit == CreditDebit.Credit)
                ps.setBoolean(5, transaction.isReconciled == Reconciled.Reconciled)
                ps.setDate(6, transaction.transactionDate.toRepresentation)
                ps.setInt(7, newBalance)
                ps
            }
        }, keyHolder)
        val key = keyHolder.getKey.intValue()
        val savedTransaction = new Transaction(key, reloadedAccount.id,
            Index(transactionIndex), transaction.amount, transaction.isCredit,
            transaction.isReconciled, transaction.transactionDate,
            AccountBalance(newBalance))
        // Update the account balance
        val newBalanceAccount = new Account(
            reloadedAccount.id, reloadedAccount.name, reloadedAccount.withBank,
            reloadedAccount.accountCode, reloadedAccount.initialBalance, CurrentBalance(newBalance))
        val updatedAccount = accountsDao.updateAccount(newBalanceAccount)
        (updatedAccount, savedTransaction)
    }

    private def getSignedAmount(transaction: Transaction) = {
        if (transaction.isCredit == CreditDebit.Credit)
            transaction.amount.toRepresentation
        else
            (-1 * transaction.amount.toRepresentation)
    }

    private def createTransactionMapper() = new ParameterizedRowMapper[Transaction]() {
        @throws(classOf[SQLException])
        def mapRow(rs: ResultSet, rowNum: Int) = new Transaction(
            rs.getInt("id"),
            rs.getInt("accountId"),
            Index(rs.getInt("index")),
            Amount(rs.getInt("amount")),
            if (rs.getBoolean("isCredit")) CreditDebit.Credit else CreditDebit.Debit,
            if (rs.getBoolean("isReconciled")) Reconciled.Reconciled else Reconciled.NotReconciled,
            rs.getDate("transactionDate"),
            AccountBalance(rs.getInt("accountBalance"))
        )
    }
}
