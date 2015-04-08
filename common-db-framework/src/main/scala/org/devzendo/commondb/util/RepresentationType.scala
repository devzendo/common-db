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
 * A mechanism for creating typesafe extensions of some underlying type that is
 * possibly not extendible, e.g. String.
 * @param value the value to be held in the representation
 * @tparam T the underlying type of the representation
 */
abstract class RepresentationType[T](val value: T) extends Equals {
    /**
     * Obtain the value as the underlying type
     * @return the value
     */
    def toRepresentation: T = value

    override def canEqual(other: Any) = other.isInstanceOf[RepresentationType[T]]

    override def hashCode: Int = 17 + this.getClass.## + value.##

    override def equals(other: Any): Boolean = other match {
        case null => false
        case x: RepresentationType[T] =>
            if (x eq this)
                true
            else
                (x.## == this.##) && (x canEqual this) && (x.value == this.value)
        case _ => false
    }

    override def toString: String = this.getClass.getSimpleName + "(" + value + ")"
}
