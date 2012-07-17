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

package org.devzendo.commondb.beanminder.persistence

import impl.{JdbcTemplateBeanMinderUserDatabaseCreator, JdbcTemplateBeanMinderDatabaseAccess}
import org.devzendo.commondb._
import org.devzendo.commondb.CodeVersion
import org.devzendo.commondb.DatabaseAccess
import scala.Some
import org.springframework.dao.DataAccessException
import org.devzendo.commondb.CreateProgressStage.Enum
import org.junit.After

private class BeanMinderUnittestCreateWorkflowAdapter extends CreateWorkflowAdapter {
    // the user interface methods here are all no-ops for this example
    def startCreating() {}
    def reportProgress(progressStage: Enum, description: String) {}
    def seriousProblemOccurred(exception: DataAccessException) {}
    def stopCreating() {}
}

trait BeanMinderUnittest extends AbstractTempFolderUnittest {
    val codeVersion = CodeVersion("0.1")
    val schemaVersion = SchemaVersion("0.1")

    val beanMinderDatabaseAccessFactory = DatabaseAccessFactory[BeanMinderDatabaseAccess]

    def createBeanMinderDatabase(name: String) = {
        val beanMinderUserDatabaseFactory =
            new ((DatabaseAccess[BeanMinderDatabaseAccess]) => BeanMinderDatabaseAccess) {
                def apply(databaseAccess: DatabaseAccess[BeanMinderDatabaseAccess]) =
                    new JdbcTemplateBeanMinderDatabaseAccess(databaseAccess)
            }

        beanMinderDatabaseAccessFactory.create(
            temporaryDirectory, name, None,
            codeVersion, schemaVersion,
            Some(new BeanMinderUnittestCreateWorkflowAdapter),
            Some(new JdbcTemplateBeanMinderUserDatabaseCreator),
            Some(beanMinderUserDatabaseFactory))
    }

    var database: Option[DatabaseAccess[_]] = None

    @After
    def closeDatabase() {
        for (d <- database) {
            d.close()
        }
    }
}
