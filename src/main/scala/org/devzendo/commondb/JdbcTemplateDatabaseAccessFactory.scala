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

import org.springframework.jdbc.core.simple.{ParameterizedRowMapper, SimpleJdbcTemplate}
import java.io.File
import javax.sql.DataSource
import org.springframework.jdbc.CannotGetJdbcConnectionException
import java.sql.{SQLException, ResultSet}
import org.springframework.dao.{DataAccessResourceFailureException, EmptyResultDataAccessException, DataAccessException}
import collection.mutable.ListBuffer
import org.springframework.jdbc.datasource.{SingleConnectionDataSource, DataSourceUtils}
import org.h2.constant.ErrorCode
import org.devzendo.commoncode.string.StringUtils
import scala.throws

private class JdbcTemplateVersionsDao(jdbcTemplate: SimpleJdbcTemplate) extends VersionsDao {

    @throws(classOf[DataAccessException])
    def findVersion[V <: Version](versionType: Class[V]): Option[V] = {

        val sql = "SELECT version FROM Versions WHERE entity = ?"
        val mapper: ParameterizedRowMapper[V] = new ParameterizedRowMapper[V]() {
            // notice the return type with respect to Java 5 covariant return types
            def mapRow(rs: ResultSet, rowNum: Int) = {
                val ctor = versionType.getConstructor(classOf[String])
                ctor.newInstance(rs.getString("version"))
            }
        }
        //noinspection deprecation
        try {
            Some(jdbcTemplate.queryForObject(sql, mapper, versionType.getSimpleName))
        } catch {
            case e: EmptyResultDataAccessException => None
        }
    }

    @throws(classOf[DataAccessException])
    def persistVersion[V <: Version](version: V) {
        val count = jdbcTemplate.queryForInt(
            "SELECT COUNT(0) FROM Versions WHERE entity = ?",
            version.getClass.getSimpleName)
        if (count == 0) {
            jdbcTemplate.update(
                "INSERT INTO Versions (entity, version) VALUES (?, ?)",
                version.getClass.getSimpleName, version.toRepresentation)
        } else {
            jdbcTemplate.update(
                "UPDATE Versions SET version = ? WHERE entity = ?",
                version.toRepresentation, version.getClass.getSimpleName)
        }
    }
}

private class JdbcTemplateSequenceDao(jdbcTemplate: SimpleJdbcTemplate) extends SequenceDao {
    @throws(classOf[DataAccessException])
    def nextSequence: Long = {
        jdbcTemplate.queryForLong("SELECT NEXT VALUE FOR SEQUENCE Sequence")
    }
}

sealed case class JdbcTemplateDatabaseAccess[U <: UserDatabaseAccess](
        override val databasePath: File,
        override val databaseName: String,
        override val dataSource: DataSource,
        override val jdbcTemplate: SimpleJdbcTemplate) extends DatabaseAccess[U](databasePath, databaseName, dataSource, jdbcTemplate) {
    private[this] var closed: Boolean = false
    val versionsDao: VersionsDao = new JdbcTemplateVersionsDao(jdbcTemplate)
    val sequenceDao: SequenceDao = new JdbcTemplateSequenceDao(jdbcTemplate)

    def close() {
        if (closed) {
            DatabaseAccessFactory.LOGGER.info("Database '" + databaseName + "' at '" + databasePath + "' is already closed")
            return
        }

        // TODO callDatabaseClosingFacades();
        try {
            DatabaseAccessFactory.LOGGER.info("Closing database '" + databaseName + "' at '" + databasePath + "'")
            DataSourceUtils.getConnection(dataSource).close()
            DatabaseAccessFactory.LOGGER.info("Closed database '" + databaseName + "' at '" + databasePath + "'")
            closed = true
        } catch {
            case e: CannotGetJdbcConnectionException =>
                DatabaseAccessFactory.LOGGER.warn("Can't get JDBC Connection on close: " + e.getMessage, e)
            case s: SQLException =>
                DatabaseAccessFactory.LOGGER.warn("SQL Exception on close: " + s.getMessage, s)
        }
    }

    def isClosed: Boolean = {
        if (closed)
            return true
        try {
            return DataSourceUtils.getConnection(dataSource).isClosed
        }
        catch {
            case e: CannotGetJdbcConnectionException => {
                DatabaseAccessFactory.LOGGER.warn("Can't get JDBC Connection on isClosed: " + e.getMessage, e)
            }
            case e: SQLException => {
                DatabaseAccessFactory.LOGGER.warn("SQL Exception on isClosed: " + e.getMessage, e)
            }
        }
        false
    }
}

