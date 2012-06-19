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
import javax.sql.DataSource
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate

class TestDatabaseMigration extends AbstractDatabaseMigrationUnittest with AssertionsForJUnit with MustMatchersForJUnit {
    @Test
    def openOldDatabaseUpdatesSchemaVersionToCurrent() {
        val databaseName = "oldschemaupdate"
        createOldDatabase(databaseName).get.close()

        val openerAdapter = EasyMock.createStrictMock(classOf[OpenWorkflowAdapter])
        EasyMock.checkOrder(openerAdapter, true)
        openerAdapter.startOpening()
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.OpenStarting), EasyMock.eq("Starting to open 'oldschemaupdate'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.Opening), EasyMock.eq("Opening database 'oldschemaupdate'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.Opened), EasyMock.eq("Opened database 'oldschemaupdate'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.MigrationRequired), EasyMock.eq("Database 'oldschemaupdate' requires migration"))
        openerAdapter.requestMigration()
        EasyMock.expectLastCall().andReturn(true)
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.Migrating), EasyMock.eq("Migrating database 'oldschemaupdate'"))
        openerAdapter.migrateSchema(EasyMock.isA(classOf[DataSource]), EasyMock.isA(classOf[SimpleJdbcTemplate]), EasyMock.eq(oldSchemaVersion))
        openerAdapter.migrationSucceeded()
        openerAdapter.stopOpening()
        EasyMock.replay(openerAdapter)

        database = openNewDatabase(databaseName, openerAdapter)
        val updatedSchemaVersion = database.get.versionsDao.findVersion(classOf[SchemaVersion]).get

        updatedSchemaVersion must be(newSchemaVersion)

        EasyMock.verify(openerAdapter)
    }
}
