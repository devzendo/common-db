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

import org.devzendo.commondb.beanminder.persistence.dao.AccountsDao
import org.devzendo.commondb.beanminder.persistence.domain.Account
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.jdbc.core.PreparedStatementCreator
import java.sql.{PreparedStatement, SQLException, Connection}

class JdbcTemplateAccountsDao(jdbcTemplate: SimpleJdbcTemplate) extends AccountsDao {
    def findAllAccounts() = null // TODO


    def saveAccount(account: Account): Account = {
        if (account.id != -1) {
            updateAccount(account)
        } else {
            insertAccount(account)
        }
    }

    def deleteAccount(account: Account) {} // TODO

    private[this] def updateAccount(account: Account): Account = { // TODO
        null
    }

    private[this] def insertAccount(account: Account): Account = {
        val keyHolder = new GeneratedKeyHolder()
        jdbcTemplate.getJdbcOperations.update(new PreparedStatementCreator() {
            @throws(classOf[SQLException])
            def createPreparedStatement(conn: Connection): PreparedStatement = {
                val sql = "INSERT INTO Accounts " +
                    "(name, with, accountCode, initialBalance, currentBalance) " +
                    "VALUES (?, ?, ?, ?, ?)"
                val ps = conn.prepareStatement(sql, Array[String]("id"))
                ps.setString(1, account.name)
                ps.setString(2, account.withBank)
                ps.setString(3, account.accountCode)
                ps.setInt(4, account.initialBalance)
                ps.setInt(5, account.currentBalance)
                ps
            }
        }, keyHolder)
        val key = keyHolder.getKey.intValue()
        new Account(key, account.name, account.withBank, account.accountCode,
            account.initialBalance, account.currentBalance)
    }
}
