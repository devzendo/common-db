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
        // Need to compare representation values as these are different types
        savedAccount.currentBalance.toRepresentation must equal(savedAccount.initialBalance.toRepresentation)

        // No transactions for this account
        val transactionsDao = userAccess.transactionsDao
        transactionsDao.findTransactionsForAccount(savedAccount).size must equal(0)
    }

    @Test
    def someAccountDetailsCanBeChanged() {
        database = createBeanMinderDatabase(databaseName)
        var userAccess = database.get.user.get
        val accountsDao = userAccess.accountsDao

        val newAccount = createTestAccount()
        val savedAccount = accountsDao.saveAccount(newAccount)
        savedAccount.id must not equal(newAccount.id)

        val renamedAccount = accountsDao.saveAccount(savedAccount copy (name = AccountName("My bank account")))
        renamedAccount.name must equal(AccountName("My bank account"))
        renamedAccount.id must equal(savedAccount.id)

        val recodedAccount = accountsDao.saveAccount(renamedAccount copy (accountCode = AccountCode("0101010")))
        recodedAccount.accountCode must equal(AccountCode("0101010"))
        recodedAccount.id must equal(renamedAccount.id)

        val movedAccount = accountsDao.saveAccount(recodedAccount copy (withBank = BankName("Another bank")))
        movedAccount.withBank must equal(BankName("Another bank"))
        movedAccount.id must equal(recodedAccount.id)

        // These don't change
        movedAccount.currentBalance must equal(CurrentBalance(5600))
        movedAccount.initialBalance must equal(InitialBalance(5600))
    }

    @Test
    def accountsCanBeListed() {
        database = createBeanMinderDatabase(databaseName)
        var userAccess = database.get.user.get
        val accountsDao = userAccess.accountsDao

        val accountOne = createTestAccount()
        val savedAccountOne = accountsDao.saveAccount(accountOne)
        val accountTwo = createSecondTestAccount()
        val savedAccountTwo = accountsDao.saveAccount(accountTwo)

        val allAccounts = accountsDao.findAllAccounts()
        allAccounts must have size (2)
        allAccounts(0) must not equal (allAccounts(1))
        // ordered by name
        allAccounts(0) must equal(savedAccountTwo) // Aardvark...
        allAccounts(1) must equal(savedAccountOne) // Test...
    }

    private def createSecondTestAccount() =
        Account(
            AccountName("Aardvark Test account"),
            BankName("Imaginary Bank of London"),
            AccountCode("867456"),
            InitialBalance(50))


}
