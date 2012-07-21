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

import domain.Transaction
import org.scalatest.junit.{MustMatchersForJUnit, AssertionsForJUnit}
import org.junit.Test
import org.springframework.dao.DataAccessException

class TestTransactionsDao extends BeanMinderUnittest with AssertionsForJUnit with MustMatchersForJUnit {
    val databaseName = "testtransactionsdao"

    @Test
    def cannotCommitTransactionAgainstUnsavedAccount() {
        database = createBeanMinderDatabase(databaseName)
        var userAccess = database.get.user.get
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
        var userAccess = database.get.user.get
        val newAccount = createTestAccount()
        val accountsDao = userAccess.accountsDao
        val transactionsDao = userAccess.transactionsDao
        val savedAccount = accountsDao.saveAccount(newAccount)
        val today = todayNormalised()
        val newTransaction = Transaction(200, isCredit = true, isReconciled = false, transactionDate = today)

        val (updatedAccount, savedTransaction) = transactionsDao.saveTransaction(savedAccount, newTransaction)

        savedTransaction.id must be > (0)
        savedTransaction.accountId must equal(updatedAccount.id)
        savedTransaction.amount must equal (200)
        savedTransaction.isCredit must equal (true)
        savedTransaction.isReconciled must equal (false)
        savedTransaction.origTransactionDate must equal (today)

        val transactions = transactionsDao.findTransactionsForAccount(updatedAccount)
        transactions must have size (1)
        transactions(0) must equal (savedTransaction)
    }
}
