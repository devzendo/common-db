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

class OpeningDatabaseWorkflow extends AbstractTempFolderUnittest {
    val databaseAccessFactory = new DatabaseAccessFactory()

    @Test
    def databaseDoesNotExistSoReturnsNone() {
        databaseAccessFactory.open(temporaryDirectory, "doesnotexist", None, None) must be(None)
    }

    @Test
    def databaseDoesNotExistProgressReporting() {
        val openerAdapter = EasyMock.createNiceMock(classOf[OpenWorkflowAdapter])
        EasyMock.checkOrder(openerAdapter, true)
        openerAdapter.startOpening()
        openerAdapter.reportProgress(EasyMock.eq(Starting), EasyMock.isA(classOf[String]))
        openerAdapter.reportProgress(EasyMock.eq(Opening), EasyMock.eq("Opening database 'doesnotexist'"))
        openerAdapter.reportProgress(EasyMock.eq(NotPresent), EasyMock.isA(classOf[String]))
        openerAdapter.stopOpening()
        EasyMock.replay(openerAdapter)

        databaseAccessFactory.open(temporaryDirectory, "doesnotexist", None, Some(openerAdapter))

        EasyMock.verify(openerAdapter)
    }
}
