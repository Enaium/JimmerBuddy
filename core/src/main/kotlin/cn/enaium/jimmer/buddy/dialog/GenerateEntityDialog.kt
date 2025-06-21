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
import cn.enaium.jimmer.buddy.utility.packageChooserField
import cn.enaium.jimmer.buddy.utility.relativeLocationField
import cn.enaium.jimmer.buddy.utility.I18n
import com.intellij.openapi.project.Project
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
import kotlin.io.path.*

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
            Messages.showErrorDialog(
                I18n.message("dialog.generate.entity.message.connectFail", e.message),
                "Error"
            )
            JimmerBuddy.getWorkspace(project).log.error(e)
            emptySet()
        }
    )

    init {
        title = I18n.message("dialog.generate.entity.title")
        setSize(800, 600)
        init()
    }

    override fun createCenterPanel(): JComponent {
        return borderPanel {
            addToTop(panel {
                row(I18n.message("dialog.generate.entity.label.relativePath")) {
                    relativeLocationField(project, generateEntityModel.relativePathProperty).align(Align.FILL)
                }
                row(I18n.message("dialog.generate.entity.label.packageName")) {
                    packageChooserField(project, generateEntityModel.packageNameProperty).align(Align.FILL)
                }
                row(I18n.message("dialog.generate.entity.label.language")) {
                    JimmerBuddy.Services.UI.segmentedButtonText(this, GenerateEntityModel.Language.entries) {
                        it.text
                    }.bind(generateEntityModel.languageProperty)
                }

                collapsibleGroup(I18n.message("dialog.generate.entity.group.advanced")) {
                    row {
                        checkBox(I18n.message("dialog.generate.entity.checkbox.comment")).bindSelected(
                            generateEntityModel.commentProperty
                        )
                        checkBox(I18n.message("dialog.generate.entity.checkbox.table")).bindSelected(
                            generateEntityModel.tableAnnotationProperty
                        )
                        checkBox(I18n.message("dialog.generate.entity.checkbox.column")).bindSelected(
                            generateEntityModel.columnAnnotationProperty
                        )
                        checkBox(I18n.message("dialog.generate.entity.checkbox.idView")).bindSelected(
                            generateEntityModel.idViewAnnotationProperty
                        )
                        checkBox(I18n.message("dialog.generate.entity.checkbox.joinTable")).bindSelected(
                            generateEntityModel.joinTableAnnotationProperty
                        )
                    }
                    row(I18n.message("dialog.generate.entity.label.primaryKeyName")) {
                        textField().align(Align.FILL).bindText(generateEntityModel.primaryKeyNameProperty)
                    }
                    row(I18n.message("dialog.generate.entity.label.association")) {
                        JimmerBuddy.Services.UI.segmentedButtonText(this, GenerateEntityModel.Association.entries) {
                            it.text
                        }.bind(generateEntityModel.associationProperty)
                    }
                    row(I18n.message("dialog.generate.entity.label.tableNameRegex")) {
                        textField().align(Align.FILL).bindText(generateEntityModel.tableNameRegexProperty)
                        textField().align(Align.FILL).bindText(generateEntityModel.tableNameReplaceProperty)
                    }
                    row(I18n.message("dialog.generate.entity.label.columnNameRegex")) {
                        textField().align(Align.FILL).bindText(generateEntityModel.columnNameRegexProperty)
                        textField().align(Align.FILL).bindText(generateEntityModel.columnNameReplaceProperty)
                    }
                }
            })
            addToCenter(tableTreeTable)
        }
    }

    private fun getTables(): Set<Table> {
        val uri = databaseItem.uri
        val isDDL = uri.startsWith("file:")

        var driverJarFile =
            databaseItem.driverFile.takeIf { it.isNotBlank() }?.let { Path(it).takeIf { path -> path.exists() } }
        val driverName = databaseItem.driverName.takeIf { it.isNotBlank() }

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
            } else if (databaseItem.driverFile.isNotBlank()) {
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
                catalog = databaseItem.catalog.takeIf { it.isNotBlank() },
                schemaPattern = if (isDDL) "public" else databaseItem.schemaPattern.takeIf { it.isNotBlank() },
                tableNamePattern = databaseItem.tableNamePattern.takeIf { it.isNotBlank() },
            )
        }
    }

    private fun getConnection(): Connection {
        return URI.create(databaseItem.uri.replace("\\", "/")).takeIf { it.scheme == "file" }?.let { ddl ->
            DriverManager.getConnection(
                "jdbc:h2:mem:jimmer-buddy-ddl;DATABASE_TO_LOWER=true;INIT=RUNSCRIPT FROM '${
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
            Messages.showErrorDialog(I18n.message("dialog.generate.entity.message.noSelectTable"), "Error")
            return
        }

        if (generateEntityModel.relativePath.isBlank()) {
            Messages.showErrorDialog(I18n.message("dialog.generate.entity.message.relativePathEmpty"), "Error")
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

        JimmerBuddy.getWorkspace(project).asyncRefresh(
            generate.generate(
                project,
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