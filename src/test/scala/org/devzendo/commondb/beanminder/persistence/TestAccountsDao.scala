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
import domain.AccountCode
import domain.AccountName
import domain.BankName
import org.scalatest.junit.{MustMatchersForJUnit, AssertionsForJUnit}
import org.junit.Test

class TestAccountsDao extends BeanMinderUnittest with AssertionsForJUnit with MustMatchersForJUnit {
    val databaseName = "testaccountsdao"

    @Test
    def anEmptyAccountCanBeCreated() {
        database = createBeanMinderDatabase(databaseName)
        var userAccess = database.get.user.get
        val accountsDao = userAccess.accountsDao

        val newAccount = createTestAccount()
        val savedAccount = accountsDao.saveAccount(newAccount)

        savedAccount.id must be > (0)
        savedAccount.accountCode must equal(newAccount.accountCode)
        savedAccount.name must equal(newAccount.name)
        savedAccount.withBank must equal(newAccount.withBank)
        savedAccount.currentBalance must equal(newAccount.currentBalance)
        savedAccount.initialBalance must equal(newAccount.initialBalance)

        // The current balance is initialised to the initial balance
        savedAccount.currentBalance must equal(savedAccount.initialBalance)

        // No transactions for this account
        // TODO add this back in when the transactionsDao is written
//        val transactionsDao = userAccess.transactionsDao
//        transactionsDao.findTransactionsForAccount(savedAccount).size must equal(0)
    }

    def createTestAccount() =
        Account(
            AccountName("Test account"),
            BankName("Imaginary Bank of London"),
            AccountCode("123456"),
            InitialBalance(5600))
}
