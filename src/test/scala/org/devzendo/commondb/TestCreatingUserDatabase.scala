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
import javax.sql.DataSource
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate
import org.devzendo.commondb.CreateProgressStage.Enum
import org.springframework.dao.{EmptyResultDataAccessException, DataAccessException}

case class CakeAge(age: Int) extends RepresentationType[Int](age)
case class CakeName(name: String) extends RepresentationType[String](name)

trait CakeDao {
    def ageOfCake(name: CakeName): Option[CakeAge]
    def cakeAgeIs(name: CakeName, age: CakeAge)
}

class CakeCreateWorkflowAdapter extends CreateWorkflowAdapter {
    def startCreating() {
    }

    def reportProgress(progressStage: Enum, description: String) {
    }

    def createApplicationTables(access: DatabaseAccess[_]) {
        access.jdbcTemplate.getJdbcOperations.execute("CREATE TABLE Cakes(name VARCHAR(40), age VARCHAR(40))")
    }

    def populateApplicationTables(access: DatabaseAccess[_]) {
        val data: List[(String, Int)] = List(("Sponge", 4), ("Gateaux", 1))
        for (cake_age <- data) {
            access.jdbcTemplate.update("INSERT INTO Cakes (name, age) VALUES (?, ?)",
                cake_age._1, cake_age._2: java.lang.Integer)
        }
    }

    def seriousProblemOccurred(exception: DataAccessException) {
    }

    def stopCreating() {
    }
}

class CakeDatabaseAccess(override val databaseAccess: DatabaseAccess[CakeDatabaseAccess]) extends UserDatabaseAccess(databaseAccess) {
    val jdbcTemplate = databaseAccess.jdbcTemplate

    def cakeDao = new CakeDao {

        def cakeAgeIs(name: CakeName, age: CakeAge) {
            val count = jdbcTemplate.queryForInt(
                "SELECT COUNT(0) FROM Cakes WHERE name = ?",
                name.toRepresentation)
            if (count == 0) {
                jdbcTemplate.update(
                    "INSERT INTO Cakes (name, age) VALUES (?, ?)",
                    name.toRepresentation, age.toRepresentation: java.lang.Integer)
            } else {
                jdbcTemplate.update(
                    "UPDATE Cakes SET age = ? WHERE name = ?",
                    age.toRepresentation: java.lang.Integer, name.toRepresentation)
            }
        }

        def ageOfCake(name: CakeName) =
            try {
                Some(CakeAge(jdbcTemplate.queryForInt(
                    "SELECT age FROM Cakes WHERE name = ?",
                    name.toRepresentation)))
            } catch {
                case e: EmptyResultDataAccessException => None
            }
    }

    def close() {
    }
}

class TestCreatingUserDatabase extends AbstractTempFolderUnittest with AssertionsForJUnit with MustMatchersForJUnit {
    val codeVersion = CodeVersion("1.0")
    val schemaVersion = SchemaVersion("0.4")

    val cakeDatabaseAccessFactory = new JdbcTemplateDatabaseAccessFactory[CakeDatabaseAccess]()

    val cakeUserDatabaseFactory = new ((DatabaseAccess[CakeDatabaseAccess]) => CakeDatabaseAccess) {
        def apply(databaseAccess: DatabaseAccess[CakeDatabaseAccess]) = new CakeDatabaseAccess(databaseAccess)
    }

    def createCakeDatabase(name: String) = {
        cakeDatabaseAccessFactory.create(temporaryDirectory, name, None, codeVersion, schemaVersion,
            Some(new CakeCreateWorkflowAdapter), Some(cakeUserDatabaseFactory))
    }

    def openCheeseDatabase(name: String) = {
        cakeDatabaseAccessFactory.open(temporaryDirectory, name, None, codeVersion, schemaVersion,
            None, Some(cakeUserDatabaseFactory))
    }

    @Test
    def userDaoCanBeUsedToAccessCreatedUserDataAfterCreation() {
        val userDatabase = createCakeDatabase("userdao")

        try {
            userDatabase must be('defined)
            val userAccess = userDatabase.get.user
            userAccess must be('defined)
            def cakeDao = userAccess.get.cakeDao

            cakeDao.ageOfCake(CakeName("Sponge")) must be(Some(CakeAge(4)))
            cakeDao.ageOfCake(CakeName("Gorgonzola")) must be(None)

            cakeDao.cakeAgeIs(CakeName("Cupcake"), CakeAge(2))
            cakeDao.ageOfCake(CakeName("Cupcake")) must be(Some(CakeAge(2)))

            cakeDao.cakeAgeIs(CakeName("Cupcake"), CakeAge(3))
            cakeDao.ageOfCake(CakeName("Cupcake")) must be(Some(CakeAge(3)))

        } finally {
            for (u <- userDatabase) {
                u.close()
            }
        }
    }
}
