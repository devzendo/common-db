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

import org.scalatest.junit.{MustMatchersForJUnit, AssertionsForJUnit}
import org.junit.Test

class TestDatabaseExistence extends AutoCloseDatabaseCreatingUnittest with AssertionsForJUnit with MustMatchersForJUnit {

    @Test
    def databaseDoesNotExistSoReturnsFalse() {
        databaseAccessFactory.exists(temporaryDirectory, "doesnotexist") must equal(false)
    }

    @Test
    def databaseDoesExistSoReturnsTrue() {
        createDatabase(temporaryDirectory, "exists", None).get.close()

        databaseAccessFactory.exists(temporaryDirectory, "exists") must equal(true)
    }
}