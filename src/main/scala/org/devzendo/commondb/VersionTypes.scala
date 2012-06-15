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
abstract class Version(version: String) extends RepresentationType[String](version) with Comparable[Version] {
    if (version == null) {
        throw new IllegalArgumentException("Null version not allowed")
    }
    if (version.length() == 0) {
        throw new IllegalArgumentException("Empty version not allowed")
    }


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
