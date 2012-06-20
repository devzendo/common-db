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
import org.springframework.dao.{DataAccessException, DataIntegrityViolationException}
import org.slf4j.LoggerFactory

case class CustomVersion(version: String) extends Version(version)
object TestTransactionHandling {
    val LOGGER = LoggerFactory.getLogger(classOf[TestTransactionHandling])
}
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
                    versionsDao.exists(classOf[CustomVersion])
                }
            }
        )

        existsInTransaction must equal(true)
        versionsDao.exists(classOf[CustomVersion]) must equal(true)
    }

    @Test
    def exceptionCausesDataRollback() {
        createDatabase("rollback")
        val access = database.get
        val transactionTemplate = access.createTransactionTemplate
        val versionsDao = access.versionsDao
        var existsInTransaction = false
        var correctlyCaught = false
        try {
            transactionTemplate.execute(new TransactionCallback[AnyRef]() {
                def doInTransaction(ts: TransactionStatus): AnyRef = {
                    val version = new CustomVersion("1.0")
                    versionsDao.persistVersion(version)
                    existsInTransaction = versionsDao.exists(classOf[CustomVersion])
                    throw new DataIntegrityViolationException("A simulated access failure")
                }
            })
        } catch {
            case dae: DataAccessException =>
                correctlyCaught = true
        }

        correctlyCaught must equal(true)
        existsInTransaction must equal(true)
        versionsDao.exists(classOf[CustomVersion]) must equal(false)
    }

    /**
     * Unfortunately, H2 does not deal nicely DDL inside a
     * transaction - it'll not allow it to be rolled back.
     * <p>
     * This will change though:
     * http://markmail.org/message/3yd3jxfgia7lzpq5?q=h2+transaction+ddl+list:com%2Egooglegroups%2Eh2-database+from:%22Thomas+Mueller%22
     */
    @Test
    def ddlInATransactionCausesCommit() {
        createDatabase("ddlcommit")
        val access = database.get
        val jdbcTemplate = access.jdbcTemplate
        val transactionTemplate = access.createTransactionTemplate
        val versionsDao = access.versionsDao
        var existsInTransaction = false
        try {
            transactionTemplate.execute(new TransactionCallback[AnyRef]() {
                def doInTransaction(ts: TransactionStatus): AnyRef = {
                    val version = new CustomVersion("1.0")
                    versionsDao.persistVersion(version)
                    existsInTransaction = versionsDao.exists(classOf[CustomVersion])
                    // if I do some DDL then force a rollback, the above DML will commit
                    jdbcTemplate.update("CREATE TABLE TEST(ID INT PRIMARY KEY, NAME VARCHAR(255))")
                    jdbcTemplate.update("INSERT INTO TEST (ID, NAME) VALUES(?, ?)", 69: java.lang.Integer, "testobject")
                    throw new DataIntegrityViolationException("A simulated access failure")
                }
            })
        } catch {
            case dae: DataAccessException =>
                // this is tested for elsewhere
                TestTransactionHandling.LOGGER.warn("Correctly caught exception: " + dae.getMessage, dae)
        }

        TestTransactionHandling.LOGGER.info("End of transaction template")
        existsInTransaction must equal(true)
        // Unfortunately, the DML for the Versions table will have been committed.
        versionsDao.exists(classOf[CustomVersion]) must equal(true)
        // But the DML for the TEST table will have been rolled back
        // The TEST table will exist, however.
        val count = jdbcTemplate.queryForInt("SELECT COUNT(*) FROM TEST WHERE NAME = ?", "testobject")
        count must be(0)
    }

}
