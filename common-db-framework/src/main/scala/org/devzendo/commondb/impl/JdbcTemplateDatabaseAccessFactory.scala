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

package org.devzendo.commondb.impl

import org.springframework.jdbc.core.simple.SimpleJdbcTemplate
import java.io.File
import javax.sql.DataSource
import java.sql.SQLException
import org.springframework.dao.{DataAccessResourceFailureException, DataAccessException}
import collection.mutable.ListBuffer
import org.springframework.jdbc.datasource.SingleConnectionDataSource
import org.h2.constant.ErrorCode
import scala.throws
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.TransactionStatus
import org.h2.engine.ExistenceChecker
import org.devzendo.commondb._
import dao.CodeVersion
import dao.SchemaVersion
import scala.Some
import org.devzendo.commondb.Password

class JdbcTemplateDatabaseAccessFactory[U <: UserDatabaseAccess] extends DatabaseAccessFactory[U] {
    private[this] val CREATION_DDL_STRINGS = List[String](
        "CREATE TABLE Versions(entity VARCHAR(40), version VARCHAR(40))",
        "CREATE SEQUENCE Sequence START WITH 0 INCREMENT BY 1"
    )

    def exists(databasePath: File, databaseName: String): Boolean = {
        val pathToDatabaseFile = new File(databasePath, databaseName)
        //Database.exists(pathToDatabaseFile.getAbsolutePath)
        ExistenceChecker.exists(pathToDatabaseFile.getAbsolutePath)
    }

    def create(
            databasePath: File,
            databaseName: String,
            password: Option[Password],
            codeVersion: CodeVersion,
            schemaVersion: SchemaVersion,
            workflowAdapter: Option[CreateWorkflowAdapter],
            userDatabaseCreator: Option[UserDatabaseCreator],
            userDatabaseAccessFactory: Option[Function1[DatabaseAccess[U], U]]): Option[DatabaseAccess[U]] = {

        val adapter = new LoggingDecoratorCreateWorkflowAdapter(workflowAdapter)
        val creator = new LoggingDecoratorUserDatabaseCreator(userDatabaseCreator)

        adapter.startCreating()
        DatabaseAccessFactory.LOGGER.info("Creating database '" + databaseName + "' at path '" + databasePath + "'")
        adapter.reportProgress(CreateProgressStage.Creating, "Starting to create '" + databaseName + "'")
        val details = accessDatabase(databasePath, databaseName, password, allowCreate = true)

        // The access is created incomplete, then filled in with the user access,
        // since the user access may need to use the rest of the access object.
        val access: DatabaseAccess[U] = JdbcTemplateDatabaseAccess[U](databasePath, databaseName, details._1, details._2)

        createTables(access, adapter)
        creator.createApplicationTables(access)

        populateTables(access, adapter, codeVersion, schemaVersion)
        creator.populateApplicationTables(access)

        for (userFactory <- userDatabaseAccessFactory) {
            access.user = Some(userFactory.apply(access))
        }

        adapter.reportProgress(CreateProgressStage.Created, "Created '" + databaseName + "'")
        adapter.stopCreating()
        Some(access)
    }

    private[this] def createTables(access: DatabaseAccess[U], adapter: CreateWorkflowAdapter) {
        adapter.reportProgress(CreateProgressStage.CreatingTables, "Creating tables")
        CREATION_DDL_STRINGS.foreach((ddl) => {
            access.jdbcTemplate.getJdbcOperations.execute(ddl)
        })
    }

    private[this] def populateTables(access: DatabaseAccess[U], adapter: CreateWorkflowAdapter, codeVersion: CodeVersion, schemaVersion: SchemaVersion) {
        adapter.reportProgress(CreateProgressStage.PopulatingTables, "Populating tables")
        access.versionsDao.persistVersion(schemaVersion)
        access.versionsDao.persistVersion(codeVersion)
    }

