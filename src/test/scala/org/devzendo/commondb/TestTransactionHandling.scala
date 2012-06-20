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

import org.scalatest.junit.{MustMatchersForJUnit, AssertionsForJUnit}
import org.junit.Test
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.TransactionStatus

case class CustomVersion(version: String) extends Version(version)
class TestTransactionHandling extends AbstractTempFolderUnittest with AutoCloseDatabaseUnittest with AssertionsForJUnit with MustMatchersForJUnit {
    val codeVersion = CodeVersion("1.0")
    val schemaVersion = SchemaVersion("0.4")

    def createDatabase(databaseName: String)  {
        database = databaseAccessFactory.create(temporaryDirectory, databaseName, None, codeVersion, schemaVersion, None, None)
    }

    @Test
    def commitCausesDataCommit() {
        createDatabase("commit")
        val access = database.get
        val transactionTemplate = access.createTransactionTemplate
        val versionsDao = access.versionsDao
        val existsInTransaction = transactionTemplate.execute(
            new TransactionCallback[Boolean] {
                def doInTransaction(ts: TransactionStatus): Boolean = {
                    val version = new CustomVersion("1.0")
                    versionsDao.persistVersion(version)
                    versionsDao.findVersion(classOf[CustomVersion]).isDefined
                }
            }
        )

        existsInTransaction must be(true)
        versionsDao.findVersion(classOf[CustomVersion]) must be('defined)
    }

}
