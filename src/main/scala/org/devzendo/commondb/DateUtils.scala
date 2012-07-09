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

import java.sql.Date
import java.util

object NormalisedDate {
    implicit def nonNormalisedDate2NormalisedDate(nonNormalisedDate: Date) = new NormalisedDate(nonNormalisedDate)

    implicit def normalisedDate2NonNormalisedDate(normalisedDate: Date) = normalisedDate.self

    def apply(nonNormalisedDate: Date): NormalisedDate = {
        new NormalisedDate(DateUtils.normalise(nonNormalisedDate))
    }
}

class NormalisedDate private(nonNormalisedDate: Date) extends Proxy {
    val self: Date = DateUtils.normalise(nonNormalisedDate)
}

object DateUtils {
    /**
     * Given a Date, possibly having Hour, Minute, Second, Millisecond values, return a
     * new Date with the same Year, Month, Day, with the time parts set to zero.
     * @param initialDate the date with time components
     * @return the date without time components
     */
    def normalise(initialDate: Date): Date = {
        val calendar = new util.GregorianCalendar()
        calendar.setTime(initialDate)
        calendar.set(util.Calendar.MILLISECOND, 0)
        calendar.set(util.Calendar.SECOND, 0)
        calendar.set(util.Calendar.MINUTE, 0)
        calendar.set(util.Calendar.HOUR, 0)
        new Date(calendar.getTimeInMillis)
    }
}


