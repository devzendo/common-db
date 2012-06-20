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
import org.springframework.dao.{DataAccessResourceFailureException, DataAccessException}

trait MigratingOpenWorkflowAdapter extends OpenWorkflowAdapter {
    def startOpening() {}
    def reportProgress(progressStage: OpenProgressStage.Enum, description: String) {}
    def requestPassword() = null
    def requestMigration() = true
    def migrationSucceeded() {}
    def migrationCancelled() {}
    def migrationNotPossible() {}
    def migrationFailed(exception: DataAccessException) {}
    def databaseNotFound(exception: DataAccessResourceFailureException) {}
    def seriousProblemOccurred(exception: DataAccessException) {}
    def stopOpening() {}
}

class TestDatabaseMigration extends AbstractDatabaseMigrationUnittest with AssertionsForJUnit with MustMatchersForJUnit {

    @Test
    def openOldDatabaseUpdatesSchemaVersionToCurrent() {
        val databaseName = "oldschemaupdate"
        createOldDatabase(databaseName).get.close()
        val openerAdapter = createMigratingAdapter()

        database = openNewDatabase(databaseName, openerAdapter)
        val updatedSchemaVersion = database.get.versionsDao.findVersion(classOf[SchemaVersion]).get

        updatedSchemaVersion must be(newSchemaVersion)
        EasyMock.verify(openerAdapter)
    }

    @Test
    def openOldDatabaseMigrationCanPerformChanges() {
        val databaseName = "oldschemachangesmade"
        createOldDatabase(databaseName).get.close()
        val openerAdapter = new MigratingOpenWorkflowAdapter {
            def migrateSchema(access: DatabaseAccess[_], currentSchemaVersion: SchemaVersion) {
                access.jdbcTemplate.getJdbcOperations.execute("CREATE TABLE Cheeses (name VARCHAR(40), age VARCHAR(40))")
                val data: List[(String, Int)] = List(("Edam", 4), ("Cheddar", 1))
                for (cake_age <- data) {
                    access.jdbcTemplate.update("INSERT INTO Cheeses (name, age) VALUES (?, ?)",
                        cake_age._1, cake_age._2: java.lang.Integer)
                }
            }
        }

        database = openNewDatabase(databaseName, openerAdapter)
        val jdbcTemplate = database.get.jdbcTemplate
        val expected: List[(String, Int)] = List(("Edam", 1), ("Cheddar", 1), ("Gorgonzola", 0))
        val actual: List[Int] = for(e <- expected) yield
            jdbcTemplate.queryForInt("SELECT COUNT(0) FROM Cheeses WHERE name = ?", e._1)

        (expected, actual).zip.foreach((e: ((String, Int), Int)) => {(e._1)._2 must be(e._2)})
    }

    private[this] def createMigratingAdapter(): OpenWorkflowAdapter = {
        val openerAdapter = EasyMock.createMock(classOf[OpenWorkflowAdapter])
        openerAdapter.startOpening()
        openerAdapter.reportProgress(EasyMock.isA(classOf[OpenProgressStage.Enum]), EasyMock.isA(classOf[String]))
        EasyMock.expectLastCall().anyTimes()
        openerAdapter.requestMigration()
        EasyMock.expectLastCall().andReturn(true)
        openerAdapter.migrateSchema(EasyMock.isA(classOf[DatabaseAccess[_]]), EasyMock.eq(oldSchemaVersion))
        openerAdapter.migrationSucceeded()
        openerAdapter.stopOpening()
        EasyMock.replay(openerAdapter)

        openerAdapter
    }

    // TODO migrations occur in a transaction.


    // TODO migration failure rolls back the transaction

}
