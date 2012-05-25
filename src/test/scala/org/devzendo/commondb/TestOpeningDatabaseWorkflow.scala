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
import java.io.File

class TestOpeningDatabaseWorkflow extends AbstractTempFolderUnittest with AssertionsForJUnit with MustMatchersForJUnit {
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
        openerAdapter.reportProgress(EasyMock.eq(Starting), EasyMock.eq("Starting to open 'doesnotexist'"))
        openerAdapter.reportProgress(EasyMock.eq(Opening), EasyMock.eq("Opening database 'doesnotexist'"))
        openerAdapter.reportProgress(EasyMock.eq(NotPresent), EasyMock.eq("Database 'doesnotexist' not found"))
        openerAdapter.stopOpening()
        EasyMock.replay(openerAdapter)

        databaseAccessFactory.open(temporaryDirectory, "doesnotexist", None, Some(openerAdapter))

        EasyMock.verify(openerAdapter)
    }

    def createDatabase(databaseDirectory: File, databaseName: String, password: Option[String]): Option[DatabaseAccess] = {
        databaseAccessFactory.create(databaseDirectory, databaseName, password, None)
    }
//
//    @Test
//    def plainOpenProgressNotification() {
//        val dbName = "plainprogress"
//        createDatabase(temporaryDirectory, dbName, None).get.close()
//        val openerAdapter = EasyMock.createNiceMock(classOf[OpenWorkflowAdapter])
//        EasyMock.checkOrder(openerAdapter, true)
//        openerAdapter.startOpening()
//        openerAdapter.reportProgress(EasyMock.eq(Starting), EasyMock.eq("Starting to open 'progressplain'"))
//        openerAdapter.reportProgress(EasyMock.eq(Opening), EasyMock.eq("Opening database 'progressplain'"))
//        openerAdapter.reportProgress(EasyMock.eq(Opened), EasyMock.eq("Opened database 'progressplain'"))
//        openerAdapter.stopOpening()
//        EasyMock.replay(openerAdapter)
//
//        val database = databaseAccessFactory.open(temporaryDirectory, dbName, None, Some(openerAdapter))
//
//        try {
//            EasyMock.verify(openerAdapter)
//        } finally {
//            database match {
//                case Some =>
//                    database.get.close()
//            }
//        }
//    }

    @Test
    def plainOpenDatabaseIsActuallyOpen() {
        val dbName = "plainopenisopen"
        createDatabase(temporaryDirectory, dbName, None).get.close()

        val database = databaseAccessFactory.open(temporaryDirectory, dbName, None, None)

        try {
            database must be('defined)
            database.get.isClosed must be(false)
        } finally {
            for (d <- database) {
                d.close()
            }
        }
        database.get.isClosed must be(true)
    }

    @Test
    def plainOpenDatabaseCloseActuallyCloses() {
        val dbName = "plainclose"
        createDatabase(temporaryDirectory, dbName, None).get.close()

        val database = databaseAccessFactory.open(temporaryDirectory, dbName, None, None)

        try {
            database must be('defined)
            database.get.isClosed must be(false)
            database.get.close()
            database.get.isClosed must be(true)
        } finally {
            for (d <- database) {
                d.close()
            }
        }
    }

}
