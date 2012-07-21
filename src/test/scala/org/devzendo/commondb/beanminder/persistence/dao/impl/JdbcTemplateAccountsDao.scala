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

import collection.JavaConverters._
import org.devzendo.commondb.beanminder.persistence.dao.AccountsDao
import org.devzendo.commondb.beanminder.persistence.domain._
import org.springframework.jdbc.core.simple.{ParameterizedRowMapper, SimpleJdbcTemplate}
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.jdbc.core.PreparedStatementCreator
import java.sql.{ResultSet, PreparedStatement, SQLException, Connection}
import org.devzendo.commondb.beanminder.persistence.domain.AccountName
import org.devzendo.commondb.beanminder.persistence.domain.AccountCode
import org.devzendo.commondb.beanminder.persistence.domain.BankName

class JdbcTemplateAccountsDao(jdbcTemplate: SimpleJdbcTemplate) extends AccountsDao {
    def findAllAccounts(): List[Account] = {
        val sql = "SELECT id, name, with, accountCode, initialBalance, currentBalance FROM Accounts ORDER BY name"
        jdbcTemplate.query(sql, createAccountMapper()).asScala.toList
    }

    def saveAccount(account: Account): Account = {
        if (account.id != -1) {
            updateAccount(account)
        } else {
            insertAccount(account)
        }
    }

    def deleteAccount(account: Account) {
        // TODO
    }


    /**
     * For use by the DAO layer, load an account given its data which may be old.
     * @param account the account to reload
     * @return the reloaded Account
     */
    private[impl] def loadAccount(account: Account): Account = {
        val sql = "SELECT id, name, with, accountCode, initialBalance, currentBalance FROM Accounts WHERE id = ?"
        jdbcTemplate.queryForObject(sql, createAccountMapper(), account.id: java.lang.Integer)
    }

    private[impl] def updateAccount(account: Account): Account = {
        jdbcTemplate.update(
            "UPDATE Accounts SET name = ?, with = ?, accountCode = ?, currentBalance = ? WHERE id = ?",
            Array[Any](account.name.toRepresentation, account.withBank.toRepresentation,
                account.accountCode.toRepresentation,
                account.currentBalance.toRepresentation, account.id))
        account
    }

    private def insertAccount(account: Account): Account = {
        val keyHolder = new GeneratedKeyHolder()
        jdbcTemplate.getJdbcOperations.update(new PreparedStatementCreator() {
            @throws(classOf[SQLException])
            def createPreparedStatement(conn: Connection): PreparedStatement = {
                val sql = "INSERT INTO Accounts " +
                    "(name, with, accountCode, initialBalance, currentBalance) " +
                    "VALUES (?, ?, ?, ?, ?)"
                val ps = conn.prepareStatement(sql, Array[String]("id"))
                ps.setString(1, account.name.toRepresentation)
                ps.setString(2, account.withBank.toRepresentation)
                ps.setString(3, account.accountCode.toRepresentation)
                ps.setInt(4, account.initialBalance.toRepresentation)
                ps.setInt(5, account.currentBalance.toRepresentation)
                ps
            }
        }, keyHolder)
        val key = keyHolder.getKey.intValue()
        new Account(key, account.name, account.withBank, account.accountCode,
            account.initialBalance, account.currentBalance)
    }

    private def createAccountMapper() = new ParameterizedRowMapper[Account]() {
        def mapRow(rs: ResultSet, rowNum: Int) = new Account(
            rs.getInt("id"),
            AccountName(rs.getString("name")),
            BankName(rs.getString("with")),
            AccountCode(rs.getString("accountCode")),
            InitialBalance(rs.getInt("initialBalance")),
            CurrentBalance(rs.getInt("currentBalance")))
    }
}
