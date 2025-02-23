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

package cn.enaium.jimmer.buddy.listener

import cn.enaium.jimmer.buddy.JimmerBuddy
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

/**
 * @author Enaium
 */
class FileChangeListener(val project: Project) : BulkFileListener {
    override fun after(events: List<VFileEvent>) {
        events.isEmpty() && return
        if (JimmerBuddy.isJavaProject(project)) {
            JimmerBuddy.init()
            JimmerBuddy.sourcesProcessJava(
                project,
                events.filter { it.file?.extension == "java" }.map { it.file!!.toNioPath() })
            JimmerBuddy.dtoProcessJava(
                project,
                events.filter { it.file?.extension == "dto" }.map { it.file!!.toNioPath() })
        } else if (JimmerBuddy.isKotlinProject(project)) {
            JimmerBuddy.init()
            JimmerBuddy.sourceProcessKotlin(
                project,
                events.filter { it.file?.extension == "kt" }.map { it.file!!.toNioPath() })
            JimmerBuddy.dtoProcessKotlin(
                project,
                events.filter { it.file?.extension == "dto" }.map { it.file!!.toNioPath() })
        }
    }
}