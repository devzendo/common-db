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

package org.devzendo.commondb.dao.impl

import org.springframework.jdbc.core.simple.{ParameterizedRowMapper, SimpleJdbcTemplate}
import org.devzendo.commondb.dao.VersionsDao
import org.springframework.dao.{EmptyResultDataAccessException, DataAccessException}
import org.devzendo.commondb.util.Version
import java.sql.ResultSet

class JdbcTemplateVersionsDao(jdbcTemplate: SimpleJdbcTemplate) extends VersionsDao {

    @throws(classOf[DataAccessException])
    def findVersion[V <: Version](versionType: Class[V]): Option[V] = {

        val sql = "SELECT version FROM Versions WHERE entity = ?"
        val mapper: ParameterizedRowMapper[V] = new ParameterizedRowMapper[V]() {
            // notice the return type with respect to Java 5 covariant return types
            def mapRow(rs: ResultSet, rowNum: Int) = {
                val ctor = versionType.getConstructor(classOf[String])
                ctor.newInstance(rs.getString("version"))
            }
        }
        //noinspection deprecation
        try {
            Some(jdbcTemplate.queryForObject(sql, mapper, versionType.getSimpleName))
        } catch {
            case e: EmptyResultDataAccessException => None
        }
    }

    @throws(classOf[DataAccessException])
    def persistVersion[V <: Version](version: V) {
        if (count(version.getClass) == 0) {
            jdbcTemplate.update(
                "INSERT INTO Versions (entity, version) VALUES (?, ?)",
                version.getClass.getSimpleName, version.toRepresentation)
        } else {
            jdbcTemplate.update(
                "UPDATE Versions SET version = ? WHERE entity = ?",
                version.toRepresentation, version.getClass.getSimpleName)
        }
    }

    @throws(classOf[DataAccessException])
    def exists[V <: Version](versionType: Class[V]): Boolean = {
        count(versionType) == 1
    }

    @throws(classOf[DataAccessException])
    private[this] def count[V <: Version](versionType: Class[V]): Int = {
        jdbcTemplate.queryForInt(
            "SELECT COUNT(0) FROM Versions WHERE entity = ?",
            versionType.getSimpleName)
    }
}

