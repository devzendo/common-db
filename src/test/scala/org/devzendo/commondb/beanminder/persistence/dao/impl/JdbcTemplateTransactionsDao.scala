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
import org.devzendo.commondb.beanminder.persistence.domain.{Transaction, Account}
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.simple.{SimpleJdbcTemplate, ParameterizedRowMapper}
import java.sql.{SQLException, ResultSet}
import collection.JavaConverters._

class JdbcTemplateTransactionsDao(jdbcTemplate: SimpleJdbcTemplate) extends TransactionsDao {
    def findTransactionsForAccount(account: Account): List[Transaction] = {
        ensureAccountSaved(account)
        val sql = "SELECT id, accountId, index, amount, isCredit, isReconciled, transactionDate, accountBalance " +
            "FROM Transactions WHERE accountId = ?" +
            "ORDER BY index ASC"
        jdbcTemplate.query(sql, createTransactionMapper(), account.id: java.lang.Integer).asScala.toList
    }

    private def ensureAccountSaved(account: Account) {
        if (account.id == -1) {
            throw new DataIntegrityViolationException(
                "Cannot store a transaction against an unsaved account")
        }
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


    def findTransactionsForAccountByIndexRange(account: Account, fromIndex: Int, toIndex: Int) = null // TODO

    def saveTransaction(account: Account, transaction: Transaction) = null // TODO

    def deleteTransaction(account: Account, transaction: Transaction) = null // TODO

    def getNumberOfTransactions(account: Account) = 0 // TODO
}
