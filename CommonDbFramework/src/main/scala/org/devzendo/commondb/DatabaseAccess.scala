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

package org.devzendo.commondb

import dao.{SequenceDao, VersionsDao}
import java.io.File
import javax.sql.DataSource
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate
import org.springframework.transaction.support.TransactionTemplate

/**
 * The DatabaseAccess gives access to the framework/user database abstractions,
 * provides a mechanism in which operations can be performed inside a
 * transaction, and finally allows the database to be closed.
 *
 * These are obtained from the DatabaseAccessFactory, and are not directly
 * instantiatable from user code.
 *
 * @param databasePath the directory in which the database's file set are to be
 *                     stored.
 * @param databaseName the common prefix name used by the database's file set.
 * @param dataSource the JDBC DataSource that can be used to perform low-level
 *                   operations on the database.
 * @param jdbcTemplate the Spring-JDBC template that is used to perform JDBC
 *                     operations.
 * @tparam U a subclass of UserDatabaseAccess, from which user DAOs may be
 *           obtained
 */
abstract class DatabaseAccess[U <: UserDatabaseAccess](
       val databasePath: File,
       val databaseName: String,
       val dataSource: DataSource,
       val jdbcTemplate: SimpleJdbcTemplate) {
    var user: Option[U] = None
    def close()
    def isClosed: Boolean
    def versionsDao: VersionsDao
    def sequenceDao: SequenceDao
    def createTransactionTemplate: TransactionTemplate
}
