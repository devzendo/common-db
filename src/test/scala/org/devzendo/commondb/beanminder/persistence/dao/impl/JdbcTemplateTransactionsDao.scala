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
import org.devzendo.commondb.beanminder.persistence.domain.{CurrentBalance, Transaction, Account}
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.simple.{SimpleJdbcTemplate, ParameterizedRowMapper}
import java.sql.{Connection, SQLException, ResultSet}
import collection.JavaConverters._
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.jdbc.core.PreparedStatementCreator

class JdbcTemplateTransactionsDao(jdbcTemplate: SimpleJdbcTemplate) extends TransactionsDao {
    var accountsDao: JdbcTemplateAccountsDao = null

    def findTransactionsForAccount(account: Account): List[Transaction] = {
        ensureAccountSaved(account)
        val sql = "SELECT id, accountId, index, amount, isCredit, isReconciled, transactionDate, accountBalance " +
            "FROM Transactions WHERE accountId = ?" +
            "ORDER BY index ASC"
        jdbcTemplate.query(sql, createTransactionMapper(), account.id: java.lang.Integer).asScala.toList
    }

    def findTransactionsForAccountByIndexRange(account: Account, fromIndex: Int, toIndex: Int) = null // TODO

    def saveTransaction(account: Account, transaction: Transaction): (Account, Transaction) = {
        ensureAccountSaved(account)
        //        if (transaction.id == -1) {
        insertTransaction(account, transaction)
        //        } /*else {
        //            return updateTransaction(account, transaction)
        //        }   */

    }

    def deleteTransaction(account: Account, transaction: Transaction) = null // TODO

    def getNumberOfTransactions(account: Account) = 0 // TODO


    private def ensureAccountSaved(account: Account) {
        if (account.id == -1) {
            throw new DataIntegrityViolationException(
                "Cannot store a transaction against an unsaved account")
        }
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
                ps.setInt(3, transaction.amount)
                ps.setBoolean(4, transaction.isCredit)
                ps.setBoolean(5, transaction.isReconciled)
                ps.setDate(6, transaction.transactionDate.toRepresentation)
                ps.setInt(7, newBalance)
                ps
            }
        }, keyHolder)
        val key = keyHolder.getKey.intValue()
        val savedTransaction = new Transaction(key, reloadedAccount.id,
            transactionIndex, transaction.amount, transaction.isCredit,
            transaction.isReconciled, transaction.transactionDate,
            newBalance)
        // Update the account balance
        val newBalanceAccount = new Account(
            reloadedAccount.id, reloadedAccount.name, reloadedAccount.withBank,
            reloadedAccount.accountCode, reloadedAccount.initialBalance, CurrentBalance(newBalance))
        val updatedAccount = accountsDao.updateAccount(newBalanceAccount)
        (updatedAccount, savedTransaction)
    }

    private def getSignedAmount(transaction: Transaction) = {
        if (transaction.isCredit)
            transaction.amount
        else
            (-1 * transaction.amount)
    }

    private def createTransactionMapper() = new ParameterizedRowMapper[Transaction]() {
        @throws(classOf[SQLException])
        def mapRow(rs: ResultSet, rowNum: Int) = new Transaction(
            rs.getInt("id"),
            rs.getInt("accountId"),
            rs.getInt("index"),
            rs.getInt("amount"),
            rs.getBoolean("isCredit"),
            rs.getBoolean("isReconciled"),
            rs.getDate("transactionDate"),
            rs.getInt("accountBalance")
        )
    }
}
