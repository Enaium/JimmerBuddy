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

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * @author Enaium
 */
class Log(project: Project) : ConsoleViewImpl(project, true) {
    fun info(text: String) {
        print(log(text), ConsoleViewContentType.NORMAL_OUTPUT)
    }

    fun error(e: Throwable) {
        e.printStackTrace(PrintWriter(object : ByteArrayOutputStream() {
            override fun close() {
                print(log(toString()), ConsoleViewContentType.ERROR_OUTPUT)
            }
        }))
    }

    fun warn(text: String) {
        print(log(text), ConsoleViewContentType.LOG_WARNING_OUTPUT)
    }

    fun log(text: String): String {
        return "[${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}] $text\n"
    }
}