    private[this] def migrate(databaseName: String, access: DatabaseAccess[U], adapter: OpenWorkflowAdapter, migrator: UserDatabaseMigrator, currentSchemaVersion: SchemaVersion, codeVersion: CodeVersion, schemaVersion: SchemaVersion): Boolean = {
        adapter.reportProgress(OpenProgressStage.MigrationRequired, "Database '" + databaseName + "' requires migration")
        if (adapter.requestMigration()) {
            adapter.reportProgress(OpenProgressStage.Migrating, "Migrating database '" + databaseName + "'")
            try {
                val transactionTemplate = access.createTransactionTemplate
                transactionTemplate.execute(new TransactionCallback[Unit]() {
                    def doInTransaction(status: TransactionStatus) {
                        migrator.migrateSchema(access, currentSchemaVersion)
                        adapter.migrationSucceeded()
                        access.versionsDao.persistVersion(schemaVersion)
                        // Template now commits transaction
                    }
                })
            } catch {
                case dae: DataAccessException =>
                    // Template now rolls back transaction
                    DatabaseAccessFactory.LOGGER.error("Migration failed: " + dae.getMessage, dae)
                    adapter.reportProgress(OpenProgressStage.MigrationFailed, "Migration of database '" + databaseName + "' failed: " + dae.getMessage)
                    adapter.migrationFailed(dae)
                    DatabaseAccessFactory.LOGGER.info("Migration failed; closing")
                    return false
            }
            true
        } else {
            adapter.reportProgress(OpenProgressStage.MigrationCancelled, "Migration of database '" + databaseName + "' cancelled")
            adapter.migrationCancelled()
            DatabaseAccessFactory.LOGGER.info("Migration cancelled; closing")
            false
        }
    }

    def open(
            databasePath: File,
            databaseName: String,
            password: Option[Password],
            codeVersion: CodeVersion,
            schemaVersion: SchemaVersion,
            workflowAdapter: Option[OpenWorkflowAdapter],
            userDatabaseMigrator: Option[UserDatabaseMigrator],
            userDatabaseAccessFactory: Option[Function1[DatabaseAccess[U], U]]): Option[DatabaseAccess[U]] = {

        val adapter = new LoggingDecoratorOpenWorkflowAdapter(workflowAdapter)
        val migrator = new LoggingDecoratorUserDatabaseMigrator(userDatabaseMigrator)

        adapter.startOpening()
        DatabaseAccessFactory.LOGGER.info("Opening database '" + databaseName + "' from path '" + databasePath + "'")
        adapter.reportProgress(OpenProgressStage.OpenStarting, "Starting to open '" + databaseName + "'")

        // Try at first with the supplied password - if we get a BadPasswordException,
        // prompt for password and retry.
        var tryingToOpenMessage = "Opening database '" + databaseName + "'"
        var passwordAttempt = password
        while (true) {
            try {
                adapter.reportProgress(OpenProgressStage.Opening, tryingToOpenMessage)

                val details =
                    accessDatabase(databasePath, databaseName, passwordAttempt, allowCreate = false)

                adapter.reportProgress(OpenProgressStage.Opened, "Opened database '" + databaseName + "'")

                // The access is created incomplete, then filled in with the user access,
                // since the user access may need to use the rest of the access object.
                val access: DatabaseAccess[U] = JdbcTemplateDatabaseAccess(databasePath, databaseName, details._1, details._2)
                for (userFactory <- userDatabaseAccessFactory) {
                    access.user = Some(userFactory.apply(access))
                }

                val currentSchemaVersion = access.versionsDao.findVersion(classOf[SchemaVersion]).get
                DatabaseAccessFactory.LOGGER.info("Schema version in database: " + currentSchemaVersion)
                DatabaseAccessFactory.LOGGER.info("Current application schema version: " + schemaVersion)
                currentSchemaVersion.compareTo(schemaVersion) match {
                    case 1 => // opened future database
                        DatabaseAccessFactory.LOGGER.warn("This database is from the future!")
                        adapter.reportProgress(OpenProgressStage.MigrationNotPossible, "Database '" + databaseName + "' has a more modern schema than this application supports")
                        adapter.migrationNotPossible()
                        adapter.stopOpening()
                        access.close()
                        return None
                    case -1 => // opened old database, so migrate it if request succeeds
                        DatabaseAccessFactory.LOGGER.info("This database has an older schema version")
                        if (!migrate(databaseName, access, adapter, migrator, currentSchemaVersion, codeVersion, schemaVersion)) {
                            adapter.stopOpening()
                            access.close()
                            return None
                        }
                    case 0 => // database is same version as current schema
                        DatabaseAccessFactory.LOGGER.info("This database has the current schema version")
                }
                adapter.stopOpening()

                return Some(access)

            } catch {

                case bad: BadPasswordException =>
                    DatabaseAccessFactory.LOGGER.warn("Bad password: " + bad.getMessage)
                    adapter.reportProgress(OpenProgressStage.PasswordRequired, "Password required for '" + databaseName + "'")
                    val thisAttempt = adapter.requestPassword()
                    passwordAttempt = thisAttempt
                    passwordAttempt match {
                        case None =>
                            DatabaseAccessFactory.LOGGER.info("Open of encrypted database cancelled")
                            adapter.reportProgress(OpenProgressStage.PasswordCancelled, "Open of '" + databaseName + "' cancelled")
                            adapter.stopOpening()
                            return None
                        case _ =>
                            // Change the progress message, second time round...
                            tryingToOpenMessage = "Trying to open database '" + databaseName + "'"
                    }

                case darfe: DataAccessResourceFailureException =>
                    DatabaseAccessFactory.LOGGER.warn("Could not open database: " + darfe.getMessage)
                    adapter.reportProgress(OpenProgressStage.NotPresent, "Database '" + databaseName + "' not found")
                    adapter.databaseNotFound(darfe)
                    adapter.stopOpening()
                    return None

                case dae: DataAccessException =>
                    DatabaseAccessFactory.LOGGER.warn("Data access exception opening database: " + dae.getMessage, dae)
                    adapter.reportProgress(OpenProgressStage.OpenFailed, "Open of '" + databaseName + "' failed")
                    adapter.seriousProblemOccurred(dae)
                    adapter.stopOpening()
                    return None
            }
        }
        None
    }

