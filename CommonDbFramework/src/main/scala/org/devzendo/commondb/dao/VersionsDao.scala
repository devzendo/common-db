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

package org.devzendo.commondb.dao

import org.springframework.dao.DataAccessException
import org.devzendo.commondb.Version

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
