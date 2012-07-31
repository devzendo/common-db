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

import dao.{CodeVersion, SchemaVersion}

abstract class AbstractDatabaseMigrationUnittest extends AbstractTempFolderUnittest with AutoCloseDatabaseUnittest {
    val oldCodeVersion = CodeVersion("1.0")
    val oldSchemaVersion = SchemaVersion("0.4")

    val newCodeVersion = CodeVersion("1.1")
    val newSchemaVersion = SchemaVersion("0.5")

    protected[this] def createOldDatabase(databaseName: String): Option[DatabaseAccess[_]] = {
        databaseAccessFactory.create(temporaryDirectory, databaseName, None, oldCodeVersion, oldSchemaVersion, None, None, None)
    }

    protected[this] def createNewDatabase(databaseName: String): Option[DatabaseAccess[_]] = {
        databaseAccessFactory.create(temporaryDirectory, databaseName, None, newCodeVersion, newSchemaVersion, None, None, None)
    }

    protected[this] def openNewDatabase(databaseName: String, openerAdapter: OpenWorkflowAdapter, userDatabaseMigrator: UserDatabaseMigrator): Option[DatabaseAccess[_]] = {
        // It isn't possible to have a newer schema with the same version of
        // code, but I don't want to trigger the code updated progress messages
        // in this test.
        databaseAccessFactory.open(temporaryDirectory, databaseName, None, oldCodeVersion, newSchemaVersion, Some(openerAdapter), Some(userDatabaseMigrator), None)
    }

    protected[this] def openOldDatabase(databaseName: String): Option[DatabaseAccess[_]] = {
        databaseAccessFactory.open(temporaryDirectory, databaseName, None, oldCodeVersion, oldSchemaVersion, None, None, None)
    }

    protected[this] def openOldDatabase(databaseName: String, openerAdapter: OpenWorkflowAdapter, userDatabaseMigrator: UserDatabaseMigrator): Option[DatabaseAccess[_]] = {
        databaseAccessFactory.open(temporaryDirectory, databaseName, None, oldCodeVersion, oldSchemaVersion, Some(openerAdapter), Some(userDatabaseMigrator), None)
    }
}
