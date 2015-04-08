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

package org.devzendo.commondb.beanminder.persistence.impl

import org.devzendo.commondb.{UserDatabaseCreator, DatabaseAccess}

class JdbcTemplateBeanMinderUserDatabaseCreator extends UserDatabaseCreator {
    /**
     * Create the tables for the application. This will be called before
     * populateApplicationTables, so that you can create any tables needed by
     * your application.
     * @param access the DatabaseAccess, which contains the DataSource, for
     * low-level access to the database, the Spring SimpleJdbcTemplate, for
     * easier access to the database atop JDBC, and the Version/Sequence DAOs
     */
    def createApplicationTables(access: DatabaseAccess[_]) {
        val ddl = List(
            "CREATE TABLE Accounts("
                + "id INT IDENTITY,"
                + "name VARCHAR(40) NOT NULL,"
                + "with VARCHAR(40),"
                + "accountCode VARCHAR(40) NOT NULL,"
                + "initialBalance INT,"
                + "currentBalance INT"
                + ")",
            "CREATE TABLE Transactions("
                + "id INT IDENTITY,"
                + "accountId INT NOT NULL,"
                + "FOREIGN KEY (accountId) REFERENCES Accounts (id) ON DELETE CASCADE,"
                + "index INT NOT NULL,"
                + "amount INT NOT NULL,"
                + "isCredit BOOLEAN,"
                + "isReconciled BOOLEAN,"
                + "transactionDate DATE,"
                + "accountBalance INT"
                + ")")
        ddl.foreach(access.jdbcTemplate.getJdbcOperations.execute(_))
    }

    /**
     * Populate the tables for the application. This is called after
     * createApplicationTables, so that you can populate the tables previously
     * created. After this, your UserDatabaseAccessFactory's apply function
     * will be called, to create your UserDatabaseAccess facade.
     * @param access the DatabaseAccess, which contains the DataSource, for
     * low-level access to the database, the Spring SimpleJdbcTemplate, for
     * easier access to the database atop JDBC, and the Version/Sequence DAOs
     */
    def populateApplicationTables(access: DatabaseAccess[_]) {

    }
}
