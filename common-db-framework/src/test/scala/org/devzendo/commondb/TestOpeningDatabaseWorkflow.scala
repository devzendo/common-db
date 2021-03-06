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
import org.springframework.dao.DataAccessResourceFailureException

class TestOpeningDatabaseWorkflow extends AutoCloseDatabaseCreatingUnittest with AssertionsForJUnit with MustMatchers {

    @Test
    def databaseDoesNotExistSoReturnsNone() {
        databaseAccessFactory.open(temporaryDirectory, "doesnotexist", None, codeVersion, schemaVersion, None, None, None) must be(None)
    }

    @Test
    def databaseDoesNotExistProgressReporting() {
        val openerAdapter = EasyMock.createStrictMock(classOf[OpenWorkflowAdapter])
        EasyMock.checkOrder(openerAdapter, true)
        openerAdapter.startOpening()
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.OpenStarting), EasyMock.eq("Starting to open 'doesnotexist'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.Opening), EasyMock.eq("Opening database 'doesnotexist'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.NotPresent), EasyMock.eq("Database 'doesnotexist' not found"))
        openerAdapter.databaseNotFound(EasyMock.isA(classOf[DataAccessResourceFailureException]))
        openerAdapter.stopOpening()
        EasyMock.replay(openerAdapter)

        databaseAccessFactory.open(temporaryDirectory, "doesnotexist", None, codeVersion, schemaVersion, Some(openerAdapter), None, None)

        EasyMock.verify(openerAdapter)
    }

    @Test
    def plainOpenProgressNotification() {
        createDatabase(temporaryDirectory, "plainprogress", None).get.close()
        val openerAdapter = EasyMock.createStrictMock(classOf[OpenWorkflowAdapter])
        EasyMock.checkOrder(openerAdapter, true)
        openerAdapter.startOpening()
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.OpenStarting), EasyMock.eq("Starting to open 'plainprogress'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.Opening), EasyMock.eq("Opening database 'plainprogress'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.Opened), EasyMock.eq("Opened database 'plainprogress'"))
        openerAdapter.stopOpening()
        EasyMock.replay(openerAdapter)

        database = databaseAccessFactory.open(temporaryDirectory, "plainprogress", None, codeVersion, schemaVersion, Some(openerAdapter), None, None)

        EasyMock.verify(openerAdapter)
    }

    @Test
    def plainOpenDatabaseIsActuallyOpen() {
        createDatabase(temporaryDirectory, "plainopenisopen", None).get.close()

        database = databaseAccessFactory.open(temporaryDirectory, "plainopenisopen", None, codeVersion, schemaVersion, None, None, None)

        database must be('defined)
        database.get.isClosed must equal(false)
    }

    @Test
    def plainOpenDatabaseCloseActuallyCloses() {
        createDatabase(temporaryDirectory, "plainclose", None).get.close()

        database = databaseAccessFactory.open(temporaryDirectory, "plainclose", None, codeVersion, schemaVersion, None, None, None)

        database must be('defined)
        database.get.close()
        database.get.isClosed must equal(true)
    }
}
