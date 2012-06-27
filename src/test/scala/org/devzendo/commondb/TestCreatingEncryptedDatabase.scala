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
import java.io.{FileInputStream, IOException, FileFilter, File}
import org.slf4j.LoggerFactory

object TestCreatingEncryptedDatabase {
    val LOGGER = LoggerFactory.getLogger(classOf[TestCreatingEncryptedDatabase])
}

class TestCreatingEncryptedDatabase extends AutoCloseDatabaseCreatingUnittest with AssertionsForJUnit with MustMatchersForJUnit {
    val password = Password("Squeamish Ossifrage")

    @Test
    def encryptedDatabaseLooksRandom() {
        val databaseName = "encrypteddb"
        databaseAccessFactory.create(temporaryDirectory, databaseName, Some(password), codeVersion, schemaVersion, None, None).get.close()

        databaseLooksRandom(temporaryDirectory, databaseName) must equal(true)
    }

    @Test
    def unencryptedDatabaseDoesNotLookRandom() {
        val databaseName = "plaindb"
        databaseAccessFactory.create(temporaryDirectory, databaseName, None, codeVersion, schemaVersion, None, None).get.close()

        databaseLooksRandom(temporaryDirectory, databaseName) must equal(false)
    }

    private[this] def databaseLooksRandom(databaseDirectory: File, databaseName: String): Boolean = {
        // skip small files e.g. lock files are ~ 99 bytes.
        // even an empty db has files around 128KB.
        val dbFiles = databaseFiles(databaseDirectory, databaseName) filter((f:File) => f.isFile && f.length() > 1024)

        dbFiles.size > 0 && dbFiles.forall((f: File) => isRandom(f))
    }

    private[this] def isRandom(file: File): Boolean = {
        TestCreatingEncryptedDatabase.LOGGER.info("Checking %s for randomness".format(file.getAbsolutePath))
        try {
            val hist = new Array[Long](256)
            val size = file.length()
            val is = new FileInputStream(file)
            try {
                val buf = new Array[Byte](512)
                var nread = 0
                do {
                    nread = is.read(buf)
                    if (nread != -1) {
                        for (i <- 0 until nread) {
                            val rand = buf(i) & 0x00ff
                            hist(rand) += 1
                        }
                    }
                } while (nread != -1)
            } finally {
                is.close()
            }
            // need 75% of the data in the file to be within 20% of size/256
            val bytesPerfectlyEquallyDistributed: Double  = size / 256
            val twentyPercent: Double = (bytesPerfectlyEquallyDistributed * 0.2)
            val upperTolerance: Double = bytesPerfectlyEquallyDistributed + twentyPercent
            val lowerTolerance: Double = bytesPerfectlyEquallyDistributed - twentyPercent
//            TestCreatingEncryptedDatabase.LOGGER.debug("[%f, %f, %f]".format(lowerTolerance, bytesPerfectlyEquallyDistributed, upperTolerance))
            var numWithinTolerance = 0
            for (i <- 0 until 256) {
                val withinTolerance = (hist(i) >= lowerTolerance.toLong) && (hist(i) <= upperTolerance.toLong)
//                TestCreatingEncryptedDatabase.LOGGER.debug("Byte 0x%02X, count %d %swithin tolerance".format(i, hist(i), if(withinTolerance) " " else "NOT "))
                if (withinTolerance) {
                    numWithinTolerance += 1
                }
            }
            val randomness: Double = numWithinTolerance / 256.0
            TestCreatingEncryptedDatabase.LOGGER.debug("Randomness is %f".format(randomness))
            randomness >= 0.75
        } catch {
            case e: IOException =>
                TestCreatingEncryptedDatabase.LOGGER.warn("IOException checking randomness: %s".format(e.getMessage))
                return false
        }
    }

    private[this] def databaseFiles(databaseDirectory: File, databaseName: String): List[File] = {
        val dbFileArray = if (databaseDirectory.exists() && databaseDirectory.isDirectory) {
            val filter = new FileFilter() {
                def accept(pathname: File) = {
                    val ok = pathname.isFile && pathname.getName.startsWith(databaseName)
                    TestCreatingEncryptedDatabase.LOGGER.debug("Considering " + pathname.getAbsolutePath + " " + ok)
                    ok
                }
            }
            databaseDirectory.listFiles(filter)
        } else {
            Array.empty[File]
        }
        dbFileArray.toList
    }
}
