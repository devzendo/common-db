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

case class ComparableVersion(version: String) extends Version(version)

class TestComparableVersion extends AssertionsForJUnit with MustMatchersForJUnit {

    @Test(expected = classOf[IllegalArgumentException])
    def testNull() {
        new ComparableVersion(null)
    }

    @Test(expected = classOf[IllegalArgumentException])
    def testEmpty() {
        new ComparableVersion("")
    }

    @Test(expected = classOf[IllegalArgumentException])
    def testEmptySpace() {
        new ComparableVersion("   ");
    }

    @Test
    def trimmedNicely() {
        new ComparableVersion(" 1.0.0 ").toRepresentation must be("1.0.0");
    }

    @Test(expected = classOf[IllegalArgumentException])
    def testBadForm() {
        new ComparableVersion("I say, Jeeves - what ho?")
    }

    @Test
    def testClassifier() {
        new ComparableVersion(" 1 ").classifier must be("")
        new ComparableVersion("1.0.0").classifier must be("")
        new ComparableVersion(" 1-x  ").classifier must be("x")
        new ComparableVersion("1-x").classifier must be("x")
    }

    @Test(expected = classOf[IllegalArgumentException])
    def testBadClassifier() {
        new ComparableVersion("2.3-")
    }

    @Test
    def testClassifierWithNoHyphen() {
        val version = new ComparableVersion("2.3alpha")
        version.versionNumberString must be("2.3")
        version.classifier must be ("alpha")
    }

    @Test
    def testToString() {
        new ComparableVersion("v1.0.3-beta").toString must be("v1.0.3-beta")
        new ComparableVersion("v1.0.3").toString must be("v1.0.3")
    }

    @Test
    def testVersionIntegers() {
        val versionNumberIntegers = new ComparableVersion("1.2.3").versionNumberIntegers
        versionNumberIntegers must have size(3)
        versionNumberIntegers(0) must be(1)
        versionNumberIntegers(1) must be(2)
        versionNumberIntegers(2) must be(3)

        val singleVersionNumberIntegers = new ComparableVersion("1").versionNumberIntegers
        singleVersionNumberIntegers must have size(1)
        singleVersionNumberIntegers(0) must be(1)

        val classifierVersionNumberIntegers = new ComparableVersion("1-beta").versionNumberIntegers
        classifierVersionNumberIntegers must have size(1)
        classifierVersionNumberIntegers(0) must be(1)
    }

    @Test(expected = classOf[IllegalArgumentException])
    def testNoDigitsBetweenDots() {
        new ComparableVersion("1..2.3")
    }

    @Test
    def testEquality() {
        val firstOne = new ComparableVersion("1.0.0")
        val secondOne = new ComparableVersion("1.0.0")
        firstOne must equal(secondOne)
        firstOne.compareTo(secondOne) must be (0)
        val otherOne = new ComparableVersion("1.0")
        firstOne must not(equal(otherOne))
        firstOne.compareTo(otherOne) must not(be(0))
        val firstOneClassifier = new ComparableVersion("1.0.0-beta")
        firstOne.equals(firstOneClassifier) must be(false)
        firstOne.compareTo(firstOneClassifier) must not(be(0))
        // all classifiers are equal...
        val secondOneClassifier = new ComparableVersion("1.0.0-snapshot")
        firstOneClassifier must equal(secondOneClassifier)
        firstOneClassifier.compareTo(secondOneClassifier) must be(0)
    }

    @Test
    def testSimpleComparison() {
        val early = new ComparableVersion("1")
        val later = new ComparableVersion("2")
        early.compareTo(later) must be(-1)
        later.compareTo(early) must be(1)
    }

    @Test
    def testComparison() {
        val early = new ComparableVersion("1.0.0")
        val later = new ComparableVersion("1.2.3")
        early.compareTo(later) must be(-1)
        later.compareTo(early) must be(1)
    }


    @Test
    def testComparisonWithMismatchedElements() {
        val early = new ComparableVersion("1.0")
        val later = new ComparableVersion("1.0.1")
        early.compareTo(later) must be(-1)
        later.compareTo(early) must be(1)
    }


    @Test
    def testComparisonWithAdditionalZeroElements() {
        val early = new ComparableVersion("1.0")
        val later = new ComparableVersion("1.0.0.0")
        early must not(equal(later))
        early.compareTo(later) must be(-1)
        later.compareTo(early) must be(1)
    }

    @Test
    def testComparisonWithClassifiers() {
        val early = new ComparableVersion("1.0.0-snapshot")
        val later = new ComparableVersion("1.0.0")
        early.compareTo(later) must be(-1)
        later.compareTo(early) must be(1)
    }



    /*
     *
     *



    @Test
    public void rangeTests() {
        Assert.assertFalse(ComparableVersion.inRange(new ComparableVersion("1.0.0"), new ComparableVersion("2.0"), new ComparableVersion("3.0")));
        Assert.assertFalse(ComparableVersion.inRange(new ComparableVersion("3.1.1"), new ComparableVersion("2.0"), new ComparableVersion("3.0")));
        Assert.assertTrue(ComparableVersion.inRange(new ComparableVersion("2.0"), new ComparableVersion("2.0"), new ComparableVersion("3.0")));
        Assert.assertTrue(ComparableVersion.inRange(new ComparableVersion("3.0"), new ComparableVersion("2.0"), new ComparableVersion("3.0")));
        Assert.assertTrue(ComparableVersion.inRange(new ComparableVersion("2.1"), new ComparableVersion("2.0"), new ComparableVersion("3.0")));
        Assert.assertTrue(ComparableVersion.inRange(new ComparableVersion("2.9.9"), new ComparableVersion("2.0"), new ComparableVersion("3.0")));
    }

    */

}
