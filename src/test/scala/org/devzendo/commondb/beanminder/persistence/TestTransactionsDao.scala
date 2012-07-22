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

package org.devzendo.commondb.beanminder.persistence

import domain._
import domain.Amount
import org.scalatest.junit.{MustMatchersForJUnit, AssertionsForJUnit}
import org.junit.Test
import org.springframework.dao.DataAccessException

class TestTransactionsDao extends BeanMinderUnittest with AssertionsForJUnit with MustMatchersForJUnit {
    val databaseName = "testtransactionsdao"

    @Test
    def cannotCommitTransactionAgainstUnsavedAccount() {
        database = createBeanMinderDatabase(databaseName)
        val userAccess = database.get.user.get
        val newAccount = createTestAccount()
        // note: unsaved Account
        val transactionsDao = userAccess.transactionsDao

        evaluating {
            transactionsDao.findTransactionsForAccount(newAccount)

        } must produce [DataAccessException]
    }

    @Test
    def transactionCanBeAddedToAccount() {
        database = createBeanMinderDatabase(databaseName)
        val userAccess = database.get.user.get
        val newAccount = createTestAccount()
        val accountsDao = userAccess.accountsDao
        val transactionsDao = userAccess.transactionsDao
        val savedAccount = accountsDao.saveAccount(newAccount)
        val today = todayNormalised()
        val newTransaction = Transaction(Amount(200), CreditDebit.Credit, Reconciled.NotReconciled, today)

        val (updatedAccount, savedTransaction) = transactionsDao.saveTransaction(savedAccount, newTransaction)

        savedTransaction.id must be > (0)
        savedTransaction.accountId must equal(updatedAccount.id)
        savedTransaction.amount must equal (Amount(200))
        savedTransaction.isCredit must equal (CreditDebit.Credit)
        savedTransaction.isReconciled must equal (Reconciled.NotReconciled)
        savedTransaction.transactionDate must equal (today)

        val transactions = transactionsDao.findTransactionsForAccount(updatedAccount)
        transactions must have size (1)
        transactions(0) must equal (savedTransaction)
    }

    @Test
    def addCreditTransactionToAccountIncreasesBalance() {
        database = createBeanMinderDatabase(databaseName)
        val userAccess = database.get.user.get
        val newAccount = createTestAccount()
        val accountsDao = userAccess.accountsDao
        val transactionsDao = userAccess.transactionsDao
        val savedAccount = accountsDao.saveAccount(newAccount)
        val today = todayNormalised()
        val newTransaction = Transaction(Amount(200), CreditDebit.Credit, Reconciled.NotReconciled, today)

        val (updatedAccount, _) = transactionsDao.saveTransaction(savedAccount, newTransaction)

        updatedAccount.currentBalance must equal (CurrentBalance(5800))
        // initial balance should not change
        updatedAccount.initialBalance must equal (InitialBalance(5600))
    }

    @Test
    def addDebitTransactionToAccountDecreasesBalance() {
        database = createBeanMinderDatabase(databaseName)
        val userAccess = database.get.user.get
        val newAccount = createTestAccount()
        val accountsDao = userAccess.accountsDao
        val transactionsDao = userAccess.transactionsDao
        val savedAccount = accountsDao.saveAccount(newAccount)
        val today = todayNormalised()

        val newTransaction = Transaction(Amount(200), CreditDebit.Debit, Reconciled.NotReconciled, today)

        val (updatedAccount, _) = transactionsDao.saveTransaction(savedAccount, newTransaction)
        updatedAccount.currentBalance must equal (CurrentBalance(5400))
        // initial balance should not change
        updatedAccount.initialBalance must equal (InitialBalance(5600))
    }

    @Test
    def numberOfTransactionsIsCorrectAfterSavingTransactions() {
        database = createBeanMinderDatabase(databaseName)
        val userAccess = database.get.user.get
        val newAccount = createTestAccount()
        val accountsDao = userAccess.accountsDao
        val transactionsDao = userAccess.transactionsDao
        val savedAccount = accountsDao.saveAccount(newAccount)
        val today = todayNormalised()
        transactionsDao.getNumberOfTransactions(savedAccount) must equal(0)
        transactionsDao.saveTransaction(savedAccount, Transaction(Amount(200), CreditDebit.Credit, Reconciled.NotReconciled, today))
        transactionsDao.getNumberOfTransactions(savedAccount) must equal(1)
        transactionsDao.saveTransaction(savedAccount, Transaction(Amount(400), CreditDebit.Credit, Reconciled.NotReconciled, today))
        transactionsDao.getNumberOfTransactions(savedAccount) must equal(2)
    }
}
