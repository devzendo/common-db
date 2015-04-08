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
