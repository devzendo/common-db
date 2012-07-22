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

    @Test
    def transactionsHaveMonotonicallyIncreasingIndexAndCorrectAccountBalance() {
        database = createBeanMinderDatabase(databaseName)
        val userAccess = database.get.user.get
        val newAccount = createTestAccount() // balance 5600
        val accountsDao = userAccess.accountsDao
        val transactionsDao = userAccess.transactionsDao
        val savedAccount = accountsDao.saveAccount(newAccount)
        val today = todayNormalised()

        // Transaction 1
        val newTransaction1 = Transaction(Amount(200), CreditDebit.Credit, Reconciled.NotReconciled, today) // +200 = 5800
        newTransaction1.index must equal(Index(-1)) // HMMM internal detail?
        newTransaction1.accountBalance must equal(AccountBalance(-1)) // HMMM internal detail?

        val (savedAccount1, savedTransaction1) = transactionsDao.saveTransaction(savedAccount, newTransaction1)
        savedTransaction1.index must equal(Index(0))
        savedTransaction1.accountBalance must equal(AccountBalance(5800)) // TODO use the same representation type for a transaction's view of the account balance?
        savedAccount1.currentBalance must equal(CurrentBalance(5800))

        // Transaction 2
        val newTransaction2 = Transaction(Amount(20), CreditDebit.Credit, Reconciled.NotReconciled, today) // +20 = 5820
        val (savedAccount2, savedTransaction2) = transactionsDao.saveTransaction(savedAccount1, newTransaction2)
        savedTransaction2.index must equal(Index(1))
        savedTransaction2.accountBalance must equal(AccountBalance(5820))
        savedAccount2.currentBalance must equal(CurrentBalance(5820))

        // Transaction 3
        val newTransaction3 = Transaction(Amount(10), CreditDebit.Debit, Reconciled.NotReconciled, today) // -10 = 5810
        val (savedAccount3, savedTransaction3) = transactionsDao.saveTransaction(savedAccount2, newTransaction3)
        savedTransaction3.index must equal(Index(2))
        savedTransaction3.accountBalance must equal(AccountBalance(5810))
        savedAccount3.currentBalance must equal(CurrentBalance(5810))
    }

    @Test
    def transactionsAreListedOrderedByIndex() {
        // if the select is not ordered by index, this insertion seems to yield
        // a list in the order inserted here, but it needs the ORDER BY index
        // ASC for correctness, so I'll test for it anyway
        database = createBeanMinderDatabase(databaseName)
        val userAccess = database.get.user.get
        val newAccount = createTestAccount()
        val accountsDao = userAccess.accountsDao
        val transactionsDao = userAccess.transactionsDao
        val savedAccount = accountsDao.saveAccount(newAccount)
        val today = todayNormalised()
        // Transaction 0
        transactionsDao.saveTransaction(savedAccount, Transaction(Amount(200), CreditDebit.Credit, Reconciled.NotReconciled, today))

        // Transaction 1
        transactionsDao.saveTransaction(savedAccount, Transaction(Amount(20), CreditDebit.Credit, Reconciled.NotReconciled, today))

        // Transaction 2
        transactionsDao.saveTransaction(savedAccount, Transaction(Amount(10), CreditDebit.Debit, Reconciled.NotReconciled, today))

        val allTransactions = transactionsDao.findTransactionsForAccount(savedAccount)
        allTransactions must have size (3)
        allTransactions(0).amount must equal(Amount(200))
        allTransactions(0).index must equal(Index(0))
        allTransactions(1).amount must equal(Amount(20))
        allTransactions(1).index must equal(Index(1))
        allTransactions(2).amount must equal(Amount(10))
        allTransactions(2).index must equal(Index(2))
    }

    @Test
    def updateATransactionAndSubsequentTransactionsAndAccountBalanceUpdated() {
        database = createBeanMinderDatabase(databaseName)
        val userAccess = database.get.user.get
        val newAccount = createTestAccount()
        val accountsDao = userAccess.accountsDao
        val transactionsDao = userAccess.transactionsDao
        val savedAccount = accountsDao.saveAccount(newAccount)
        val today = todayNormalised()

        // Transaction 0
        val newTransaction0 = Transaction(Amount(200), CreditDebit.Credit, Reconciled.NotReconciled, today)
        val (savedAccount0, savedTransaction0) = transactionsDao.saveTransaction(savedAccount, newTransaction0)

        // Transaction 1
        val newTransaction1 = Transaction(Amount(20), CreditDebit.Credit, Reconciled.NotReconciled, today)
        val (savedAccount1, savedTransaction1) = transactionsDao.saveTransaction(savedAccount0, newTransaction1)

        // Transaction 2
        val newTransaction2 = Transaction(Amount(10), CreditDebit.Debit, Reconciled.NotReconciled, today)
        val (savedAccount2, savedTransaction2) = transactionsDao.saveTransaction(savedAccount1, newTransaction2)

        // Transaction 3
        val newTransaction3 = Transaction(Amount(500), CreditDebit.Credit, Reconciled.NotReconciled, today)
        val (accountBeforeUpdate, savedTransaction3) = transactionsDao.saveTransaction(savedAccount2, newTransaction3)
        accountBeforeUpdate.currentBalance must be (CurrentBalance(5600 + 200 + 20 - 10 + 500))

        // Update Transaction 1
        val alteredTransaction1 = savedTransaction1 copy (amount = Amount(30)) // +10
        val (reloadedAccount, savedUpdatedTransaction1) = transactionsDao.saveTransaction(accountBeforeUpdate, alteredTransaction1)

        // Have the account, the updated transaction, and subsequent transactions been modified?
        reloadedAccount.currentBalance must be (CurrentBalance(5600 + 200 + 30 - 10 + 500))

        val allTransactions = transactionsDao.findTransactionsForAccount(savedAccount)
        allTransactions must have size (4)

        allTransactions(0).amount must equal(Amount(200))
        allTransactions(0).index must equal(Index(0))
        allTransactions(0).accountBalance must equal(AccountBalance(5800))

        allTransactions(1).amount must equal(Amount(30))
        allTransactions(1).index must equal(Index(1))
        allTransactions(1).accountBalance must equal(AccountBalance(5830))

        allTransactions(2).amount must equal(Amount(10))
        allTransactions(2).index must equal(Index(2))
        allTransactions(2).accountBalance must equal(AccountBalance(5820))

        allTransactions(3).amount must equal(Amount(500))
        allTransactions(3).index must equal(Index(3))
        allTransactions(3).accountBalance must equal(AccountBalance(6320))
    }


    @Test
    def updateATransactionByAlsoChangingCreditDebitFlag() {
        database = createBeanMinderDatabase(databaseName)
        val userAccess = database.get.user.get
        val newAccount = createTestAccount()
        val accountsDao = userAccess.accountsDao
        val transactionsDao = userAccess.transactionsDao
        val savedAccount = accountsDao.saveAccount(newAccount)
        val today = todayNormalised()

        // Transaction 0
        val newTransaction0 = Transaction(Amount(200), CreditDebit.Credit, Reconciled.NotReconciled, today)
        transactionsDao.saveTransaction(savedAccount, newTransaction0)

        // Transaction 1, the last one in the a/c.
        val newTransaction1 = Transaction(Amount(20), CreditDebit.Credit, Reconciled.NotReconciled, today)
        val (accountBeforeUpdate, savedTransaction1) = transactionsDao.saveTransaction(savedAccount, newTransaction1)
        accountBeforeUpdate.currentBalance.toRepresentation must equal (5600 + 200 + 20)

        // Update Transaction 1. +10, but change to a debit, so effect on the a/c is -50
        val updatedSavedTransaction1 = savedTransaction1 copy (amount = Amount(30), isCredit = CreditDebit.Debit)
        val (reloadedAccount, _) = transactionsDao.saveTransaction(savedAccount, updatedSavedTransaction1)

        // Have the account, the updated transaction, and subsequent transactions been modified?
        reloadedAccount.currentBalance.toRepresentation must equal (5600 + 200 - 30)

        val allTransactions = transactionsDao.findTransactionsForAccount(savedAccount)
        allTransactions must have size (2)

        allTransactions(0).amount.toRepresentation must equal(200)
        allTransactions(0).index.toRepresentation must equal(0)
        allTransactions(0).accountBalance.toRepresentation must equal(5800)

        allTransactions(1).amount.toRepresentation must equal(30)
        allTransactions(1).isCredit must equal(CreditDebit.Debit)
        allTransactions(1).index.toRepresentation must equal(1)
        allTransactions(1).accountBalance.toRepresentation must equal(5600 + 200 - 30)
    }
}