class JdbcTemplateDatabaseAccessFactory[U <: UserDatabaseAccess] extends DatabaseAccessFactory[U] {
    private[this] val CREATION_DDL_STRINGS = List[String](
        "CREATE TABLE Versions(entity VARCHAR(40), version VARCHAR(40))",
        "CREATE SEQUENCE Sequence START WITH 0 INCREMENT BY 1"
    )

    def create(
                  databasePath: File,
                  databaseName: String,
                  password: Option[String],
                  codeVersion: CodeVersion,
                  schemaVersion: SchemaVersion,
                  workflowAdapter: Option[CreateWorkflowAdapter],
                  userDatabaseAccessFactory: Option[Function1[DatabaseAccess[U], U]]): Option[DatabaseAccess[U]] = {

        val adapter = new LoggingDecoratorCreateWorkflowAdapter(workflowAdapter)
        adapter.startCreating()
        DatabaseAccessFactory.LOGGER.info("Creating database '" + databaseName + "' at path '" + databasePath + "'")
        adapter.reportProgress(CreateProgressStage.Creating, "Starting to create '" + databaseName + "'")
        val details = accessDatabase(databasePath, databaseName, password, allowCreate = true)

        // The access is created incomplete, then filled in with the user access,
        // since the user access may need to use the rest of the access object.
        val access: DatabaseAccess[U] = JdbcTemplateDatabaseAccess[U](databasePath, databaseName, details._1, details._2)
        for (userFactory <- userDatabaseAccessFactory) {
            access.user = Some(userFactory.apply(access))
        }
        createTables(access, adapter, details._1, details._2)
        populateTables(access, adapter, details._1, details._2, codeVersion, schemaVersion)
        adapter.reportProgress(CreateProgressStage.Created, "Created '" + databaseName + "'")
        adapter.stopCreating()
        Some(access)
    }

    private[this] def createTables(access: DatabaseAccess[U], adapter: CreateWorkflowAdapter, dataSource: DataSource, jdbcTemplate: SimpleJdbcTemplate) {
        adapter.reportProgress(CreateProgressStage.CreatingTables, "Creating tables")
        CREATION_DDL_STRINGS.foreach( (ddl) => {
            jdbcTemplate.getJdbcOperations.execute(ddl)
        })
        // TODO call back into the caller to create its own tables
    }

    private[this] def populateTables(access: DatabaseAccess[U], adapter: CreateWorkflowAdapter, dataSource: DataSource, jdbcTemplate: SimpleJdbcTemplate, codeVersion: CodeVersion, schemaVersion: SchemaVersion) {
        adapter.reportProgress(CreateProgressStage.PopulatingTables, "Populating tables")
        access.versionsDao.persistVersion(schemaVersion)
        access.versionsDao.persistVersion(codeVersion)
    }

    def open(
                databasePath: File,
                databaseName: String,
                password: Option[String],
                codeVersion: CodeVersion,
                schemaVersion: SchemaVersion,
                workflowAdapter: Option[OpenWorkflowAdapter]): Option[DatabaseAccess[U]] = {

        val adapter = new LoggingDecoratorOpenWorkflowAdapter(workflowAdapter)

        adapter.startOpening()
        DatabaseAccessFactory.LOGGER.info("Opening database '" + databaseName + "' from path '" + databasePath + "'")
        adapter.reportProgress(OpenProgressStage.OpenStarting, "Starting to open '" + databaseName + "'")

        // Try at first with the supplied password - if we get a BadPasswordException,
        // prompt for password and retry.
        var tryingToOpenMessage = "Opening database '" + databaseName + "'"
        var passwordAttempt = password
        //while (true) {
        try {
            adapter.reportProgress(OpenProgressStage.Opening, tryingToOpenMessage)

            val details =
                accessDatabase(databasePath, databaseName, passwordAttempt, allowCreate = false)
            // TODO check for other application?
            // TODO migration...

            adapter.reportProgress(OpenProgressStage.Opened, "Opened database '" + databaseName + "'")
            adapter.stopOpening()
            return Some(JdbcTemplateDatabaseAccess(databasePath, databaseName, details._1, details._2))

        } catch {
            //
            //                case bad: BadPasswordException =>
            //                    DatabaseAccessFactory.LOGGER.warn("Bad password: " + bad.getMessage)
            //                    adapter.reportProgress(OpenProgressStage.PasswordRequired, "Password required for '" + databaseName + "'")
            //                    val thisAttempt = adapter.requestPassword()
            //                    passwordAttempt = thisAttempt
            //                    passwordAttempt match {
            //                        case None =>
            //                            DatabaseAccessFactory.LOGGER.info("Open of encrypted database cancelled")
            //                            adapter.reportProgress(OpenProgressStage.PasswordCancelled, "Open of '" + databaseName + "' cancelled")
            //                            adapter.stopOpening()
            //                            return None
            //                        case _ =>
            //                            // Change the progress message, second time round...
            //                            tryingToOpenMessage = "Trying to open database '" + databaseName + "'"
            //                    }
            //
            case darfe: DataAccessResourceFailureException =>
                DatabaseAccessFactory.LOGGER.warn("Could not open database: " + darfe.getMessage)
                adapter.reportProgress(OpenProgressStage.NotPresent, "Database '" + databaseName + "' not found")
                adapter.databaseNotFound(darfe)
                adapter.stopOpening()
                return None
            //
            //                case dae: DataAccessException =>
            //                    DatabaseAccessFactory.LOGGER.warn("Data access exception opening database: " + dae.getMessage, dae)
            //                    adapter.reportProgress(OpenProgressStage.OpenFailed, "Open of '" + databaseName + "' failed")
            //                    adapter.seriousProblemOccurred(dae)
            //                    adapter.stopOpening()
            //                    return None
        }
        //        }
        None
    }

