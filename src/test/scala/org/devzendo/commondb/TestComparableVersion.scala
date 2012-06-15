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


    /*
     *
     *


    @Test(expected = classOf[IllegalArgumentException])
    def testNoDigitsBetweenDots() {
        new ComparableVersion("1..2.3")
    }





    @Test
    public void testEquality() {
        final ComparableVersion firstOne = new ComparableVersion("1.0.0");
        final ComparableVersion secondOne = new ComparableVersion("1.0.0");
        Assert.assertEquals(firstOne, secondOne);
        Assert.assertEquals(0, firstOne.compareTo(secondOne));
        final ComparableVersion otherOne = new ComparableVersion("1.0");
        Assert.assertFalse(firstOne.equals(otherOne));
        Assert.assertFalse(firstOne.compareTo(otherOne) == 0);
        final ComparableVersion firstOneClassifier = new ComparableVersion("1.0.0-beta");
        Assert.assertFalse(firstOne.equals(firstOneClassifier));
        Assert.assertFalse(firstOne.compareTo(firstOneClassifier) == 0);
        // all classifiers are equal...
        final ComparableVersion secondOneClassifier = new ComparableVersion("1.0.0-snapshot");
        Assert.assertEquals(firstOneClassifier, secondOneClassifier);
        Assert.assertEquals(0, firstOneClassifier.compareTo(secondOneClassifier));
    }


    @Test
    public void testSimpleComparison() {
        final ComparableVersion early = new ComparableVersion("1");
        final ComparableVersion later = new ComparableVersion("2");
        Assert.assertEquals(-1, early.compareTo(later));
        Assert.assertEquals(1, later.compareTo(early));
    }

    @Test
    public void testComparison() {
        final ComparableVersion early = new ComparableVersion("1.0.0");
        final ComparableVersion later = new ComparableVersion("1.2.3");
        Assert.assertEquals(-1, early.compareTo(later));
        Assert.assertEquals(1, later.compareTo(early));
    }


    @Test
    public void testComparisonWithMismatchedElements() {
        final ComparableVersion early = new ComparableVersion("1.0");
        final ComparableVersion later = new ComparableVersion("1.0.1");
        Assert.assertEquals(-1, early.compareTo(later));
        Assert.assertEquals(1, later.compareTo(early));
    }


    @Test
    public void testComparisonWithAdditionalZeroElements() {
        final ComparableVersion early = new ComparableVersion("1.0");
        final ComparableVersion later = new ComparableVersion("1.0.0.0");
        Assert.assertFalse(early.equals(later));
        Assert.assertEquals(-1, early.compareTo(later));
        Assert.assertEquals(1, later.compareTo(early));
    }

    @Test
    public void testComparisonWithClassifiers() {
        final ComparableVersion early = new ComparableVersion("1.0.0-snapshot");
        final ComparableVersion later = new ComparableVersion("1.0.0");
        Assert.assertEquals(-1, early.compareTo(later));
        Assert.assertEquals(1, later.compareTo(early));
    }


    @Test
    public void testVersionIntegers() {
        final List<Integer> versionNumberIntegers = new ComparableVersion("1.2.3").getVersionNumberIntegers();
        Assert.assertEquals(3, versionNumberIntegers.size());
        Assert.assertEquals(new Integer(1), versionNumberIntegers.get(0));
        Assert.assertEquals(new Integer(2), versionNumberIntegers.get(1));
        Assert.assertEquals(new Integer(3), versionNumberIntegers.get(2));

        final List<Integer> singleVersionNumberIntegers = new ComparableVersion("1").getVersionNumberIntegers();
        Assert.assertEquals(1, singleVersionNumberIntegers.size());
        Assert.assertEquals(new Integer(1), singleVersionNumberIntegers.get(0));

        final List<Integer> classifierVersionNumberIntegers = new ComparableVersion("1-beta").getVersionNumberIntegers();
        Assert.assertEquals(1, classifierVersionNumberIntegers.size());
        Assert.assertEquals(new Integer(1), classifierVersionNumberIntegers.get(0));
    }

    @Test
    public void rangeTests() {
        Assert.assertFalse(ComparableVersion.inRange(new ComparableVersion("1.0.0"), new ComparableVersion("2.0"), new ComparableVersion("3.0")));
        Assert.assertFalse(ComparableVersion.inRange(new ComparableVersion("3.1.1"), new ComparableVersion("2.0"), new ComparableVersion("3.0")));
        Assert.assertTrue(ComparableVersion.inRange(new ComparableVersion("2.0"), new ComparableVersion("2.0"), new ComparableVersion("3.0")));
        Assert.assertTrue(ComparableVersion.inRange(new ComparableVersion("3.0"), new ComparableVersion("2.0"), new ComparableVersion("3.0")));
        Assert.assertTrue(ComparableVersion.inRange(new ComparableVersion("2.1"), new ComparableVersion("2.0"), new ComparableVersion("3.0")));
        Assert.assertTrue(ComparableVersion.inRange(new ComparableVersion("2.9.9"), new ComparableVersion("2.0"), new ComparableVersion("3.0")));
    }

    @Test
    public void testToString() {
        Assert.assertEquals("v1.0.3-beta", new ComparableVersion("v1.0.3-beta").toString());
        Assert.assertEquals("v1.0.3", new ComparableVersion("v1.0.3").toString());
    }
    */

}
