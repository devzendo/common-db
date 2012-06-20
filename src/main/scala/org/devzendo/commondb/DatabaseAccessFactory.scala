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

import java.io.File
import javax.sql.DataSource
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate
import org.slf4j.LoggerFactory
import org.springframework.transaction.support.TransactionTemplate

object DatabaseAccessFactory {
    val LOGGER = LoggerFactory.getLogger(classOf[DatabaseAccessFactory[_]])
}

trait VersionsDao {
    def persistVersion[V <: Version](version: V)
    def findVersion[V <: Version](versionType: Class[V]): Option[V]
    def exists[V <: Version](versionType: Class[V]): Boolean
}

trait SequenceDao {
    def nextSequence: Long
}

abstract class UserDatabaseAccess(val databaseAccess: DatabaseAccess[_]) {
    def close()
}

abstract case class DatabaseAccess[U <: UserDatabaseAccess](
        databasePath: File,
        databaseName: String,
        dataSource: DataSource,
        jdbcTemplate: SimpleJdbcTemplate) {
    var user: Option[U] = None
    def close()
    def isClosed: Boolean
    def versionsDao: VersionsDao
    def sequenceDao: SequenceDao
    def createTransactionTemplate: TransactionTemplate
}

trait DatabaseAccessFactory[U <: UserDatabaseAccess] {
    def create(
        databasePath: File,
        databaseName: String,
        password: Option[String],
        codeVersion: CodeVersion,
        schemaVersion: SchemaVersion,
        workflowAdapter: Option[CreateWorkflowAdapter],
        userDatabaseAccessFactory: Option[Function1[DatabaseAccess[U], U]]): Option[DatabaseAccess[U]]

    def open(
        databasePath: File,
        databaseName: String,
        password: Option[String],
        codeVersion: CodeVersion,
        schemaVersion: SchemaVersion,
        workflowAdapter: Option[OpenWorkflowAdapter],
        userDatabaseAccessFactory: Option[Function1[DatabaseAccess[U], U]]): Option[DatabaseAccess[U]]
}
