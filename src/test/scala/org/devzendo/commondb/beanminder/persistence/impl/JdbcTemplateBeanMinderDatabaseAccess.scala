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

package org.devzendo.commondb.beanminder.persistence.impl

import org.devzendo.commondb.beanminder.persistence.BeanMinderDatabaseAccess
import org.devzendo.commondb.beanminder.persistence.dao.{TransactionsDao, AccountsDao}
import org.devzendo.commondb.beanminder.persistence.dao.impl.{JdbcTemplateTransactionsDao, JdbcTemplateAccountsDao}
import org.devzendo.commondb.{UserDatabaseAccess, DatabaseAccess}

class JdbcTemplateBeanMinderDatabaseAccess(override val databaseAccess: DatabaseAccess[BeanMinderDatabaseAccess]) extends UserDatabaseAccess(databaseAccess) with BeanMinderDatabaseAccess {
    private val _accountsDao = new JdbcTemplateAccountsDao(databaseAccess.jdbcTemplate)
    private val _transactionsDao = new JdbcTemplateTransactionsDao(databaseAccess.jdbcTemplate)
    // wire up dependencies between the DAOs
    _transactionsDao.accountsDao = _accountsDao

    def accountsDao: AccountsDao = _accountsDao

    def transactionsDao: TransactionsDao = _transactionsDao

    def close() {

    }
}
