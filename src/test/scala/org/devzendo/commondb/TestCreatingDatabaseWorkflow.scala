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

import org.junit.Test
import org.easymock.EasyMock
import org.scalatest.junit.{MustMatchersForJUnit, AssertionsForJUnit}

class TestCreatingDatabaseWorkflow extends AbstractTempFolderUnittest with AssertionsForJUnit with MustMatchersForJUnit {
    val databaseAccessFactory = new DatabaseAccessFactory()
    val codeVersion = CodeVersion("1.0")
    val schemaVersion = SchemaVersion("0.4")

    @Test
    def createDatabaseReturnsSome() {
        val database = databaseAccessFactory.create(temporaryDirectory, "newdb", None, codeVersion, schemaVersion, None)
        for (d <- database) {
            d.close()
        }

        database must be('defined)
    }

    @Test
    def createDatabaseProgressReporting() {
        val creatorAdapter = EasyMock.createNiceMock(classOf[CreateWorkflowAdapter])
        EasyMock.checkOrder(creatorAdapter, true)
        creatorAdapter.startCreating()
        creatorAdapter.reportProgress(EasyMock.eq(Creating), EasyMock.eq("Starting to create 'newdb'"))
//        creatorAdapter.reportProgress(EasyMock.eq(Opening), EasyMock.eq("Opening database 'doesnotexist'"))
//        creatorAdapter.reportProgress(EasyMock.eq(NotPresent), EasyMock.eq("Database 'doesnotexist' not found"))
        creatorAdapter.reportProgress(EasyMock.eq(Created), EasyMock.eq("Created 'newdb'"))
        creatorAdapter.stopCreating()
        EasyMock.replay(creatorAdapter)

        val database = databaseAccessFactory.create(temporaryDirectory, "newdb", None, codeVersion, schemaVersion, Some(creatorAdapter))
        for (d <- database) {
            d.close()
        }

        EasyMock.verify(creatorAdapter)
    }
}
