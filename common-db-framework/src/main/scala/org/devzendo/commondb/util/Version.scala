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

/**
 * A typesafe representation of a version of something.
 * This isn't sealed so that application code can have typesafe versions of its
 * own things.
 * Note that due to the way Version subclass instances are reflectively
 * constructed by the findVersion method, it does not appear possible to define
 * them as inner classes - construction fails in this case.
 */

object Version {
    /**
     * Is the version within the range indicated, inclusively?
     * @param version the version to test for
     * @param leftBound the left hand side of the range
     * @param rightBound the right hand side of the range
     * @return true if leftBound <= version <= rightBound
     */
    def inRange(version: Version, leftBound: Version, rightBound: Version): Boolean = {
        val fromSignum = version.compareTo(leftBound)
        val toSignum = version.compareTo(rightBound)
        if ((fromSignum == 0 || fromSignum == 1)
            &&
            (toSignum == 0 || toSignum == -1)) {
            true
        }
        else {
            false
        }
    }
}

/**
 * A Version string that may be compared to another Version.
 * The Version syntax is:
 * <ul>
 *     <li> Optionally starts with a v or V </li>
 *     <li> digits . digits . digits (as many dot-separated sequences of digits
 *     as required) </li>
 *     <li> An optional classifier, starting with a hyphen, for example: -beta3,
 *     -alpha, -SNAPSHOT, -ga </li>
 * </ul>
 * @param version a version string conforming to the above syntax
 */
abstract class Version(version: String) extends Comparable[Version] {

    if (version == null) {
        throw new IllegalArgumentException("Null version not allowed")
    }
    val trimmedVersion = version.trim
    if (trimmedVersion.length() == 0) {
        throw new IllegalArgumentException("Empty version not allowed")
    }

    val versionRegex = """^[vV]?(\d+(?:\.\d+)*)(-?)(\S+)?$""".r
    val (versionNumberString, classifier, hyphen, versionNumberIntegers) =
        trimmedVersion match {
            case versionRegex(rversionNumbers, rhyphen, rclassifier) =>
                val stringNumbers = rversionNumbers.split("""\.""")
                val processed = (rversionNumbers, // 1

                     rclassifier match {          // 2
                        case null => ""
                        case _ => rclassifier
                     },

                     rhyphen match {              // 3
                         case null => ""
                         case _ => rhyphen
                     },

                     (stringNumbers map {
                         Integer.parseInt(_)
                     }).toList // 4
                    )
                if (processed._3.length() != 0 && processed._2.length() == 0) {
                    throw new IllegalArgumentException("Version '" + version + "' does not have a classifier following a hyphen")
                }
                if (processed._2.indexOf(".") != -1) {
                    // the classifier regex should be non-whitespace but not dots
                    throw new IllegalArgumentException("Version '" + version + "' does not have a valid classifier '" + processed._2 + "'")
                }

                processed
            case _ =>
                throw new IllegalArgumentException("Version '" + version + "' is not a valid version")
        }

    val representation = trimmedVersion

    final def toRepresentation = { representation }
    final override def toString = { representation }

    final override def hashCode: Int = {
        val prime = 31
        var result = 1
        result = prime * result + ( if (classifier == null) 0 else classifier.hashCode() )
        result = prime * result + ( if (versionNumberIntegers == null) 0 else versionNumberIntegers.hashCode() )
        result
    }

    final override def equals(obj: Any): Boolean = {
        obj match {
            case that: Version =>
                if (this eq that) {
                    true
                } else {
                    compareTo(that) == 0
                }
            case _ => false
        }
    }

    /**
     * Compare a Version against this Version.
     * Some examples:
     * <ul>
     *     <li> 1.0 &lt; 1.0.1 </li>
     *     <li> 1.0 &lt; 1.1 </li>
     *     <li> 1.0-alpha &lt; 1.0 </li>
     *     *     <li> 1.0-ga &lt; 1.0 <b>beware</b> </li>
     * </ul>
     * Classifiers are not parsed; there is no difference between -alpha, -beta,
     * -octopus. Their presence is taken to mean that this Version is 'earlier'.
     * @param obj some other Version
     * @return -1, 0 or 1
     */
    final def compareTo(obj: Version): Int = {
        val elementSignum = compareElementForElement(obj)
        if (elementSignum != 0) {
            return elementSignum
        }
        // Version numbers are identical, but shorter numbers of elements mean
        // earlier releases (e.g. 1.0 is earlier than 1.0.0)
        // compareElementForElement will perform right-zero-padding, and checks
        // for 1.0 < 1.0.1, but not 1.0 < 1.0.0
        if (this.versionNumberIntegers.size < obj.versionNumberIntegers.size) {
            return -1
        }
        if (this.versionNumberIntegers.size > obj.versionNumberIntegers.size) {
            return 1
        }
        // version numbers are identical. classifiers always indicate earlier
        // releases than non-classifiers
        if (this.classifier.length() > 0 && obj.classifier.length() == 0) {
            return -1
        }
        if (this.classifier.length() == 0 && obj.classifier.length() > 0) {
            return 1
        }
        // they have equal version numbers and both have a classifier.
        // I could go into snapshot < alpha < beta < rc and all that guff...?
        0
    }

    /**
     * compare version numbers by padding each out to same width, filling
     * short ones with zeros, then comparing element-for-element.
     * @param obj another one of these
     * @return a signum value
     */
    final private[this] def compareElementForElement(obj: Version): Int = {
        val maxVersionElements = Math.max(this.versionNumberIntegers.size, obj.versionNumberIntegers.size)
        for (i <- 0 until maxVersionElements) {
            val thisVersionElement = if (i < this.versionNumberIntegers.size) this.versionNumberIntegers(i) else 0
            val thatVersionElement = if (i < obj.versionNumberIntegers.size) obj.versionNumberIntegers(i) else 0
            val elementSignum = thisVersionElement.compareTo(thatVersionElement)
            if (elementSignum != 0) {
                return elementSignum
            }
        }
        0
    }
}
