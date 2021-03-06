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

import org.devzendo.commondb.dao.{CodeVersion, SchemaVersion}
import org.junit.Test
import org.scalatest.MustMatchers
import org.scalatest.junit.AssertionsForJUnit

trait CheeseDao {
    def nextSequence: Long // just a copy of the sequence dao operation
}

class CheeseDatabaseAccess(override val databaseAccess: DatabaseAccess[CheeseDatabaseAccess]) extends UserDatabaseAccess(databaseAccess) {
    var closeCalled = false

    def cheeseDao = new CheeseDao {
        def nextSequence = {
            databaseAccess.jdbcTemplate.queryForLong("SELECT NEXT VALUE FOR SEQUENCE Sequence")
        }
    }

    def close() {
        closeCalled = true
    }
}

class TestUserDatabaseAccess extends AbstractTempFolderUnittest with AssertionsForJUnit with MustMatchers {
    val codeVersion = CodeVersion("1.0")
    val schemaVersion = SchemaVersion("0.4")

    val cheesyDatabaseAccessFactory = DatabaseAccessFactory[CheeseDatabaseAccess]()

    val cheeseUserDatabaseFactory = new ((DatabaseAccess[CheeseDatabaseAccess]) => CheeseDatabaseAccess) {
        def apply(databaseAccess: DatabaseAccess[CheeseDatabaseAccess]) = new CheeseDatabaseAccess(databaseAccess)
    }

    def createCheeseDatabase(name: String) = {
        cheesyDatabaseAccessFactory.create(temporaryDirectory, name, None, codeVersion, schemaVersion, None, None,
            Some(cheeseUserDatabaseFactory))
    }

    def openCheeseDatabase(name: String) = {
        cheesyDatabaseAccessFactory.open(temporaryDirectory, name, None, codeVersion, schemaVersion, None, None,
            Some(cheeseUserDatabaseFactory))
    }

    def performUserAccessChecks(userDatabase: Option[DatabaseAccess[CheeseDatabaseAccess]]) {
        userDatabase must be('defined)
        val userAccess = userDatabase.get.user
        userAccess must be('defined)
        def cheeseDao = userAccess.get.cheeseDao

        cheeseDao.nextSequence must be(0L)
        cheeseDao.nextSequence must be(1L)
    }

    @Test
    def userAccessIsPossibleAfterCreation() {
        val userDatabase = createCheeseDatabase("useraccesscreate")

        try {
            performUserAccessChecks(userDatabase)
        } finally {
            for (u <- userDatabase) {
                u.close()
            }
        }
    }

    @Test
    def userAccessIsPossibleAfterOpening() {
        val dbName = "useraccessopen"
        createCheeseDatabase(dbName).get.close()

        val userDatabase = openCheeseDatabase(dbName)

        try {
            performUserAccessChecks(userDatabase)
        } finally {
            for (u <- userDatabase) {
                u.close()
            }
        }
    }

    @Test
    def closeIsCalledInUserAccessUponClose() {
        val userDatabase = createCheeseDatabase("useraccessclose").get
        val userAccess = userDatabase.user.get
        userAccess.closeCalled must equal(false)
        userDatabase.isClosed must equal(false)

        userDatabase.close()

        userAccess.closeCalled must equal(true)
        userDatabase.isClosed must equal(true)
    }
}
