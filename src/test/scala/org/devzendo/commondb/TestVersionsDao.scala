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

class TestVersionsDao extends AbstractTempFolderUnittest with AssertionsForJUnit with MustMatchersForJUnit {
    val databaseAccessFactory = new DatabaseAccessFactory()

    @Test
    def checkVersionPopulation() {
        val dbName = "checkversionpopulation"
        val codeVersion = CodeVersion("1.0")
        val schemaVersion = SchemaVersion("0.4")

        val database = databaseAccessFactory.create(temporaryDirectory, dbName, None, codeVersion, schemaVersion, None)

        try {
            database must be('defined)
            val databaseAccess = database.get
            def versionsDao = databaseAccess.versionsDao

            def dbSchemaVersion = versionsDao.findVersion(classOf[SchemaVersion])
            dbSchemaVersion must be ('defined)
            dbSchemaVersion.get.getClass must be(classOf[SchemaVersion])
            dbSchemaVersion.get must be(schemaVersion)

            def dbCodeVersion = versionsDao.findVersion(classOf[CodeVersion])
            dbCodeVersion must be ('defined)
            dbCodeVersion.get.getClass must be(classOf[CodeVersion])
            dbCodeVersion.get must be(codeVersion)
        } finally {
            for (d <- database) {
                d.close()
            }
        }
    }
}
