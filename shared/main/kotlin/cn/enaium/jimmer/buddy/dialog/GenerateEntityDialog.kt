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

package cn.enaium.jimmer.buddy.dialog

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.database.generate.JavaEntityGenerate
import cn.enaium.jimmer.buddy.database.generate.KotlinEntityGenerate
import cn.enaium.jimmer.buddy.database.model.GenerateEntityModel
import cn.enaium.jimmer.buddy.database.model.Table
import cn.enaium.jimmer.buddy.dialog.panel.TableTreeTable
import cn.enaium.jimmer.buddy.storage.JimmerBuddySetting
import cn.enaium.jimmer.buddy.utility.getTables
import cn.enaium.jimmer.buddy.utitlity.segmentedButtonText
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.rd.util.reflection.toPath
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.borderPanel
import java.net.URI
import java.net.URLClassLoader
import java.nio.file.Path
import java.sql.Connection
import java.sql.Driver
import java.sql.DriverManager
import java.sql.DriverPropertyInfo
import java.util.*
import java.util.logging.Logger
import javax.swing.JComponent
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.walk

/**
 * @author Enaium
 */
class GenerateEntityDialog(
    private val project: Project,
    private val databaseItem: JimmerBuddySetting.DatabaseItem
) : DialogWrapper(false) {

    private val generateEntityModel = GenerateEntityModel()
    private val tableTreeTable = TableTreeTable(
        try {
            getTables()
        } catch (e: Throwable) {
            Messages.showErrorDialog("Failed to connect to database:\n${e.message}", "Error")
            JimmerBuddy.LOG.error(e)
            emptySet()
        }
    )

    init {
        title = "Generate Entity"
        setSize(800, 600)
        init()
    }

    override fun createCenterPanel(): JComponent {
        return borderPanel {
            addToTop(panel {
                row("Relative Path:") {
                    textField().align(Align.FILL).bindText(generateEntityModel.relativePathProperty)
                }
                row("Package Name:") {
                    textField().align(Align.FILL).bindText(generateEntityModel.packageNameProperty)
                }
                row("Language:") {
                    segmentedButtonText(GenerateEntityModel.Language.entries) {
                        it.text
                    }.bind(generateEntityModel.languageProperty)
                }

                collapsibleGroup("Advanced") {
                    row {
                        checkBox("Comment").bindSelected(generateEntityModel.commentProperty)
                        checkBox("@Table").bindSelected(generateEntityModel.tableAnnotationProperty)
                        checkBox("@Column").bindSelected(generateEntityModel.columnAnnotationProperty)
                        checkBox("@IdView").bindSelected(generateEntityModel.idViewAnnotationProperty)
                        checkBox("@JoinTable").bindSelected(generateEntityModel.joinTableAnnotationProperty)
                    }
                    row("Primary Key Name:") {
                        textField().align(Align.FILL).bindText(generateEntityModel.primaryKeyNameProperty)
                    }
                    row("Association:") {
                        segmentedButtonText(GenerateEntityModel.Association.entries) {
                            it.text
                        }.bind(generateEntityModel.associationProperty)
                    }
                }
            })
            addToCenter(tableTreeTable)
        }
    }

    private fun getTables(): Set<Table> {
        val uri = databaseItem.uri

        val jdbcDriver = JdbcDriver.entries.find { uri.startsWith("jdbc:${it.scheme}") } ?: let {
            if (uri.startsWith("file:")) {
                return@let JdbcDriver.H2
            } else {
                Messages.showErrorDialog(
                    "Unsupported JDBC Driver(${JdbcDriver.entries.joinToString(", ") { it.scheme }})",
                    "Error"
                )
                return emptySet()
            }
        }

        var driverJarFile: Path? = null

        Path(System.getProperty("user.home")).resolve(
            ".m2/repository/${
                jdbcDriver.group.split(".").joinToString("/")
            }/${jdbcDriver.artifact}"
        ).walk().findLast {
            it.isDirectory().not() && it.name.endsWith("-sources.jar").not() && it.name.endsWith("-javadoc.jar")
                .not() && it.name.endsWith(".jar")
        }?.also {
            driverJarFile = it
        }

        Path(System.getProperty("user.home")).resolve(".gradle/caches/modules-2/files-2.1/${jdbcDriver.group}/${jdbcDriver.artifact}")
            .walk().findLast {
                it.isDirectory().not() && it.name.endsWith("-sources.jar").not() && it.name.endsWith("-javadoc.jar")
                    .not() && it.name.endsWith(".jar")
            }?.also {
                driverJarFile = it
            }

        if (driverJarFile == null) {
            Messages.showErrorDialog("Failed to find driver jar, please check your maven or gradle cache", "Error")
            return emptySet()
        }

        DriverManager.registerDriver(
            DiverWrapper(
                Class.forName(
                    jdbcDriver.className, true,
                    URLClassLoader(arrayOf(driverJarFile.toUri().toURL()), this.javaClass.classLoader)
                ).getConstructor()
                    .newInstance() as Driver
            )
        )
        return getConnection().use {
            it.metaData.getTables(
                catalog = databaseItem.catalog.takeIf { it.isNotBlank() },
                schemaPattern = databaseItem.schemaPattern.takeIf { it.isNotBlank() },
                tableNamePattern = databaseItem.tableNamePattern.takeIf { it.isNotBlank() },
            )
        }
    }


    private fun getConnection(): Connection {
        return URI.create(databaseItem.uri).takeIf { it.scheme == "file" }?.let { ddl ->
            DriverManager.getConnection(
                "jdbc:h2:mem:test;DATABASE_TO_LOWER=true;INIT=RUNSCRIPT FROM '${
                    ddl.toURL().toPath().absolutePath.replace(
                        "\\",
                        "/"
                    )
                }'"
            )
        } ?: let {
            DriverManager.getConnection(
                databaseItem.uri,
                databaseItem.username,
                databaseItem.password
            )
        }
    }

    override fun doOKAction() {

        val result = tableTreeTable.getResult().takeIf { it.isNotEmpty() } ?: run {
            Messages.showErrorDialog("Please select table", "Error")
            return
        }

        if (generateEntityModel.relativePath.isBlank()) {
            Messages.showErrorDialog("Relative Path cannot be empty", "Error")
            return
        }

        val generate = when (generateEntityModel.language) {
            GenerateEntityModel.Language.KOTLIN -> {
                KotlinEntityGenerate()
            }

            GenerateEntityModel.Language.JAVA -> {
                JavaEntityGenerate()
            }
        }

        val projectDir = project.guessProjectDir() ?: return
        JimmerBuddy.asyncRefresh(
            generate.generate(
                projectDir.toNioPath(),
                generateEntityModel,
                result
            )
        )
        super.doOKAction()
    }

    enum class JdbcDriver(val className: String, val group: String, val artifact: String, val scheme: String) {
        POSTGRESQL("org.postgresql.Driver", "org.postgresql", "postgresql", "postgresql"),
        MARIADB("org.mariadb.jdbc.Driver", "org.mariadb.jdbc", "mariadb-java-client", "mariadb"),
        MYSQL("com.mysql.cj.jdbc.Driver", "com.mysql", "mysql-connector-j", "mysql"),
        SQLITE("org.sqlite.JDBC", "org.xerial", "sqlite-jdbc", "sqlite"),
        H2("org.h2.Driver", "com.h2database", "h2", "h2")
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
}