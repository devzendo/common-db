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

class TestOpeningEncryptedDatabaseWorkflow extends AutoCloseDatabaseCreatingUnittest with AssertionsForJUnit with MustMatchersForJUnit {
    val creationPassword = Password("Squeamish Ossifrage")

    @Test
    def encryptedDatabaseCannotBeOpenedWithWrongPasswordAttemptAbandoned() {
        val databaseName = "encrypteddbopenwithwrongpasswordattemptabandoned"
        databaseAccessFactory.create(temporaryDirectory, databaseName, Some(creationPassword), codeVersion, schemaVersion, None, None).get.close()

        val openerAdapter = EasyMock.createStrictMock(classOf[OpenWorkflowAdapter])
        EasyMock.checkOrder(openerAdapter, true)
        openerAdapter.startOpening()
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.OpenStarting), EasyMock.eq("Starting to open 'encrypteddbopenwithwrongpasswordattemptabandoned'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.Opening), EasyMock.eq("Opening database 'encrypteddbopenwithwrongpasswordattemptabandoned'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.PasswordRequired), EasyMock.eq("Password required for 'encrypteddbopenwithwrongpasswordattemptabandoned'"))
        openerAdapter.requestPassword()
        EasyMock.expectLastCall().andReturn(None)
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.PasswordCancelled), EasyMock.eq("Open of 'encrypteddbopenwithwrongpasswordattemptabandoned' cancelled"))
        openerAdapter.stopOpening()
        EasyMock.replay(openerAdapter)

        val incorrectPassword = Password("evilhacker")
        database = databaseAccessFactory.open(temporaryDirectory, databaseName, Some(incorrectPassword), codeVersion, schemaVersion, Some(openerAdapter), None)
        database must not(be('defined))

        EasyMock.verify(openerAdapter)
    }

    @Test
    def encryptedDatabaseCannotBeOpenedWithWrongPasswordHaveAnotherGo() {
        val databaseName = "encrypteddbopenwithwrongpasswordhaveanothergo"
        databaseAccessFactory.create(temporaryDirectory, databaseName, Some(creationPassword), codeVersion, schemaVersion, None, None).get.close()

        val openerAdapter = EasyMock.createStrictMock(classOf[OpenWorkflowAdapter])
        EasyMock.checkOrder(openerAdapter, true)
        openerAdapter.startOpening()
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.OpenStarting), EasyMock.eq("Starting to open 'encrypteddbopenwithwrongpasswordhaveanothergo'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.Opening), EasyMock.eq("Opening database 'encrypteddbopenwithwrongpasswordhaveanothergo'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.PasswordRequired), EasyMock.eq("Password required for 'encrypteddbopenwithwrongpasswordhaveanothergo'"))
        // first bad attempt
        openerAdapter.requestPassword()
        EasyMock.expectLastCall().andReturn(Some(Password("notquitetherightone")))
        // note different message second time around
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.Opening), EasyMock.eq("Trying to open database 'encrypteddbopenwithwrongpasswordhaveanothergo'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.PasswordRequired), EasyMock.eq("Password required for 'encrypteddbopenwithwrongpasswordhaveanothergo'"))
        // second bad attempt
        openerAdapter.requestPassword()
        EasyMock.expectLastCall().andReturn(Some(Password("stillnotright")))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.Opening), EasyMock.eq("Trying to open database 'encrypteddbopenwithwrongpasswordhaveanothergo'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.PasswordRequired), EasyMock.eq("Password required for 'encrypteddbopenwithwrongpasswordhaveanothergo'"))
        // now get it right
        openerAdapter.requestPassword()
        EasyMock.expectLastCall().andReturn(Some(creationPassword))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.Opening), EasyMock.eq("Trying to open database 'encrypteddbopenwithwrongpasswordhaveanothergo'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.Opened), EasyMock.eq("Opened database 'encrypteddbopenwithwrongpasswordhaveanothergo'"))
        openerAdapter.stopOpening()
        EasyMock.replay(openerAdapter)

        val incorrectPassword = Password("evilhacker")
        database = databaseAccessFactory.open(temporaryDirectory, databaseName, Some(incorrectPassword), codeVersion, schemaVersion, Some(openerAdapter), None)
        database must be('defined)

        EasyMock.verify(openerAdapter)
    }
}
