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
