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
import java.io.File
import org.junit.Test
import org.easymock.EasyMock
import javax.sql.DataSource
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate
import org.springframework.dao.{DataIntegrityViolationException, DataAccessException}

class TestDatabaseMigrationWorkflow extends AbstractTempFolderUnittest with AutoCloseDatabaseUnittest with AssertionsForJUnit with MustMatchersForJUnit {
    val oldCodeVersion = CodeVersion("1.0")
    val oldSchemaVersion = SchemaVersion("0.4")

    val newCodeVersion = CodeVersion("1.1")
    val newSchemaVersion = SchemaVersion("0.5")

    def createOldDatabase(databaseDirectory: File, databaseName: String, password: Option[String]): Option[DatabaseAccess[_]] = {
        databaseAccessFactory.create(databaseDirectory, databaseName, password, oldCodeVersion, oldSchemaVersion, None, None)
    }

    @Test
    def openOldDatabaseSchemaProgressNotification() {
        createOldDatabase(temporaryDirectory, "oldschemaprogress", None).get.close()

        val openerAdapter = EasyMock.createStrictMock(classOf[OpenWorkflowAdapter])
        EasyMock.checkOrder(openerAdapter, true)
        openerAdapter.startOpening()
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.OpenStarting), EasyMock.eq("Starting to open 'oldschemaprogress'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.Opening), EasyMock.eq("Opening database 'oldschemaprogress'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.Opened), EasyMock.eq("Opened database 'oldschemaprogress'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.MigrationRequired), EasyMock.eq("Database 'oldschemaprogress' requires migration"))
        openerAdapter.requestMigration()
        EasyMock.expectLastCall().andReturn(true)
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.Migrating), EasyMock.eq("Migrating database 'oldschemaprogress'"))
        openerAdapter.migrateSchema(EasyMock.isA(classOf[DataSource]), EasyMock.isA(classOf[SimpleJdbcTemplate]), EasyMock.eq(oldSchemaVersion))
        openerAdapter.migrationSucceeded()
        openerAdapter.stopOpening()
        EasyMock.replay(openerAdapter)

        // It isn't possible to have a newer schema with the same version of
        // code, but I don't want to trigger the code updated progress messages
        // in this test.
        database = databaseAccessFactory.open(temporaryDirectory, "oldschemaprogress", None, oldCodeVersion, newSchemaVersion, Some(openerAdapter), None)
        database must be('defined)

        EasyMock.verify(openerAdapter)
    }

    @Test
    def openOldDatabaseSchemaButCancelMigrationProgressNotification() {
        createOldDatabase(temporaryDirectory, "oldschemacancelmigrationprogress", None).get.close()

        val openerAdapter = EasyMock.createStrictMock(classOf[OpenWorkflowAdapter])
        EasyMock.checkOrder(openerAdapter, true)
        openerAdapter.startOpening()
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.OpenStarting), EasyMock.eq("Starting to open 'oldschemacancelmigrationprogress'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.Opening), EasyMock.eq("Opening database 'oldschemacancelmigrationprogress'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.Opened), EasyMock.eq("Opened database 'oldschemacancelmigrationprogress'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.MigrationRequired), EasyMock.eq("Database 'oldschemacancelmigrationprogress' requires migration"))
        openerAdapter.requestMigration()
        EasyMock.expectLastCall().andReturn(false)
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.MigrationCancelled), EasyMock.eq("Migration of database 'oldschemacancelmigrationprogress' cancelled"))
        openerAdapter.migrationCancelled()
        openerAdapter.stopOpening()
        EasyMock.replay(openerAdapter)

        // It isn't possible to have a newer schema with the same version of
        // code, but I don't want to trigger the code updated progress messages
        // in this test.
        database = databaseAccessFactory.open(temporaryDirectory, "oldschemacancelmigrationprogress", None, oldCodeVersion, newSchemaVersion, Some(openerAdapter), None)
        database must be(None)

        EasyMock.verify(openerAdapter)
    }

    @Test
    def openOldDatabaseSchemaMigrationFailureProgressNotification() {
        createOldDatabase(temporaryDirectory, "oldschemamigrationfailureprogress", None).get.close()

        val openerAdapter = EasyMock.createStrictMock(classOf[OpenWorkflowAdapter])
        EasyMock.checkOrder(openerAdapter, true)
        openerAdapter.startOpening()
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.OpenStarting), EasyMock.eq("Starting to open 'oldschemamigrationfailureprogress'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.Opening), EasyMock.eq("Opening database 'oldschemamigrationfailureprogress'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.Opened), EasyMock.eq("Opened database 'oldschemamigrationfailureprogress'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.MigrationRequired), EasyMock.eq("Database 'oldschemamigrationfailureprogress' requires migration"))
        openerAdapter.requestMigration()
        EasyMock.expectLastCall().andReturn(true)
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.Migrating), EasyMock.eq("Migrating database 'oldschemamigrationfailureprogress'"))
        openerAdapter.migrateSchema(EasyMock.isA(classOf[DataSource]), EasyMock.isA(classOf[SimpleJdbcTemplate]), EasyMock.eq(oldSchemaVersion))
        EasyMock.expectLastCall().andThrow(new DataIntegrityViolationException("Fake failure", new RuntimeException("some cause")))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.MigrationFailed), EasyMock.eq("Migration of database 'oldschemamigrationfailureprogress' failed: Fake failure; nested exception is java.lang.RuntimeException: some cause"))
        openerAdapter.migrationFailed(EasyMock.isA(classOf[DataIntegrityViolationException]))
        openerAdapter.stopOpening()
        EasyMock.replay(openerAdapter)

        // It isn't possible to have a newer schema with the same version of
        // code, but I don't want to trigger the code updated progress messages
        // in this test.
        database = databaseAccessFactory.open(temporaryDirectory, "oldschemamigrationfailureprogress", None, oldCodeVersion, newSchemaVersion, Some(openerAdapter), None)
        database must be(None)

        EasyMock.verify(openerAdapter)
    }

    // TODO schema version must be updated after successful migration
}
