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

import org.springframework.dao.{DataAccessResourceFailureException, DataAccessException}

object OpenProgressStage {

    sealed trait Enum {
        def index: Int

        /**
         * How many steps are there in total?
         * @return the maximum value of a ProgressStage
         */
        def maximumStages = 6
    }

    /**
     * The open operation is starting. Sent almost immediately to give some
     * immediate feedback.
     */
    case object OpenStarting extends Enum {
        val index = 0
    }

    /**
     * Sent immediately prior to opening the database.
     */
    case object Opening extends Enum {
        val index = 1
    }

    /**
     * Sent before the password is requested from the adapter.
     */
    case object PasswordRequired extends Enum {
        val index = 2
    }

    /**
     * Sent if the database requires migration and the user should be prompted by the adapter.
     */
    case object MigrationRequired extends Enum {
        val index = 3
    }

    /**
     * Sent during migration if the user allowed it.
     */
    case object Migrating extends Enum {
        val index = 4
    }

    /**
     * Sent after successful migration
     */
    case object Migrated extends Enum {
        val index = 5
    }

    // End states ---------------------------------------------

    /**
     * Sent upon successful open.
     */
    case object Opened extends Enum {
        val index = 6
    }

    /**
     * The user cancelled the password entry on an encrypted database.
     */
    case object PasswordCancelled extends Enum {
        val index = 6
    }

    /**
     * The user rejected the migration request on an old database.
     */
    case object MigrationCancelled extends Enum {
        val index = 6
    }

    /**
     * The migration cannot be done as this database is at a
     * more recent version than the application supports. After
     * receiving this ProgressStage, you will receive a
     * migrationNotPossible() call.
     */
    case object MigrationNotPossible extends Enum {
        val index = 6
    }

    /**
     * The migration cannot be done as this database was
     * created by some other application (the application
     * declared in the database does not match the
     * runtime application). After receiving this ProgressStage,
     * you will receive a createdByOtherApplication() call.
     */
    case object OtherApplicationDatabase extends Enum {
        val index = 6
    }

    /**
     * The open cannot be done since there is no application
     * details available, so the opener cannot check whether
     * this database was created by that application. After
     * receiving this ProgressStage, you will receive a
     * noApplicationAvailable call.
     */
    case object NoApplicationDetails extends Enum {
        val index = 6
    }

    /**
     * The migration failed and its effects have been rolled
     * back (as far is as practical, given H2's auto-commit
     * of DML when DDL is executed - ignoring the context
     * of any outstanding transaction. After receiving this
     * ProgressStage, you will receive a migrationFailed()
     * call.
     */
    case object MigrationFailed extends Enum {
        val index = 6
    }

    /**
     * The database is not present.
     */
    case object NotPresent extends Enum {
        val index = 6
    }

    /**
     * Failed to open for a serious reason
     */
    case object OpenFailed extends Enum {
        val index = 6
    }
}

/**
 * A DatabaseAccessFactory uses an OpenWorkflowAdapter to inform the user of:
 * <ul>
 * <li> progress during the open
 * <li> to request any password
 * <li> to request confirmation for migration
 * <li> to inform the user of any failures.
 * </ul>
 * The start and end of an open operation can also be signalled, e.g. for
 * setting and clearing the hourglass cursor.
 *
 */
trait OpenWorkflowAdapter {

    /**
     * The opening operation is starting. Always called before any progress.
     */
    def startOpening()

    /**
     * Report progress of the open operation to the user interface
     * @param progressStage the stage we have reached
     * @param description a short text to show the user
     */
    def reportProgress(progressStage: OpenProgressStage.Enum, description: String)

    /**
     * The database is encrypted, and the password must be prompted for and
     * returned.
     * @return Some(password), or None if the user cancels the password entry.
     */
    def requestPassword(): Option[String]

    /**
     * This is an old database, and must be migrated before it can
     * be used. Prompt the user to accept or reject the migration.
     * If they reject, the database cannot be opened. If they
     * accept, it will be migrated, and then opened.
     * @return true if the migration is to be accepted, false to
     * reject.
     */
    def requestMigration(): Boolean

    // TODO need to add the migrate(dataSource, jdbcTemplate, etc) here

    /**
     * Report to the user the migration cannot be done as this
     * database is at a more recent version than the application
     * supports.
     * TODO: possibly add the details of the application that is
     * present in the database?
     */
    def migrationNotPossible()

    /**
     * The migration failed and its effects have been rolled
     * back (as far is as practical, given H2's auto-commit
     * of DML when DDL is executed - ignoring the context
     * of any outstanding transaction.
     * @param exception the data access exception that has occurred.
     */
    def migrationFailed(exception: DataAccessException)

    /**
     * The open failed as this database was created by some other
     * application.
     */
    def createdByOtherApplication()

    /**
     * The open failed since the check for creation by the current
     * application could not be done since there are no application
     * details available.
     */
    def noApplicationDetailsAvailable()

    /**
     * Report to the user that the database could not be found.
     * @param exception the not found exception that has occurred
     */
    def databaseNotFound(exception: DataAccessResourceFailureException)

    /**
     * Report to the user that a serious problem has occurred.
     * Note that this should be treated as a very bad problem (database
     * corruption?) - so log a problem report.
     * @param exception the data access exception that has occurred.
     */
    def seriousProblemOccurred(exception: DataAccessException)

    /**
     * The opening operation has completed. Always called after all progress.
     */
    def stopOpening()
}
