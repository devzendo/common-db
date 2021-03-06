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

import org.junit.rules.TemporaryFolder
import java.io.File
import org.junit.{BeforeClass, After, Before}
import org.slf4j.bridge.SLF4JBridgeHandler
import org.slf4j.LoggerFactory
import org.apache.log4j.BasicConfigurator

object AbstractTempFolderUnittest {
    private val LOGGER = LoggerFactory.getLogger(classOf[AbstractTempFolderUnittest])

    @BeforeClass
    def setupLogging() {
        BasicConfigurator.resetConfiguration()
        BasicConfigurator.configure()

        SLF4JBridgeHandler.install()
    }

}
class AbstractTempFolderUnittest  {
    private[this] var tempDir: TemporaryFolder = null
    var temporaryDirectory: File = null

    @Before
    def createTempDirBefore() {
        tempDir = new TemporaryFolder()
        tempDir.create()
        temporaryDirectory = tempDir.getRoot
        AbstractTempFolderUnittest.LOGGER.info("temp directory is " + temporaryDirectory.getAbsolutePath)
    }

    @After
    def deleteTempDirAfter() {
        AbstractTempFolderUnittest.LOGGER.info("tidying up temp dir")
        tempDir.delete()
    }
}
