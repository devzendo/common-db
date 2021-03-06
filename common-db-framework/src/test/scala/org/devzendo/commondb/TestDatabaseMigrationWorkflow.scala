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

import org.easymock.EasyMock
import org.junit.Test
import org.scalatest.MustMatchers
import org.scalatest.junit.AssertionsForJUnit
import org.springframework.dao.DataIntegrityViolationException

class TestDatabaseMigrationWorkflow extends AbstractDatabaseMigrationUnittest with AssertionsForJUnit with MustMatchers {
    @Test
    def openOldDatabaseSchemaProgressNotification() {
        val databaseName = "oldschemaprogress"
        createOldDatabase(databaseName).get.close()

        val openerAdapter = EasyMock.createStrictMock(classOf[OpenWorkflowAdapter])
        EasyMock.checkOrder(openerAdapter, true)
        val userDatabaseMigrator = EasyMock.createStrictMock(classOf[UserDatabaseMigrator])
        EasyMock.checkOrder(userDatabaseMigrator, true)
        openerAdapter.startOpening()
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.OpenStarting), EasyMock.eq("Starting to open 'oldschemaprogress'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.Opening), EasyMock.eq("Opening database 'oldschemaprogress'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.Opened), EasyMock.eq("Opened database 'oldschemaprogress'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.MigrationRequired), EasyMock.eq("Database 'oldschemaprogress' requires migration"))
        openerAdapter.requestMigration()
        EasyMock.expectLastCall().andReturn(true)
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.Migrating), EasyMock.eq("Migrating database 'oldschemaprogress'"))
        userDatabaseMigrator.migrateSchema(EasyMock.isA(classOf[DatabaseAccess[_]]), EasyMock.eq(oldSchemaVersion))
        openerAdapter.migrationSucceeded()
        openerAdapter.stopOpening()
        EasyMock.replay(openerAdapter, userDatabaseMigrator)

        database = openNewDatabase(databaseName, openerAdapter, userDatabaseMigrator)
        database must be('defined)

        EasyMock.verify(openerAdapter)
    }


    @Test
    def openOldDatabaseSchemaButCancelMigrationProgressNotification() {
        val databaseName = "oldschemacancelmigrationprogress"
        createOldDatabase(databaseName).get.close()

        val openerAdapter = EasyMock.createStrictMock(classOf[OpenWorkflowAdapter])
        EasyMock.checkOrder(openerAdapter, true)
        val userDatabaseMigrator = EasyMock.createStrictMock(classOf[UserDatabaseMigrator])
        EasyMock.checkOrder(userDatabaseMigrator, true)
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
        EasyMock.replay(openerAdapter, userDatabaseMigrator)

        database = openNewDatabase(databaseName, openerAdapter, userDatabaseMigrator)
        database must be(None)

        EasyMock.verify(openerAdapter)
    }

    @Test
    def openOldDatabaseSchemaMigrationFailureProgressNotification() {
        val databaseName = "oldschemamigrationfailureprogress"
        createOldDatabase(databaseName).get.close()

        val openerAdapter = EasyMock.createStrictMock(classOf[OpenWorkflowAdapter])
        EasyMock.checkOrder(openerAdapter, true)
        val userDatabaseMigrator = EasyMock.createStrictMock(classOf[UserDatabaseMigrator])
        EasyMock.checkOrder(userDatabaseMigrator, true)
        openerAdapter.startOpening()
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.OpenStarting), EasyMock.eq("Starting to open 'oldschemamigrationfailureprogress'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.Opening), EasyMock.eq("Opening database 'oldschemamigrationfailureprogress'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.Opened), EasyMock.eq("Opened database 'oldschemamigrationfailureprogress'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.MigrationRequired), EasyMock.eq("Database 'oldschemamigrationfailureprogress' requires migration"))
        openerAdapter.requestMigration()
        EasyMock.expectLastCall().andReturn(true)
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.Migrating), EasyMock.eq("Migrating database 'oldschemamigrationfailureprogress'"))
        userDatabaseMigrator.migrateSchema(EasyMock.isA(classOf[DatabaseAccess[_]]), EasyMock.eq(oldSchemaVersion))
        EasyMock.expectLastCall().andThrow(new DataIntegrityViolationException("Fake failure", new RuntimeException("some cause")))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.MigrationFailed), EasyMock.eq("Migration of database 'oldschemamigrationfailureprogress' failed: Fake failure; nested exception is java.lang.RuntimeException: some cause"))
        openerAdapter.migrationFailed(EasyMock.isA(classOf[DataIntegrityViolationException]))
        openerAdapter.stopOpening()
        EasyMock.replay(openerAdapter, userDatabaseMigrator)

        database = openNewDatabase(databaseName, openerAdapter, userDatabaseMigrator)
        database must be(None)

        EasyMock.verify(openerAdapter, userDatabaseMigrator)
    }

    @Test
    def openFutureDatabaseSchemaProgressNotification() {
        val databaseName = "futureschemamigrationfailureprogress"
        createNewDatabase(databaseName).get.close() // NOTE: new

        val openerAdapter = EasyMock.createStrictMock(classOf[OpenWorkflowAdapter])
        EasyMock.checkOrder(openerAdapter, true)
        val userDatabaseMigrator = EasyMock.createStrictMock(classOf[UserDatabaseMigrator])
        EasyMock.checkOrder(userDatabaseMigrator, true)
        openerAdapter.startOpening()
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.OpenStarting), EasyMock.eq("Starting to open 'futureschemamigrationfailureprogress'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.Opening), EasyMock.eq("Opening database 'futureschemamigrationfailureprogress'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.Opened), EasyMock.eq("Opened database 'futureschemamigrationfailureprogress'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.MigrationNotPossible), EasyMock.eq("Database 'futureschemamigrationfailureprogress' has a more modern schema than this application supports"))
        openerAdapter.migrationNotPossible()
        openerAdapter.stopOpening()
        EasyMock.replay(openerAdapter, userDatabaseMigrator)

        database = openOldDatabase(databaseName, openerAdapter, userDatabaseMigrator)
        database must be(None)

        EasyMock.verify(openerAdapter, userDatabaseMigrator)
    }
}