    private[this] def accessDatabase(
            databasePath: File,
            databaseName: String,
            password: Option[Password],
            allowCreate: Boolean): (DataSource, SimpleJdbcTemplate) = {
        DatabaseAccessFactory.LOGGER.info("Opening database '" + databaseName + "' at '" + databasePath + "'")
        DatabaseAccessFactory.LOGGER.debug("Validating arguments")
        if (databasePath == null) {
            throw new DataAccessResourceFailureException("Null database path")
        }
        val mDbPath = new File(databasePath, databaseName).getAbsolutePath.trim()
        if (mDbPath.length() == 0) {
            throw new DataAccessResourceFailureException(String.format("Incorrect database path '%s'", databasePath))
        }
        val mDbPassword = password match {
            case None => ""
            case Some(p) => p.toRepresentation
        }
        val dbURLParts = ListBuffer.empty[String]
        dbURLParts.append(mDbPassword.length() match {
            case 0 => String.format("jdbc:h2:%s", mDbPath)
            case _ => String.format("jdbc:h2:%s;CIPHER=AES", mDbPath)
        })
        if (!allowCreate) {
            dbURLParts.append(";IFEXISTS=TRUE")
        }
        val dbURL = dbURLParts.mkString
        DatabaseAccessFactory.LOGGER.debug("DB URL is " + dbURL)
        val driverClassName = "org.h2.Driver"
        val userName = "sa"
        val suppressClose = false
        DatabaseAccessFactory.LOGGER.debug("Obtaining data source bean")
        //noinspection deprecation
        val dataSource = new SingleConnectionDataSource(driverClassName,
            dbURL, userName, mDbPassword + " userpwd", suppressClose)
        DatabaseAccessFactory.LOGGER.debug("DataSource is " + dataSource)

        DatabaseAccessFactory.LOGGER.debug("Obtaining SimpleJdbcTemplate")
        val jdbcTemplate = new SimpleJdbcTemplate(dataSource)
        DatabaseAccessFactory.LOGGER.debug("Database setup done")

        // Possible Spring bug: if the database isn't there, it doesn't throw
        // an (unchecked) exception. - it does detect it and logs voluminously,
        // but then doesn't pass the error on to me.
        // Looks like a 90013 (DATABASE_NOT_FOUND_1) isn't mapped by the default
        // Spring sql-error-codes.xml.
        // So, I have to check myself. (Obviating one of the reasons I chose Spring!)
        try {
            // This'll throw if the db doesn't exist.
            val closed = dataSource.getConnection.isClosed
            DatabaseAccessFactory.LOGGER.debug("db is initially closed? " + closed)
            (dataSource, jdbcTemplate)
        } catch {
            case e: SQLException =>
                e.getErrorCode match {
                    case ErrorCode.DATABASE_NOT_FOUND_1 =>
                        val dbnfMessage = String.format("Database at %s not found", databasePath)
                        DatabaseAccessFactory.LOGGER.warn(dbnfMessage)
                        throw new DataAccessResourceFailureException(dbnfMessage)
                    case ErrorCode.FILE_ENCRYPTION_ERROR_1 =>
                        val feeMessage = String.format("Bad password opening database at %s", databasePath)
                        DatabaseAccessFactory.LOGGER.warn(feeMessage)
                        throw new BadPasswordException(feeMessage)
                    case _ =>
                        val exMessage = "Could not open database - SQL Error Code " + e.getErrorCode
                        DatabaseAccessFactory.LOGGER.warn("SQLException from isClosed", e)
                        // Assume that anything that goes wrong here is bad...
                        throw new org.springframework.jdbc.UncategorizedSQLException(exMessage, "", e)
                }
        }
    }

