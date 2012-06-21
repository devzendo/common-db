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


case class CustomVersion(version: String) extends Version(version)

class TestVersionsDao extends AbstractTempFolderUnittest with AutoCloseDatabaseUnittest with AssertionsForJUnit with MustMatchersForJUnit {
    val initialCodeVersion = CodeVersion("1.0")
    val initialSchemaVersion = SchemaVersion("0.4")

    @Test
    def checkVersionPopulation() {
        database = databaseAccessFactory.create(temporaryDirectory, "checkversionpopulation", None, initialCodeVersion, initialSchemaVersion, None, None)

        database must be('defined)
        def versionsDao = database.get.versionsDao

        def dbSchemaVersion = versionsDao.findVersion(classOf[SchemaVersion])
        dbSchemaVersion must be ('defined)
        dbSchemaVersion.get.getClass must be(classOf[SchemaVersion])
        dbSchemaVersion.get must be(initialSchemaVersion)

        def dbCodeVersion = versionsDao.findVersion(classOf[CodeVersion])
        dbCodeVersion must be ('defined)
        dbCodeVersion.get.getClass must be(classOf[CodeVersion])
        dbCodeVersion.get must be(initialCodeVersion)
    }

    @Test
    def checkVersionsCanBeUpdated() {
        database = databaseAccessFactory.create(temporaryDirectory, "checkversionscanbeupdated", None, initialCodeVersion, initialSchemaVersion, None, None)

        database must be('defined)
        def versionsDao = database.get.versionsDao
        val newSchemaVersion = SchemaVersion("0.5")
        versionsDao.persistVersion(newSchemaVersion)
        val newCodeVersion = CodeVersion("0.1")
        versionsDao.persistVersion(newCodeVersion)

        def dbSchemaVersion = versionsDao.findVersion(classOf[SchemaVersion])
        dbSchemaVersion must be ('defined)
        dbSchemaVersion.get.getClass must be(classOf[SchemaVersion])
        dbSchemaVersion.get must be(newSchemaVersion)

        def dbCodeVersion = versionsDao.findVersion(classOf[CodeVersion])
        dbCodeVersion must be ('defined)
        dbCodeVersion.get.getClass must be(classOf[CodeVersion])
        dbCodeVersion.get must be(newCodeVersion)
    }


    @Test
    def nonExistentVersionsAreFoundToBeNone() {
        database = databaseAccessFactory.create(temporaryDirectory, "nonexistentversionsarefoundtobenone", None, initialCodeVersion, initialSchemaVersion, None, None)

        database.get.versionsDao.findVersion(classOf[CustomTransactionVersion]) must be(None)
    }

    @Test
    def customVersionsCanBePersisted() {
        database = databaseAccessFactory.create(temporaryDirectory, "customversionscanbepersisted", None, initialCodeVersion, initialSchemaVersion, None, None)

        def versionsDao = database.get.versionsDao
        val customVersion = CustomTransactionVersion("v12.75alpha")
        versionsDao.persistVersion(customVersion)

        val dbCustomVersion = versionsDao.findVersion(classOf[CustomTransactionVersion])
        dbCustomVersion must be ('defined)
        dbCustomVersion.get.getClass must be(classOf[CustomTransactionVersion])
        dbCustomVersion.get must be(customVersion)
    }
}
