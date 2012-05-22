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

import java.io.File
import org.apache.log4j.Logger
import collection.mutable.ListBuffer
import org.springframework.jdbc.datasource.SingleConnectionDataSource
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate
import javax.sql.DataSource
import org.springframework.dao.{DataAccessException, DataAccessResourceFailureException}
import org.devzendo.commoncode.string.StringUtils

import java.sql.SQLException
import org.h2.constant.ErrorCode

object DatabaseAccessFactory {
    private val LOGGER = Logger.getLogger(classOf[DatabaseAccessFactory])
}

sealed case class DatabaseAccess(dataSource: DataSource, jdbcTemplate: SimpleJdbcTemplate) {

}
class DatabaseAccessFactory {

    def open(
        databasePath: File,
        databaseName: String,
        password: Option[String],
        workflowAdapter: Option[OpenWorkflowAdapter]): Option[DatabaseAccess] = {

        val adapter = new LoggingDecoratorOpenWorkflowAdapter(workflowAdapter)

        adapter.startOpening()
        DatabaseAccessFactory.LOGGER.info("Opening database '" + databaseName + "' from path '" + databasePath + "'");
        adapter.reportProgress(Starting, "Starting to open '" + databaseName + "'");

        // Try at first with the supplied password - if we get a BadPasswordException,
        // prompt for password and retry.
        var tryingToOpenMessage = "Opening database '" + databaseName + "'";
        var passwordAttempt = password
        //while (true) {
            try {
                adapter.reportProgress(Opening, tryingToOpenMessage)

                val details = accessDatabase(databasePath, databaseName, passwordAttempt, false)
//                Some(DatabaseAccess(details._1, details._2))
            } catch {
//
//                case bad: BadPasswordException =>
//                    DatabaseAccessFactory.LOGGER.warn("Bad password: " + bad.getMessage)
//                    adapter.reportProgress(PasswordRequired, "Password required for '" + databaseName + "'")
//                    val thisAttempt = adapter.requestPassword()
//                    passwordAttempt = thisAttempt
//                    passwordAttempt match {
//                        case None =>
//                            DatabaseAccessFactory.LOGGER.info("Open of encrypted database cancelled")
//                            adapter.reportProgress(PasswordCancelled, "Open of '" + databaseName + "' cancelled")
//                            adapter.stopOpening()
//                            return None
//                        case _ =>
//                            // Change the progress message, second time round...
//                            tryingToOpenMessage = "Trying to open database '" + databaseName + "'"
//                    }
//
                case darfe: DataAccessResourceFailureException =>
                    DatabaseAccessFactory.LOGGER.warn("Could not open database: " + darfe.getMessage)
                    adapter.reportProgress(NotPresent, "Database '" + databaseName + "' not found")
                    adapter.databaseNotFound(darfe)
                    adapter.stopOpening()
                    return None
//
//                case dae: DataAccessException =>
//                    DatabaseAccessFactory.LOGGER.warn("Data access exception opening database: " + dae.getMessage, dae)
//                    adapter.reportProgress(OpenFailed, "Open of '" + databaseName + "' failed")
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
            allowCreate: Boolean): Tuple2[DataSource, SimpleJdbcTemplate] = {
        DatabaseAccessFactory.LOGGER.info("Opening database '" + databaseName + "' at '" + databasePath + "'");
        DatabaseAccessFactory.LOGGER.debug("Validating arguments");
        if (databasePath == null) {
            throw new DataAccessResourceFailureException("Null database path")
        }
        val mDbPath = databasePath.getAbsolutePath.trim()
        if (mDbPath.length() == 0) {
            throw new DataAccessResourceFailureException(String.format("Incorrect database path '%s'", databasePath));
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
        DatabaseAccessFactory.LOGGER.debug("Obtaining data source bean")
        val dbURL = dbURLParts.mkString
        DatabaseAccessFactory.LOGGER.debug("DB URL is " + dbURL)
        val driverClassName = "org.h2.Driver"
        val userName = "sa"
        val suppressClose = false
        //noinspection deprecation
        val dataSource = new SingleConnectionDataSource(driverClassName,
            dbURL, userName, mDbPassword + " userpwd", suppressClose);
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

        def reportProgress(progressStage: ProgressStage, description: String) {
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
            DatabaseAccessFactory.LOGGER.warn("No application details available; cannot check which application created this database");
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