    private[this] def accessDatabase(
                                        databasePath: File,
                                        databaseName: String,
                                        password: Option[String],
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
            case Some(p) => p
        }
        val dbURLParts = ListBuffer.empty[String]
        dbURLParts.append(mDbPassword.length() match {
            case 0 => String.format("jdbc:h2:%s", mDbPath)
            case _ => String.format("jdbc:h2:%s;CIPHER=AES", mDbPath)
        })
        if (!allowCreate) {
            dbURLParts.append(";IFEXISTS=TRUE")
        }
        dbURLParts.append(";PAGE_STORE=FALSE")
        // h2 1.2.128 is the last release
        // that keeps the old format; 1.2.129 made the PAGE_STORE format
        // the only one - but we want to keep the old format by default
        // for now.
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
                        DatabaseAccessFactory.LOGGER.debug(dbnfMessage)
                        throw new DataAccessResourceFailureException(dbnfMessage)
                    case ErrorCode.FILE_ENCRYPTION_ERROR_1 =>
                        val feeMessage = String.format("Bad password opening database at %s", databasePath)
                        DatabaseAccessFactory.LOGGER.debug(feeMessage)
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

        def requestApplicationCodeVersion() = {
            DatabaseAccessFactory.LOGGER.info("Requesting application code version")
            val requestedVersion = adapter.flatMap(a => Option[String] {
                a.requestApplicationCodeVersion()
            }).getOrElse("")
            DatabaseAccessFactory.LOGGER.info("Application code is at version '" + requestedVersion + "'")
            requestedVersion
        }

        def requestApplicationSchemaVersion() = {
            DatabaseAccessFactory.LOGGER.info("Requesting application schema version")
            val requestedVersion = adapter.flatMap(a => Option[String] {
                a.requestApplicationSchemaVersion()
            }).getOrElse("")
            DatabaseAccessFactory.LOGGER.info("Application schema is at version '" + requestedVersion + "'")
            requestedVersion
        }

        def createApplicationTables(dataSource: DataSource, jdbcTemplate: SimpleJdbcTemplate) {
            DatabaseAccessFactory.LOGGER.info("Creating application tables")
            for (a <- adapter) {
                a.createApplicationTables(dataSource, jdbcTemplate)
            }
            DatabaseAccessFactory.LOGGER.info("Created application tables")
        }

        def populateApplicationTables(dataSource: DataSource, jdbcTemplate: SimpleJdbcTemplate) {
            DatabaseAccessFactory.LOGGER.info("Populating application tables")
            for (a <- adapter) {
                a.populateApplicationTables(dataSource, jdbcTemplate)
            }
            DatabaseAccessFactory.LOGGER.info("Populated application tables")
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

        def requestPassword(): Option[String] = {
            DatabaseAccessFactory.LOGGER.info("Requesting password")
            val requestedPassword = adapter.flatMap(a => Option[Option[String]] {
                a.requestPassword()
            }).getOrElse(None)

            DatabaseAccessFactory.LOGGER.info("Result of password request: '" + StringUtils.maskSensitiveText(requestedPassword.getOrElse("")) + "'")
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

        def createdByOtherApplication() {
            DatabaseAccessFactory.LOGGER.warn("Could not open database created by another application")
            for (a <- adapter) {
                a.createdByOtherApplication()
            }
        }

        def noApplicationDetailsAvailable() {
            DatabaseAccessFactory.LOGGER.warn("No application details available; cannot check which application created this database")
            for (a <- adapter) {
                a.noApplicationDetailsAvailable()
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
}