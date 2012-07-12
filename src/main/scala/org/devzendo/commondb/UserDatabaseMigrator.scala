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

import org.springframework.dao.DataAccessException

trait UserDatabaseMigrator {
    /**
     * The database schema is at a version older than that given
     * by the application, so migrate it to the latest version. The
     * framework will record the new version in the Versions
     * table, after migration. If the exception is thrown, the
     * entire migration will be rolled back, and the open
     * terminated.
     *
     * @param access the DatabaseAccess, which contains the DataSource, for
     * low-level access to the database, the Spring SimpleJdbcTemplate, for
     * easier access to the database atop JDBC, and the Version/Sequence DAOs
     * @param currentSchemaVersion the version of the application's database
     * schema as recorded in the database currently; i.e. the version
     * being upgraded from
     * @throws DataAccessException on migration failure
     */
    @throws(classOf[DataAccessException])
    def migrateSchema(access: DatabaseAccess[_], currentSchemaVersion: SchemaVersion)
}
