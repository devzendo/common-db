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
import org.devzendo.commondb.impl.JdbcTemplateDatabaseAccessFactory
import org.junit.Test
import org.scalatest.MustMatchers
import org.scalatest.junit.AssertionsForJUnit
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException

object TestUserVersionsAccess {
    val LOGGER = LoggerFactory.getLogger(classOf[TestUserVersionsAccess])
}

class RobotDatabaseAccess(override val databaseAccess: DatabaseAccess[RobotDatabaseAccess]) extends UserDatabaseAccess(databaseAccess) {
    def close() {
    }
}

class RobotCreateWorkflowAdapter extends CreateWorkflowAdapter {

    def reportProgress(progressStage: CreateProgressStage.Enum, description: String) {
    }

    def startCreating() {
    }

    def seriousProblemOccurred(exception: DataAccessException) {
    }

    def stopCreating() {
    }
}

class RobotUserDatabaseCreator extends UserDatabaseCreator {
    def createApplicationTables(access: DatabaseAccess[_]) {
    }

    def populateApplicationTables(access: DatabaseAccess[_]) {
    }
}

class RobotUserDatabaseFactory extends ((DatabaseAccess[RobotDatabaseAccess]) => RobotDatabaseAccess) {
    var versionsDaoAvailableAtCreate = false
    var versionsAvailableAtCreate = false
    var applyCalled = false

    def apply(databaseAccess: DatabaseAccess[RobotDatabaseAccess]) = {
        applyCalled = true

        versionsAvailableAtCreate = versionsAvailable(databaseAccess)
        TestUserVersionsAccess.LOGGER.info("versionsAvailableAtCreate is " + versionsAvailableAtCreate)
        versionsDaoAvailableAtCreate = databaseAccess.versionsDao != null
        TestUserVersionsAccess.LOGGER.info("versionsDaoAvailableAtCreate is " + versionsDaoAvailableAtCreate)

        new RobotDatabaseAccess(databaseAccess)
    }

    private[this] def versionsAvailable(access: DatabaseAccess[_]): Boolean = {
        try {
            access.jdbcTemplate.queryForInt("SELECT COUNT(0) FROM Versions")
            TestUserVersionsAccess.LOGGER.info("Versions table exists")
            return true
        } catch {
            case dae: DataAccessException =>
                TestUserVersionsAccess.LOGGER.warn("Versions table does not exist?", dae)
        }
        false
    }
}

class TestUserVersionsAccess extends AbstractTempFolderUnittest with AssertionsForJUnit with MustMatchers {
    val codeVersion = CodeVersion("1.0")
    val schemaVersion = SchemaVersion("0.4")

    val robotDatabaseAccessFactory = new JdbcTemplateDatabaseAccessFactory[RobotDatabaseAccess]()

    val robotUserDatabaseFactory = new RobotUserDatabaseFactory

    @Test
    def versionsTableAccessibleWhenUserAccessFactoryIsCreating() {
        val openWorkflowAdapter = new RobotCreateWorkflowAdapter
        val userDatabaseCreator = new RobotUserDatabaseCreator
        val userDatabase = robotDatabaseAccessFactory.create(temporaryDirectory, "versionsatcreate", None, codeVersion, schemaVersion,
            Some(openWorkflowAdapter),
            Some(userDatabaseCreator),
            Some(robotUserDatabaseFactory))

        userDatabase must be('defined)
        try {
            robotUserDatabaseFactory.applyCalled must equal(true)
            robotUserDatabaseFactory.versionsAvailableAtCreate must equal(true)
            robotUserDatabaseFactory.versionsDaoAvailableAtCreate must equal(true)
        } finally {
            for (u <- userDatabase) {
                u.close()
            }
        }
    }
}
