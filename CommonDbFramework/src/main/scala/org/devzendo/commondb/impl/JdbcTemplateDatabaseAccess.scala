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

package org.devzendo.commondb.impl

import org.devzendo.commondb.{DatabaseAccessFactory, DatabaseAccess, UserDatabaseAccess}
import java.io.File
import javax.sql.DataSource
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate
import org.springframework.jdbc.datasource.{DataSourceUtils, DataSourceTransactionManager}
import org.devzendo.commondb.dao.{SequenceDao, VersionsDao}
import org.devzendo.commondb.dao.impl.{JdbcTemplateSequenceDao, JdbcTemplateVersionsDao}
import org.springframework.jdbc.CannotGetJdbcConnectionException
import java.sql.SQLException
import org.springframework.transaction.support.TransactionTemplate

sealed case class JdbcTemplateDatabaseAccess[U <: UserDatabaseAccess](
        override val databasePath: File,
        override val databaseName: String,
        override val dataSource: DataSource,
        override val jdbcTemplate: SimpleJdbcTemplate) extends DatabaseAccess[U](databasePath, databaseName, dataSource, jdbcTemplate) {
    private[this] var closed: Boolean = false
    private[this] val transactionManager = new DataSourceTransactionManager(dataSource)

    val versionsDao: VersionsDao = new JdbcTemplateVersionsDao(jdbcTemplate)
    val sequenceDao: SequenceDao = new JdbcTemplateSequenceDao(jdbcTemplate)

    def close() {
        if (closed) {
            DatabaseAccessFactory.LOGGER.info("Database '" + databaseName + "' at '" + databasePath + "' is already closed")
            return
        }

        try {
            DatabaseAccessFactory.LOGGER.info("Closing database '" + databaseName + "' at '" + databasePath + "'")
            for (u <- user) {
                u.close()
            }
            DataSourceUtils.getConnection(dataSource).close()
            DatabaseAccessFactory.LOGGER.info("Closed database '" + databaseName + "' at '" + databasePath + "'")
            closed = true
        } catch {
            case e: CannotGetJdbcConnectionException =>
                DatabaseAccessFactory.LOGGER.warn("Can't get JDBC Connection on close: " + e.getMessage, e)
            case s: SQLException =>
                DatabaseAccessFactory.LOGGER.warn("SQL Exception on close: " + s.getMessage, s)
        }
    }

    def isClosed: Boolean = {
        if (closed)
            return true
        try {
            return DataSourceUtils.getConnection(dataSource).isClosed
        }
        catch {
            case e: CannotGetJdbcConnectionException => {
                DatabaseAccessFactory.LOGGER.warn("Can't get JDBC Connection on isClosed: " + e.getMessage, e)
            }
            case e: SQLException => {
                DatabaseAccessFactory.LOGGER.warn("SQL Exception on isClosed: " + e.getMessage, e)
            }
        }
        false
    }

    def createTransactionTemplate: TransactionTemplate = {
        new TransactionTemplate(transactionManager)
    }
}
