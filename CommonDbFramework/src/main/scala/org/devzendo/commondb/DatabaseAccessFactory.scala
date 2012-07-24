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
import javax.sql.DataSource
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate
import org.slf4j.LoggerFactory
import org.springframework.transaction.support.TransactionTemplate
import scala.throws
import org.springframework.dao.DataAccessException

object DatabaseAccessFactory {
    def apply[U <: UserDatabaseAccess]() = new JdbcTemplateDatabaseAccessFactory[U]

    val LOGGER = LoggerFactory.getLogger(classOf[DatabaseAccessFactory[_]])
}

/**
 * The VersionsDao is used to store and find the versions of various parts of
 * the application.
 *
 * Version is a subclassable representation type, whose class
 * name is used as a key into the Versions table. The CommonDb framework has
 * two Version subclasses: CodeVersion and SchemaVersion. The actual version
 * numbers of the application code and database schema are passed into the
 * create and open methods of the DataAccessFactory. The database schema
 * version is used in conjunction with the OpenWorkflowAdapter in migrating
 * an older database to the latest schema version.
 *
 * Note that all methods can currently throw Spring-JDBC's DataAccessException.
 * These will be converted into a scalaz Validation in a future release.
 */
trait VersionsDao {
    /**
     * Persist a new value for the version of a specific subclass of Version
     * @param version the value of the version; its class name provides the
     *                key into the Versions table - so only one version can be
     *                stored for a given class.
     * @tparam V the subclass of Version being persisted.
     * @throws org.springframework.dao.DataAccessException on failure
     */
    @throws(classOf[DataAccessException])
    def persistVersion[V <: Version](version: V)

    /**
     * Find the persisted version number for a given version class.
     * @param versionType the class of Version being found.
     * @tparam V the subclass of Version being found.
     * @throws org.springframework.dao.DataAccessException on failure
     * @return Some Version represention instance if this class of version has
     *         been persisted; None if there is no version instance stored.
     */
    @throws(classOf[DataAccessException])
    def findVersion[V <: Version](versionType: Class[V]): Option[V]

    /**
     * Is there a persisted version number for a given version class?
     * @param versionType the class of Version being checked for existence.
     * @tparam V the subclass of Version being checked for existence.
     * @throws org.springframework.dao.DataAccessException on failure
     * @return true if exists, false if not.
     */
    @throws(classOf[DataAccessException])
    def exists[V <: Version](versionType: Class[V]): Boolean
}

/**
 * The SequenceDao provides an incrementing Long sequence, starting at 0L.
 */
trait SequenceDao {
    /**
     * Obtain the next value in the sequence. Start at 0L.
     * @throws org.springframework.dao.DataAccessException on failure
     * @return the incremented sequence number.
     */
    @throws(classOf[DataAccessException])
    def nextSequence: Long
}

/**
 * The UserDatabaseAccess abstract class is subclassed to provide access to the
 * DAOs of your application. Given a DatabaseAccess[YourUserDatabaseAccess]
 * object, user code can call its user() method to access user DAOs via an
 * instance of YourUserDatabaseAccess.
 * @param databaseAccess the underlying access abstraction.
 */
abstract class UserDatabaseAccess(val databaseAccess: DatabaseAccess[_]) {
    /**
     * Called by the framework before the database is closed to give user
     * code the opportunity to flush/tidy up.
     */
    def close()
}

/**
 * The DatabaseAccess gives access to the framework/user database abstractions,
 * provides a mechanism in which operations can be performed inside a
 * transaction, and finally allows the database to be closed.
 *
 * These are obtained from the DatabaseAccessFactory, and are not directly
 * instantiatable from user code.
 *
 * @param databasePath the directory in which the database's file set are to be
 *                     stored.
 * @param databaseName the common prefix name used by the database's file set.
 * @param dataSource the JDBC DataSource that can be used to perform low-level
 *                   operations on the database.
 * @param jdbcTemplate the Spring-JDBC template that is used to perform JDBC
 *                     operations.
 * @tparam U a subclass of UserDatabaseAccess, from which user DAOs may be
 *           obtained
 */
abstract case class DatabaseAccess[U <: UserDatabaseAccess](
        databasePath: File,
        databaseName: String,
        dataSource: DataSource,
        jdbcTemplate: SimpleJdbcTemplate) {
    var user: Option[U] = None
    def close()
    def isClosed: Boolean
    def versionsDao: VersionsDao
    def sequenceDao: SequenceDao
    def createTransactionTemplate: TransactionTemplate
}

/**
 * A representation type for type-safe password strings.
 * @param password the password.
 */
case class Password(password: String) extends RepresentationType[String](password)

/**
 * The DatabaseAccessFactory allows user code to create new databases, or open
 * existing ones.
 *
 * @tparam U the subtype that provides user database access, DAOs, etc.
 */
