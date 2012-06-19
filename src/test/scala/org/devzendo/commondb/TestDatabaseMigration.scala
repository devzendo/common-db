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
import org.junit.{After, Test}
import org.easymock.EasyMock
import javax.sql.DataSource
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate

class TestDatabaseMigration extends AbstractDatabaseMigrationUnittest with AssertionsForJUnit with MustMatchersForJUnit {

    val openerAdapter = createMigratingAdapter()

    @After
    def verifyAdapter() {
        EasyMock.verify(openerAdapter)
    }

    @Test
    def openOldDatabaseUpdatesSchemaVersionToCurrent() {
        val databaseName = "oldschemaupdate"
        createOldDatabase(databaseName).get.close()

        database = openNewDatabase(databaseName, openerAdapter)
        val updatedSchemaVersion = database.get.versionsDao.findVersion(classOf[SchemaVersion]).get

        updatedSchemaVersion must be(newSchemaVersion)
    }

    private[this] def createMigratingAdapter(): OpenWorkflowAdapter = {
        val openerAdapter = EasyMock.createMock(classOf[OpenWorkflowAdapter])
        openerAdapter.startOpening()
        openerAdapter.reportProgress(EasyMock.isA(classOf[OpenProgressStage.Enum]), EasyMock.isA(classOf[String]))
        EasyMock.expectLastCall().anyTimes()
        openerAdapter.requestMigration()
        EasyMock.expectLastCall().andReturn(true)
        openerAdapter.migrateSchema(EasyMock.isA(classOf[DataSource]), EasyMock.isA(classOf[SimpleJdbcTemplate]), EasyMock.eq(oldSchemaVersion))
        openerAdapter.migrationSucceeded()
        openerAdapter.stopOpening()
        EasyMock.replay(openerAdapter)

        openerAdapter
    }



    // TODO migration can effect the database

    // TODO migrations occur in a transaction.


    // TODO migration failure rolls back the transaction

}