    /**
     * A BadPasswordException is thrown when an attempt is made to access
     * an encrypted database for migration but the wrong password is
     * supplied.
     *
     */
    private class BadPasswordException(message: String) extends RuntimeException(message) {

    }

    /**
     * A CreateWorkflowAdapter that decorates an existing CreateWorkflowAdapter,
     * logging all calls made prior to passing them on to the decorated
     * CreateWorkflowAdapter.
     *
     */
    private class LoggingDecoratorUserDatabaseCreator(creator: Option[UserDatabaseCreator]) extends UserDatabaseCreator {

        def createApplicationTables(access: DatabaseAccess[_]) {
            DatabaseAccessFactory.LOGGER.info("Creating application tables")
            for (c <- creator) {
                c.createApplicationTables(access)
            }
            DatabaseAccessFactory.LOGGER.info("Created application tables")
        }

        def populateApplicationTables(access: DatabaseAccess[_]) {
            DatabaseAccessFactory.LOGGER.info("Populating application tables")
            for (c <- creator) {
                c.populateApplicationTables(access)
            }
            DatabaseAccessFactory.LOGGER.info("Populated application tables")
        }
    }


    /**
     * A CreateWorkflowAdapter that decorates an existing CreateWorkflowAdapter,
     * logging all calls made prior to passing them on to the decorated
     * CreateWorkflowAdapter.
     *
     */
    private class LoggingDecoratorCreateWorkflowAdapter(adapter: Option[CreateWorkflowAdapter]) extends CreateWorkflowAdapter {

        def startCreating() {
            DatabaseAccessFactory.LOGGER.info("Start creating")
            for (a <- adapter) {
                a.startCreating()
            }
        }

        def reportProgress(progressStage: CreateProgressStage.Enum, description: String) {
            DatabaseAccessFactory.LOGGER.info("Progress: " + progressStage + ": " + description)
            for (a <- adapter) {
                a.reportProgress(progressStage, description)
            }
        }

        def seriousProblemOccurred(exception: DataAccessException) {
            DatabaseAccessFactory.LOGGER.warn("Serious problem occurred: " + exception.getMessage)
            for (a <- adapter) {
                a.seriousProblemOccurred(exception)
            }
        }

        def stopCreating() {
            DatabaseAccessFactory.LOGGER.info("Stop creating")
            for (a <- adapter) {
                a.stopCreating()
            }
        }
    }

