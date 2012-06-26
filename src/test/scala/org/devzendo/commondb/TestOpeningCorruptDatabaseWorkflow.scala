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
import org.springframework.dao.DataAccessException
import java.io.{IOException, FileNotFoundException, RandomAccessFile, File}

class TestOpeningCorruptDatabaseWorkflow extends AutoCloseDatabaseCreatingUnittest with AssertionsForJUnit with MustMatchersForJUnit {

    @Test
    def corruptDatabaseDoesNotOpenProgressReporting() {
        val databaseName = "corruptdb"
        databaseAccessFactory.create(temporaryDirectory, databaseName, None, codeVersion, schemaVersion, None, None).get.close()

        corruptDatabase(temporaryDirectory, databaseName)

        val openerAdapter = EasyMock.createStrictMock(classOf[OpenWorkflowAdapter])
        EasyMock.checkOrder(openerAdapter, true)
        openerAdapter.startOpening()
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.OpenStarting), EasyMock.eq("Starting to open 'corruptdb'"))
        openerAdapter.reportProgress(EasyMock.eq(OpenProgressStage.Opening), EasyMock.eq("Opening database 'corruptdb'"))
        openerAdapter.reportProgress(OpenProgressStage.OpenFailed, "Open of 'corruptdb' failed")
        openerAdapter.seriousProblemOccurred(EasyMock.isA(classOf[DataAccessException]))
        openerAdapter.stopOpening()
        EasyMock.replay(openerAdapter)

        databaseAccessFactory.open(temporaryDirectory, databaseName, None, codeVersion, schemaVersion, Some(openerAdapter), None)
        database must not(be('defined))

        EasyMock.verify(openerAdapter)
    }

    /**
     * Corrupt a (database) by writing over the 2nd kilobyte of data. This
     * will result in an open failure due to checksum mismatch.
     */
    def corruptDatabase(databaseDir: File, databaseName: String) {
        val dataFile = new File(databaseDir, databaseName + ".data.db")
        dataFile.exists() must equal(true)
        try {
            val raf = new RandomAccessFile(dataFile, "rw")
            raf.seek(1024)
            raf.writeChars("Let's write all over the database, to see if it'll fail to open!")
            raf.close()
        } catch {
            case fnf: FileNotFoundException => fail("database file %s not found: %s".format(dataFile.getAbsolutePath, fnf.getMessage))
            case io: IOException => fail("IO exception on database file %s: %s".format(dataFile.getAbsolutePath, io.getMessage))
        }
    }
}
