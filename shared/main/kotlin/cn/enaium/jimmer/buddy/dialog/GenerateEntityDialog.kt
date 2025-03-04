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

import cn.enaium.jimmer.buddy.database.generate.JavaEntityGenerate
import cn.enaium.jimmer.buddy.database.generate.KotlinEntityGenerate
import cn.enaium.jimmer.buddy.database.model.GenerateEntityModel
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.io.isFile
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
import kotlin.io.path.name
import kotlin.io.path.walk

/**
 * @author Enaium
 */
class GenerateEntityDialog(
    private val project: Project
) : DialogWrapper(false) {

    private val generateEntityModel = GenerateEntityModel()

    init {
        title = "Generate Entity"
        isResizable = false
        init()
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row("URI:") {
                textField().bindText(generateEntityModel.uriProperty)
            }
            row("Username:") {
                textField().bindText(generateEntityModel.usernameProperty)
            }
            row("Password:") {
                passwordField().bindText(generateEntityModel.passwordProperty)
            }
            row("Relative Path:") {
                textField().bindText(generateEntityModel.relativePathProperty)
            }
            row("Package Name:") {
                textField().bindText(generateEntityModel.packageNameProperty)
            }
            row("Language:") {
                segmentedButton(GenerateEntityModel.Language.entries) {
                    it.text
                }.bind(generateEntityModel.languageProperty)
            }

            collapsibleGroup("Advanced") {
                row {
                    checkBox("Comment:").bindSelected(generateEntityModel.commentProperty)
                }
                row {
                    checkBox("Table Annotation:").bindSelected(generateEntityModel.tableAnnotationProperty)
                }
                row {
                    checkBox("Column Annotation:").bindSelected(generateEntityModel.columnAnnotationProperty)
                }
                row {
                    checkBox("Id View Annotation:").bindSelected(generateEntityModel.idViewAnnotationProperty)
                }
                row {
                    checkBox("Join Table Annotation:").bindSelected(generateEntityModel.joinTableAnnotationProperty)
                }
                row("Primary Key Name:") {
                    textField().bindText(generateEntityModel.primaryKeyNameProperty)
                }
                row("Association:") {
                    segmentedButton(GenerateEntityModel.Association.entries) {
                        it.text
                    }.bind(generateEntityModel.associationProperty)
                }
                row("Catalog:") {
                    textField().bindText(generateEntityModel.catalogProperty)
                }
                row("Schema Pattern:") {
                    textField().bindText(generateEntityModel.schemaPatternProperty)
                }
                row("Table Name Pattern:") {
                    textField().bindText(generateEntityModel.tableNamePatternProperty)
                }
            }
        }
    }

    override fun doOKAction() {
        if (generateEntityModel.uri.isBlank() || generateEntityModel.relativePath.isBlank()) {
            Messages.showErrorDialog("URI, Relative Path cannot be empty", "Error")
            return
        }

        val uri = generateEntityModel.uri

        val jdbcDriver = JdbcDriver.entries.find { uri.startsWith("jdbc:${it.scheme}") } ?: let {
            if (uri.startsWith("file:")) {
                return@let JdbcDriver.H2
            } else {
                Messages.showErrorDialog("Unsupported JDBC Driver", "Error")
                return
            }
        }

        var driverJarFile: Path? = null

        Path(System.getProperty("user.home")).resolve(
            ".m2/repository/${
                jdbcDriver.group.split(".").joinToString("/")
            }/${jdbcDriver.name}"
        ).walk().findLast {
            it.isFile() && it.name.endsWith("-sources.jar").not() && it.name.endsWith("-javadoc.jar")
                .not() && it.name.endsWith(".jar")
        }?.also {
            driverJarFile = it
        }

        Path(System.getProperty("user.home")).resolve(".gradle/caches/modules-2/files-2.1/${jdbcDriver.group}/${jdbcDriver.name}")
            .walk().findLast {
                it.isFile() && it.name.endsWith("-sources.jar").not() && it.name.endsWith("-javadoc.jar")
                    .not() && it.name.endsWith(".jar")
            }?.also {
                driverJarFile = it
            }

        if (driverJarFile == null) {
            Messages.showErrorDialog("Failed to find driver jar", "Error")
            return
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

        val generate = when (generateEntityModel.language) {
            GenerateEntityModel.Language.KOTLIN -> {
                KotlinEntityGenerate()
            }

            GenerateEntityModel.Language.JAVA -> {
                JavaEntityGenerate()
            }
        }

        val projectDir = project.guessProjectDir() ?: return

        generate.generate(projectDir.toNioPath(), generateEntityModel)
        super.doOKAction()
    }

    enum class JdbcDriver(val className: String, val group: String, name: String, val scheme: String) {
        POSTGRESQL("org.postgresql.Driver", "org.postgresql", "postgresql", "postgresql"),
        MARIADB("org.mariadb.jdbc.Driver", "org.mariadb.jdbc", "mariadb-java-client", "mariadb"),
        MYSQL("com.mysql.cj.jdbc.Driver", "com.mysql", "mysql-connector-j", "mysql"),
        SQLITE("org.sqlite.JDBC", "org.xerial", "sqlite-jdbc", "sqlite"),
        H2("org.h2.Driver", "com.h2database", "h2", "h2")
    }

    private class DiverWrapper(private val driver: Driver) : Driver {
        override fun connect(url: String, info: Properties): Connection {
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