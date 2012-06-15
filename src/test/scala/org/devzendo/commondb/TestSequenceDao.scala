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
import org.slf4j.LoggerFactory


class TestSequenceDao extends AbstractTempFolderUnittest with AutoCloseDatabaseUnittest with AssertionsForJUnit with MustMatchersForJUnit {
    val LOGGER = LoggerFactory.getLogger(classOf[TestSequenceDao])
    val initialCodeVersion = CodeVersion("1.0")
    val initialSchemaVersion = SchemaVersion("0.4")

    @Test
    def checkSequenceInitialValue() {
        database = databaseAccessFactory.create(temporaryDirectory, "checksequenceinitialvalue", None, initialCodeVersion, initialSchemaVersion, None, None)

        database.get.sequenceDao.nextSequence must be (0L)
    }

    @Test
    def checkSequenceIncrements() {
        database = databaseAccessFactory.create(temporaryDirectory, "checksequenceinitialvalue", None, initialCodeVersion, initialSchemaVersion, None, None)

        val sequenceDao = database.get.sequenceDao

        var last: Long = -1
        for (i <- 0 until 40) {
            val start = System.currentTimeMillis()
            val seq = sequenceDao.nextSequence
            val stop = System.currentTimeMillis()
            val duration = stop - start
            LOGGER.info("Sequence value #" + (i + 1) + " is " + seq + " (took " + duration + " ms to generate)")
            seq must be > (last)
            last = seq
        }
    }
}