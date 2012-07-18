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

class JdbcTemplateTransactionsDao extends TransactionsDao {
    def findTransactionsForAccount(account: Account) = null // TODO

    def findTransactionsForAccountByIndexRange(account: Account, fromIndex: Int, toIndex: Int) = null // TODO

    def saveTransaction(account: Account, transaction: Transaction) = null // TODO

    def deleteTransaction(account: Account, transaction: Transaction) = null // TODO

    def getNumberOfTransactions(account: Account) = 0 // TODO
}