    /**
     * A OpenWorkflowAdapter that decorates an existing OpenWorkflowAdapter,
     * logging all calls made prior to passing them on to the decorated
     * OpenWorkflowAdapter.
     *
     */
    private class LoggingDecoratorOpenWorkflowAdapter(adapter: Option[OpenWorkflowAdapter]) extends OpenWorkflowAdapter {
        def startOpening() {
            DatabaseAccessFactory.LOGGER.info("Start opening")
            for (a <- adapter) {
                a.startOpening()
            }
        }

        def reportProgress(progressStage: OpenProgressStage.Enum, description: String) {
            DatabaseAccessFactory.LOGGER.info("Progress: " + progressStage + ": " + description)
            for (a <- adapter) {
                a.reportProgress(progressStage, description)
            }
        }

        def requestPassword(): Option[Password] = {
            DatabaseAccessFactory.LOGGER.info("Requesting password")
            val requestedPassword = adapter.flatMap(a => Option[Option[Password]] {
                a.requestPassword()
            }).getOrElse(None)

            DatabaseAccessFactory.LOGGER.info("Password provided")
            requestedPassword
        }

        def requestMigration() = {
            DatabaseAccessFactory.LOGGER.info("Requesting migration")
            val requestedMigration = adapter.flatMap(a => Option[Boolean] {
                a.requestMigration()
            }).getOrElse(false)
            DatabaseAccessFactory.LOGGER.info("Result of migration request: " + requestedMigration)
            requestedMigration
        }

        def migrationSucceeded() {
            DatabaseAccessFactory.LOGGER.info("Migration succeeded")
            for (a <- adapter) {
                a.migrationSucceeded()
            }
        }

        def migrationCancelled() {
            DatabaseAccessFactory.LOGGER.info("Migration cancelled")
            for (a <- adapter) {
                a.migrationCancelled()
            }
        }

        def migrationNotPossible() {
            DatabaseAccessFactory.LOGGER.warn("Migration not possible")
            for (a <- adapter) {
                a.migrationNotPossible()
            }
        }

        def migrationFailed(exception: DataAccessException) {
            DatabaseAccessFactory.LOGGER.warn("Migration failure: " + exception.getMessage)
            for (a <- adapter) {
                a.migrationFailed(exception)
            }
        }

        def databaseNotFound(exception: DataAccessResourceFailureException) {
            DatabaseAccessFactory.LOGGER.warn("Database not found: " + exception.getMessage)
            for (a <- adapter) {
                a.databaseNotFound(exception)
            }
        }

        def seriousProblemOccurred(exception: DataAccessException) {
            DatabaseAccessFactory.LOGGER.warn("Serious problem occurred: " + exception.getMessage)
            for (a <- adapter) {
                a.seriousProblemOccurred(exception)
            }
        }

        def stopOpening() {
            DatabaseAccessFactory.LOGGER.info("Stop opening")
            for (a <- adapter) {
                a.stopOpening()
            }
        }
    }

    /**
     * A UserDatabaseMigrator that decorates an existing UserDatabaseMigrator,
     * logging all calls made prior to passing them on to the decorated
     * UserDatabaseMigrator.
     *
     */
    private class LoggingDecoratorUserDatabaseMigrator(migrator: Option[UserDatabaseMigrator]) extends UserDatabaseMigrator {
        @throws(classOf[DataAccessException])
        def migrateSchema(access: DatabaseAccess[_],
                          currentSchemaVersion: SchemaVersion) {
            DatabaseAccessFactory.LOGGER.info("Migrating from schema version '"
                + currentSchemaVersion + "' to latest version")
            try {
                for (m <- migrator) {
                    m.migrateSchema(access, currentSchemaVersion)
                }
                DatabaseAccessFactory.LOGGER.info("Migration succeeded")
            } catch {
                case e: DataAccessException =>
                    DatabaseAccessFactory.LOGGER.error("Migration failed: " + e.getMessage, e)
                    throw e
            }
        }
    }
}
