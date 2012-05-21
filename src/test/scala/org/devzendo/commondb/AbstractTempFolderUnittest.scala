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
import java.io.{FileOutputStream, File}
import org.easymock.EasyMock
import org.junit.{After, Before}
import org.scalatest.junit.{MustMatchersForJUnit, AssertionsForJUnit}

class AbstractTempFolderUnittest extends AssertionsForJUnit with MustMatchersForJUnit {
    private[this] var tempDir: TemporaryFolder = null
    var temporaryDirectory: File = null

    @Before
    def before {
        tempDir = new TemporaryFolder()
        tempDir.create()
        temporaryDirectory = tempDir.getRoot()
    }

    @After
    def after {
        tempDir.delete()
    }
}
