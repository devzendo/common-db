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
import javax.sql.DataSource
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate

object CreateProgressStage {

    sealed trait Enum {
        def index: Int

        /**
         * How many steps are there in total?
         * @return the maximum value of a CreateProgressStage
         */
        def maximumStages = 4
    }

    /**
     * The creation operation is starting. Sent almost immediately to give some
     * immediate feedback.
     */
    case object CreateStarting extends Enum {
        val index = 0;
    }

    /**
     * Sent immediately prior to creating the database.
     */
    case object Creating extends Enum {
        val index = 1;
    }

    /**
     * Sent before creating the tables
     */
    case object CreatingTables extends Enum {
        val index = 2;
    }

    /**
     * Sent before populating the tables
     */
    case object PopulatingTables extends Enum {
        val index = 3;
    }

    // End states ---------------------------------------------

    /**
     * Sent upon successful creation.
     */
    case object Created extends Enum {
        val index = 4;
    }

    /**
     * Failed to creation for a serious reason
     */
    case object CreationFailed extends Enum {
        val index = 4;
    }

}

/**
 * A DatabaseAccessFactory uses a CreateWorkflowAdapter to inform the user of:
 * <ul>
 * <li> progress during the creation
 * <li> to request application-specific information from the application, e.g.
 * what the current schema and code versions are.
 * <li> to request that the application create and populate its tables, given
 * the DataSource and SimpleJdbcTemplate.
 * <li> to inform the user of any failures.
 * </ul>
 * The start and end of a creation operation can also be signalled, e.g. for
 * setting and clearing the hourglass cursor.
 *
 */
trait CreateWorkflowAdapter {

    /**
     * The creation operation is starting. Always called before any progress.
     */
    def startCreating()

    /**
     * Report progress of the creation operation to the user interface
     * @param progressStage the stage we have reached
     * @param description a short text to show the user
     */
    def reportProgress(progressStage: CreateProgressStage.Enum, description: String)

    /**
     * Request the application code version.
     * @return the application code version.
     */
    def requestApplicationCodeVersion(): String

    /**
     * Request the application schema version.
     * @return the application schema version.
     */
    def requestApplicationSchemaVersion(): String


    /**
     * Create the tables for the application.
     * @param dataSource the JDBC DataSource to use in creating the tables
     * @param jdbcTemplate the Spring SimpleJdbcTemplate to use in creating the
     * tables.
     */
    def createApplicationTables(dataSource: DataSource, jdbcTemplate: SimpleJdbcTemplate)

    /**
     * Populate the tables for the application.
     * @param dataSource the JDBC DataSource to use in populating the tables
     * @param jdbcTemplate the Spring SimpleJdbcTemplate to use in populating
     * the tables.
     */
    def populateApplicationTables(dataSource: DataSource, jdbcTemplate: SimpleJdbcTemplate)

    /**
     * Report to the user that a serious problem has occurred.
     * Note that this should be treated as a very bad problem (database
     * corruption?) - so log a problem report.
     * @param exception the data access exception that has occurred.
     */
    def seriousProblemOccurred(exception: DataAccessException)

    /**
     * The creation operation has completed. Always called after all progress.
     */
    def stopCreating()
}
