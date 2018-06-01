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

import org.devzendo.commondb.impl.JdbcTemplateDatabaseAccessFactory
import org.junit.Test
import org.scalatest.MustMatchers
import org.scalatest.junit.AssertionsForJUnit

class AnorakDatabaseAccess(override val databaseAccess: DatabaseAccess[AnorakDatabaseAccess]) extends UserDatabaseAccess(databaseAccess) {
    def close() {
    }
}

class TestDatabaseAccessFactory extends AssertionsForJUnit with MustMatchers {
    @Test
    def aTypedJdbcDatabaseAccessFactoryIsAvailable() {
        val factory = DatabaseAccessFactory[AnorakDatabaseAccess]()
        factory.getClass must be(classOf[JdbcTemplateDatabaseAccessFactory[AnorakDatabaseAccess]])
    }

    @Test
    def anExistentialJdbcDatabaseAccessFactoryIsAvailable() {
        val factory = DatabaseAccessFactory()
        factory.getClass must be(classOf[JdbcTemplateDatabaseAccessFactory[_]])
    }

}
