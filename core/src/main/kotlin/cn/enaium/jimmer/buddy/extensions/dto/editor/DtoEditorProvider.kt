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

package cn.enaium.jimmer.buddy.extensions.dto.editor

import cn.enaium.jimmer.buddy.extensions.dto.DtoFileType
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * @author Enaium
 */
class DtoEditorProvider : FileEditorProvider, DumbAware {
    override fun accept(
        project: Project,
        file: VirtualFile
    ): Boolean {
        return file.fileType == DtoFileType
    }

    override fun createEditor(
        project: Project,
        file: VirtualFile
    ): FileEditor {
        return DtoViewerSplitEditor(
            TextEditorProvider.getInstance().createEditor(project, file) as TextEditor,
            DtoVisualFileEditor(project, file)
        )
    }

    override fun getEditorTypeId(): String {
        return "JimmerBuddy.DTO.Visual.Editor"
    }

    override fun getPolicy(): FileEditorPolicy {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR
    }
}