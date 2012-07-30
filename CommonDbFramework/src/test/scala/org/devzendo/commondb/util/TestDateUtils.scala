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

package org.devzendo.commondb.util

import org.scalatest.junit.{MustMatchersForJUnit, AssertionsForJUnit}
import org.junit.Test
import java.sql.Date

class TestDateUtils extends AssertionsForJUnit with MustMatchersForJUnit {
    @Test
    def testDateNormalisation() {
        val millisWithHourMinuteSecondMillis = createSQLDateWithMillis()
        val startDate = new Date(millisWithHourMinuteSecondMillis)
        val normalisedDate = DateUtils.normalise(startDate)
        timeMustBeZero(normalisedDate)
    }

    @Test
    def testMillisFromEpochNormalisation() {
        val millisWithHourMinuteSecondMillis = createSQLDateWithMillis()
        val normalisedDate = DateUtils.normalise(millisWithHourMinuteSecondMillis)
        timeMustBeZero(normalisedDate)
    }

    def timeMustBeZero(normalisedDate: Date) {
        val calendar = new java.util.GregorianCalendar()
        calendar.setTime(normalisedDate)
        calendar.get(java.util.Calendar.HOUR) must be(0)
        calendar.get(java.util.Calendar.MINUTE) must be(0)
        calendar.get(java.util.Calendar.SECOND) must be(0)
        calendar.get(java.util.Calendar.MILLISECOND) must be(0)
    }

    /**
     * @return a time in milliseconds that does have hours, minutes, seconds, and millisecond
     *         values that are positive
     */
    def createSQLDateWithMillis(): Long = {
        val calendar = new java.util.GregorianCalendar()
        calendar.set(java.util.Calendar.YEAR, 2009)
        calendar.set(java.util.Calendar.MONDAY, 2)
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 22)
        calendar.set(java.util.Calendar.HOUR, 4)
        calendar.set(java.util.Calendar.MINUTE, 30)
        calendar.set(java.util.Calendar.SECOND, 23)
        calendar.set(java.util.Calendar.MILLISECOND, 223)
        calendar.getTimeInMillis
    }

    // implicit conversion of a Date to a NormalisedDate happens here
    def whenCalledIsGivenNormalisedDate(nd: NormalisedDate) {
        timeMustBeZero(nd.toRepresentation)
    }

    @Test
    def testImplicitConversionOfDates() {
        val millisWithHourMinuteSecondMillis = createSQLDateWithMillis()
        val startDate = new Date(millisWithHourMinuteSecondMillis)
        whenCalledIsGivenNormalisedDate(startDate)
    }

    @Test
    def testApplyFactoryConversionOfDates() {
        val millisWithHourMinuteSecondMillis = createSQLDateWithMillis()
        val startDate = new Date(millisWithHourMinuteSecondMillis)
        val appliedDate = NormalisedDate(startDate)
        timeMustBeZero(appliedDate.toRepresentation)
    }

    @Test
    def testApplyFactoryConversionOfMillisSinceEpoch() {
        val millisWithHourMinuteSecondMillis = createSQLDateWithMillis()
        val appliedDate = NormalisedDate(millisWithHourMinuteSecondMillis)
        timeMustBeZero(appliedDate.toRepresentation)
    }

    @Test
    def normalisedDatesMsApartHaveSameHashCodesAndAreEqual() {
        val millis1 = createSQLDateWithMillis()
        val appliedDate1 = NormalisedDate(millis1)
        val millis2 = millis1 + 4000
        val appliedDate2 = NormalisedDate(millis2)

        appliedDate1.hashCode must equal(appliedDate2.hashCode)
        appliedDate1 must equal(appliedDate2)
        appliedDate2 must equal(appliedDate1)

        appliedDate1.toRepresentation.hashCode must equal(appliedDate2.toRepresentation.hashCode)
        appliedDate1.toRepresentation must equal(appliedDate2.toRepresentation)
        appliedDate2.toRepresentation must equal(appliedDate1.toRepresentation)
    }

    @Test
    def normalisedDatesADayApartHaveDifferentHashCodesAndAreNotEqual() {
        val millis1 = createSQLDateWithMillis()
        val appliedDate1 = NormalisedDate(millis1)
        val millis2 = millis1 + (1000 * 60 * 60 * 24)
        val appliedDate2 = NormalisedDate(millis2)

        appliedDate1.hashCode must not equal (appliedDate2.hashCode)
        appliedDate1 must not equal (appliedDate2)
        appliedDate2 must not equal (appliedDate1)

        appliedDate1.toRepresentation.hashCode must not equal (appliedDate2.toRepresentation.hashCode)
        appliedDate1.toRepresentation must not equal (appliedDate2.toRepresentation)
        appliedDate2.toRepresentation must not equal (appliedDate1.toRepresentation)
    }
}
