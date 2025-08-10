/*
 * Copyright 2025 Enaium
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.enaium.jimmer.buddy.utility

import cn.enaium.jimmer.buddy.database.model.Table
import cn.enaium.jimmer.buddy.dialog.GenerateEntityDialog.JdbcDriver
import cn.enaium.jimmer.buddy.storage.DatabaseCache
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.ui.Messages
import com.jetbrains.rd.util.reflection.toPath
import java.net.URI
import java.net.URLClassLoader
import java.nio.file.Path
import java.sql.Connection
import java.sql.Driver
import java.sql.DriverManager
import java.sql.DriverPropertyInfo
import java.util.*
import java.util.logging.Logger
import kotlin.io.path.*

/**
 * @author Enaium
 */
fun DatabaseCache.DatabaseItem.getTables(project: Project): Set<Table> {
    val uri = uri
    val isDDL = uri.startsWith("file:")

    var driverJarFile =
        driverFile.takeIf { it.isNotBlank() }?.let { Path(it).takeIf { path -> path.exists() } }
    val driverName = driverName.takeIf { it.isNotBlank() }

    val jdbcDriver = JdbcDriver.entries.find { uri.startsWith("jdbc:${it.scheme}") } ?: let {
        if (isDDL) {
            return@let JdbcDriver.H2
        } else if (driverJarFile == null) {
            Messages.showErrorDialog(
                I18n.message(
                    "dialog.generate.entity.message.unsupportedJDBC",
                    JdbcDriver.entries.joinToString(", ") { it.scheme }),
                "Error"
            )
            return emptySet()
        } else {
            null
        }
    }

    if (driverJarFile == null && jdbcDriver != null) {
        thread { OrderEnumerator.orderEntries(project).runtimeOnly().classesRoots }.forEach {
            if (it.name.startsWith(jdbcDriver.artifact)) {
                driverJarFile = Path(it.path.substringBefore("!"))
            }
        }
    }

    if (driverJarFile == null && jdbcDriver != null) {
        listOfNotNull(
            Path(System.getProperty("user.home")).resolve(".m2"),
            System.getenv("M2_HOME")?.takeIf { it.isNotBlank() }?.let { Path.of(it) },
        ).forEach { path ->
            path.resolve(
                "repository/${
                    jdbcDriver.group.split(".").joinToString("/")
                }/${jdbcDriver.artifact}"
            ).walk().findLast {
                it.isDirectory().not()
                        && it.name.endsWith("-sources.jar").not()
                        && it.name.endsWith("-javadoc.jar").not()
                        && it.name.endsWith(".jar")
            }?.also {
                driverJarFile = it
            }
        }
    }

    if (driverJarFile == null && jdbcDriver != null) {
        listOfNotNull(
            Path(System.getProperty("user.home")).resolve(".gradle"),
            System.getenv("GRADLE_USER_HOME")?.takeIf { it.isNotBlank() }?.let { Path.of(it) },
        ).forEach { path ->
            path.resolve("caches/modules-2/files-2.1/${jdbcDriver.group}/${jdbcDriver.artifact}")
                .walk().findLast {
                    it.isDirectory().not()
                            && it.name.endsWith("-sources.jar").not()
                            && it.name.endsWith("-javadoc.jar").not()
                            && it.name.endsWith(".jar")
                }?.also {
                    driverJarFile = it
                }
        }
    }

    if (driverJarFile == null) {
        if (isDDL) {
            Messages.showErrorDialog(
                "Failed to find H2 driver jar, please check your maven or gradle cache",
                "Error"
            )
        } else if (driverFile.isNotBlank()) {
            Messages.showErrorDialog(I18n.message("dialog.generate.entity.message.driverNotFound"), "Error");
        } else {
            Messages.showErrorDialog(I18n.message("dialog.generate.entity.message.driverFindFail"), "Error")
        }
        return emptySet()
    }

    DriverManager.registerDriver(
        DiverWrapper(
            Class.forName(
                jdbcDriver?.className ?: driverName, true,
                URLClassLoader(arrayOf(driverJarFile.toUri().toURL()), this.javaClass.classLoader)
            ).getConstructor()
                .newInstance() as Driver
        )
    )
    return getConnection().use { connection ->
        connection.metaData.getTables(
            catalog = catalog.takeIf { it.isNotBlank() },
            schemaPattern = if (isDDL) "public" else schemaPattern.takeIf { it.isNotBlank() },
            tableNamePattern = tableNamePattern.takeIf { it.isNotBlank() },
        )
    }
}

private fun DatabaseCache.DatabaseItem.getConnection(): Connection {
    return URI.create(uri.replace("\\", "/")).takeIf { it.scheme == "file" }?.let { ddl ->
        DriverManager.getConnection(
            "jdbc:h2:mem:jimmer-buddy-ddl;DATABASE_TO_LOWER=true;INIT=RUNSCRIPT FROM '${
                ddl.toURL().toPath().absolutePath.replace(
                    "\\",
                    "/"
                )
            }'"
        )
    } ?: let {
        DriverManager.getConnection(uri, username, password)
    }
}

private class DiverWrapper(private val driver: Driver) : Driver {
    override fun connect(url: String, info: Properties): Connection? {
        return driver.connect(url, info)
    }

    override fun acceptsURL(url: String): Boolean {
        return driver.acceptsURL(url)
    }

    override fun getPropertyInfo(url: String, info: Properties): Array<DriverPropertyInfo> {
        return driver.getPropertyInfo(url, info)
    }

    override fun getMajorVersion(): Int {
        return driver.majorVersion
    }

    override fun getMinorVersion(): Int {
        return driver.minorVersion
    }

    override fun jdbcCompliant(): Boolean {
        return driver.jdbcCompliant()
    }

    override fun getParentLogger(): Logger {
        return driver.parentLogger
    }
}