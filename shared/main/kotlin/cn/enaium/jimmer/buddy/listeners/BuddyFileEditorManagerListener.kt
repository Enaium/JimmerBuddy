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

package cn.enaium.jimmer.buddy.listeners

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.utility.isDumb
import cn.enaium.jimmer.buddy.utility.isJavaProject
import cn.enaium.jimmer.buddy.utility.isKotlinProject
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Path
import kotlin.io.path.extension

/**
 * @author Enaium
 */
class BuddyFileEditorManagerListener(val project: Project) : FileEditorManagerListener {
    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        try {
            fileChange(file.toNioPath())
        } catch (_: Throwable) {

        }
    }

    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        try {
            fileChange(file.toNioPath())
        } catch (_: Throwable) {

        }
    }

    override fun selectionChanged(event: FileEditorManagerEvent) {
        event.oldFile?.also {
            try {
                fileChange(it.toNioPath())
            } catch (_: Throwable) {

            }
        }
    }

    private fun fileChange(file: Path) {
        val dtoFile = file.takeIf { it.extension == "dto" } ?: return
        JimmerBuddy.DEQ.schedule("EditorChange") {
            if (project.isDumb()) return@schedule
            if (project.isJavaProject()) {
                JimmerBuddy.getWorkspace(project).also {
                    it.init()
                    it.dtoProcessJava(
                        JimmerBuddy.GenerateProject.generate(
                            dtoFile,
                            JimmerBuddy.GenerateProject.Language.DTO
                        )
                    )
                }
            } else if (project.isKotlinProject()) {
                JimmerBuddy.getWorkspace(project).also {
                    it.init()
                    it.dtoProcessKotlin(
                        JimmerBuddy.GenerateProject.generate(
                            dtoFile,
                            JimmerBuddy.GenerateProject.Language.DTO
                        )
                    )
                }
            }
        }
    }
}