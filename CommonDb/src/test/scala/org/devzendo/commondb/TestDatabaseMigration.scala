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
import org.easymock.EasyMock
import org.springframework.dao.{DataAccessException, DataIntegrityViolationException}

case class MigrationTransactionVersion(version: String) extends Version(version)


class TestDatabaseMigration extends AbstractDatabaseMigrationUnittest with AssertionsForJUnit with MustMatchersForJUnit {

    @Test
    def openOldDatabaseUpdatesSchemaVersionToCurrent() {
        val databaseName = "oldschemaupdate"
        createOldDatabase(databaseName).get.close()
        val openerAdapter = createSucceedingMigratingAdapter()
        val userDatabaseMigrator = createSucceedingMigrator()

        database = openNewDatabase(databaseName, openerAdapter, userDatabaseMigrator)
        val updatedSchemaVersion = database.get.versionsDao.findVersion(classOf[SchemaVersion]).get

        updatedSchemaVersion must be(newSchemaVersion)
        EasyMock.verify(openerAdapter, userDatabaseMigrator)
    }

    @Test
    def openOldDatabaseMigrationCanPerformChanges() {
        val databaseName = "oldschemachangesmade"
        createOldDatabase(databaseName).get.close()
        val openerAdapter = createSucceedingMigratingAdapter()

        val userDatabaseMigrator = new UserDatabaseMigrator {
            def migrateSchema(access: DatabaseAccess[_], currentSchemaVersion: SchemaVersion) {
                access.jdbcTemplate.getJdbcOperations.execute("CREATE TABLE Cheeses (name VARCHAR(40), age VARCHAR(40))")
                val data: List[(String, Int)] = List(("Edam", 4), ("Cheddar", 1))
                for (cake_age <- data) {
                    access.jdbcTemplate.update("INSERT INTO Cheeses (name, age) VALUES (?, ?)",
                        cake_age._1, cake_age._2: java.lang.Integer)
                }
            }
        }

        database = openNewDatabase(databaseName, openerAdapter, userDatabaseMigrator)
        val jdbcTemplate = database.get.jdbcTemplate
        val expected: List[(String, Int)] = List(("Edam", 1), ("Cheddar", 1), ("Gorgonzola", 0))
        val actual: List[Int] = for(e <- expected) yield
            jdbcTemplate.queryForInt("SELECT COUNT(0) FROM Cheeses WHERE name = ?", e._1)

        (expected, actual).zip.foreach((e: ((String, Int), Int)) => {(e._1)._2 must be(e._2)})

        EasyMock.verify(openerAdapter)
    }

    private[this] def createSucceedingMigratingAdapter(): OpenWorkflowAdapter = {
        val openerAdapter = EasyMock.createMock(classOf[OpenWorkflowAdapter])
        openerAdapter.startOpening()
        openerAdapter.reportProgress(EasyMock.isA(classOf[OpenProgressStage.Enum]), EasyMock.isA(classOf[String]))
        EasyMock.expectLastCall().anyTimes()
        openerAdapter.requestMigration()
        EasyMock.expectLastCall().andReturn(true)
        openerAdapter.migrationSucceeded()
        openerAdapter.stopOpening()
        EasyMock.replay(openerAdapter)

        openerAdapter
    }

    private[this] def createSucceedingMigrator(): UserDatabaseMigrator = {
        val userDatabaseMigrator = EasyMock.createMock(classOf[UserDatabaseMigrator])
        userDatabaseMigrator.migrateSchema(EasyMock.isA(classOf[DatabaseAccess[_]]), EasyMock.eq(oldSchemaVersion))
        EasyMock.replay(userDatabaseMigrator)

        userDatabaseMigrator
    }

    /**
     * Being mindful of H2's restriction on executing DDL in a transaction;
     * see TestTransactionHandling.
     */
    @Test
    def migrationsArePerformedInATransactionThatIsRolledBackOnFailure() {
        val databaseName = "failedmigrationrollsback"
        createOldDatabase(databaseName).get.close()
        val openerAdapter = createFailingMigratingAdapter()
        var existsInTransaction = false

        val userDatabaseMigrator = new UserDatabaseMigrator {
            def migrateSchema(access: DatabaseAccess[_], currentSchemaVersion: SchemaVersion) {
                val versionsDao = access.versionsDao
                val version = new MigrationTransactionVersion("1.0")
                versionsDao.persistVersion(version)
                existsInTransaction = versionsDao.exists(classOf[MigrationTransactionVersion])
                throw new DataIntegrityViolationException("A simulated access failure")
            }
        }

        database = openNewDatabase(databaseName, openerAdapter, userDatabaseMigrator)
        database must be(None)
        existsInTransaction must equal(true)
        EasyMock.verify(openerAdapter)

        val oldDatabase = openOldDatabase(databaseName)
        try {
            // If we can open it successfully with the old version, it wasn't
            // migrated (the schema version wasn't updated) - else we'd get a
            // MigrationNotPossible (opening future database)
            oldDatabase must be('defined)
            // let's prove the transaction was rolled back...
            val versionsDao = oldDatabase.get.versionsDao
            versionsDao.exists(classOf[MigrationTransactionVersion]) must equal(false)
        } finally {
            for (o <- oldDatabase) {
                o.close()
            }
        }
    }

    private[this] def createFailingMigratingAdapter(): OpenWorkflowAdapter = {
        val openerAdapter = EasyMock.createMock(classOf[OpenWorkflowAdapter])
        openerAdapter.startOpening()
        openerAdapter.reportProgress(EasyMock.isA(classOf[OpenProgressStage.Enum]), EasyMock.isA(classOf[String]))
        EasyMock.expectLastCall().anyTimes()
        openerAdapter.requestMigration()
        EasyMock.expectLastCall().andReturn(true)
        openerAdapter.migrationFailed(EasyMock.isA(classOf[DataAccessException]))
        openerAdapter.stopOpening()
        EasyMock.replay(openerAdapter)

        openerAdapter
    }
}
