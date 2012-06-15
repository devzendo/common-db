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

object VersionTypes
// I don't need a type, but it shuts IntelliJ up about the
// types in here not matching the filename

/**
 * A typesafe representation of a version of something.
 * This isn't sealed so that application code can have typesafe versions of its
 * own things.
 * Note that due to the way Version subclass instances are reflectively
 * constructed by the findVersion method, it does not appear possible to define
 * them as inner classes - construction fails in this case.
 */

abstract class Version(version: String) extends Comparable[Version] {

    if (version == null) {
        throw new IllegalArgumentException("Null version not allowed")
    }
    val trimmedVersion = version.trim
    if (trimmedVersion.length() == 0) {
        throw new IllegalArgumentException("Empty version not allowed")
    }

    println("input version '" + trimmedVersion + "'")
    val versionRegex = """^[vV]?(\d+(?:\.\d+)*)(-?)(\S+)?$""".r
    val (versionNumberString, classifier, hyphen, versionNumberIntegers) =
        trimmedVersion match {
            case versionRegex(rversionNumbers, rhyphen, rclassifier) =>
                println("good version '" + trimmedVersion + "'")

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
                println("bad version")
                throw new IllegalArgumentException("Version '" + version + "' is not a valid version")
        }

    val representation = trimmedVersion

    def toRepresentation = { representation }
    override def toString = { representation }

    def compareTo(o: Version) = 0
}

/**
 * Represents the software version of the application that created the database.
 */
case class CodeVersion(version: String) extends Version(version)

/**
 * Represents the database schema version of the application that created the
 * database.
 */
case class SchemaVersion(version: String) extends Version(version)