trait DatabaseAccessFactory[U <: UserDatabaseAccess] {

    /**
     * Check whether a database exists, so you can tell whether to call open or
     * create.
     *
     * @param databasePath the directory in which the database's file set are to be
     *                     stored.
     * @param databaseName the common prefix name used by the database's file set.
     * @return true iff the database exists, according to H2
     *
     */
    def exists(databasePath: File, databaseName: String): Boolean

    /**
     * Create a new database.
     *
     * @param databasePath the directory in which the database's file set are to be
     *                     stored.
     * @param databaseName the common prefix name used by the database's file set.
     * @param password an optional password with which to encrypt the database
     *                 contents. If None, the database is not encrypted. It is
     *                 not possible to encrypt a database after it has been
     *                 created.
     * @param codeVersion the current version of the application code. This
     *                    will be stored in the Versions table and is accessible
     *                    via the VersionsDao. See the documentation of
     *                    VersionTypes for details of the allowed formats.
     * @param schemaVersion the current version of the database schema. This
     *                    will be stored in the Versions table and is accessible
     *                    via the VersionsDao. See the documentation of
     *                    VersionTypes for details of the allowed formats.
     * @param workflowAdapter an optional CreateWorkflowAdapter that will be
     *                        called back to notify you of the progress of
     *                        creation
     * @param userDatabaseCreator an optional UserDatabaseCreator that can
     *                            provide the means by which you create and
     *                            populate your application's tables.
     * @param userDatabaseAccessFactory an optional function that will
     *                                  instantiate your UserDatabaseAccess,
     *                                  for setting into the returned
     *                                  DatabaseAccess instance.
     * @return an optional DatabaseAccess; if None, then the database cannot be
     *         created; if Some, the contained DatabaseAccess gives access to
     *         the now open database.
     */
    def create(
        databasePath: File,
        databaseName: String,
        password: Option[Password],
        codeVersion: CodeVersion,
        schemaVersion: SchemaVersion,
        workflowAdapter: Option[CreateWorkflowAdapter],
        userDatabaseCreator: Option[UserDatabaseCreator],
        userDatabaseAccessFactory: Option[Function1[DatabaseAccess[U], U]]): Option[DatabaseAccess[U]]

    /**
     * Open a database that should already exist.
     *
     * @param databasePath the directory in which the database's file set are to be
     *                     stored.
     * @param databaseName the common prefix name used by the database's file set.
     * @param password an optional password with which to attempt the opening of
     *                 an encrypted database. If the database is known to be
     *                 unencrypted, use None here. If Some[Password] is used,
     *                 an attempt is made to open the database. If this fails,
     *                 the OpenWorkflowAdapter will be called back to prompt for
     *                 another password; this repeats until the database opens
     *                 succesfully, or the user notifies the OpenWorkflowAdapter
     *                 that the open should be cancelled.
     * @param codeVersion the current version of the application code. This
     *                    will be stored in the Versions table and is accessible
     *                    via the VersionsDao. See the documentation of
     *                    VersionTypes for details of the allowed formats.
     * @param schemaVersion the current version of the database schema. This
     *                    will be stored in the Versions table and is accessible
     *                    via the VersionsDao. See the documentation of
     *                    VersionTypes for details of the allowed formats and
     *                    the version ordering logic. If the schemaVersion
     *                    stored in the database is older than the current
     *                    schemaVersion supplied here, the OpenWorkflowAdapter
     *                    will be called back for you to supply upgrade
     *                    or migration logic. The version you supply here will
     *                    be stored in the Versions table after migration.
     * @param workflowAdapter an optional OpenWorkflowAdapter that will be
     *                        called back to notify you of the progress of
     *                        opening, and to provide the means by which you
     *                        can provide further attempts at passwords.
     * @param userDatabaseMigrator an optional UserDatabaseMigrator that will
     *                             be called back to migrate your application's
     *                             tables/contents to the latest schemaVersion.
     * @param userDatabaseAccessFactory an optional function that will
     *                                  instantiate your UserDatabaseAccess,
     *                                  for setting into the returned
     *                                  DatabaseAccess instance.
     * @return an optional DatabaseAccess; if None, then the database cannot be
     *         opened; if Some, the contained DatabaseAccess gives access to
     *         the now open database.
     */
    def open(
        databasePath: File,
        databaseName: String,
        password: Option[Password],
        codeVersion: CodeVersion,
        schemaVersion: SchemaVersion,
        workflowAdapter: Option[OpenWorkflowAdapter],
        userDatabaseMigrator: Option[UserDatabaseMigrator],
        userDatabaseAccessFactory: Option[Function1[DatabaseAccess[U], U]]): Option[DatabaseAccess[U]]
}
