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

private case class OneString(thing: String) extends RepresentationType[String](thing)
private case class TwoString(thing: String) extends RepresentationType[String](thing)
private case class OneInt(thing: Int) extends RepresentationType[Int](thing)

class TestRepresentationType extends AssertionsForJUnit with MustMatchersForJUnit {
    @Test
    def instancesOfSameRepresentationTypeWithSameContentAreEqual() {
        val one1 = OneString("hi")
        val one2 = OneString("hi")
        one1 must equal(one2)
        one2 must equal(one1)
        one1.hashCode() must equal(one2.hashCode())
    }

    @Test
    def instancesOfSameRepresentationWithDifferentContentTypeAreNotEqual() {
        val one1 = OneString("hi")
        val one2 = OneString("a suffusion of yellow")
        one1 must not equal(one2)
        one2 must not equal(one1)
        one1.hashCode() must not equal(one2.hashCode())
    }

    @Test
    def instancesOfDifferentRepresentationTypeWithSameContentAreNotEqual() {
        val one1 = OneString("hi")
        val two1 = TwoString("hi")
        one1 must not equal(two1)
        two1 must not equal(one1)
        one1.hashCode() must not equal(two1.hashCode())
    }

    @Test
    def instancesOfDifferentRepresentationTypeWithDifferentContentTypesAreNotEqual() {
        val one1 = OneString("hi")
        val one2 = OneInt(42)
        one1 must not equal(one2)
        one2 must not equal(one1)
        one1.hashCode() must not equal(one2.hashCode())
    }

    @Test
    def toStringShowsType() {
        OneString("hi").toString must equal("OneString(hi)")
        OneString(null).toString must equal("OneString(null)")
    }
}
