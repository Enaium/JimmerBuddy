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

package cn.enaium.jimmer.buddy.extensions.dto.editor.panel

import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiAlias
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiElement
import cn.enaium.jimmer.buddy.utility.I18n
import cn.enaium.jimmer.buddy.utility.replaceString
import com.intellij.openapi.editor.Document
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import javax.swing.JPanel

/**
 * @author Enaium
 */
class DtoAliasEditor(
    private val aliasProperty: ObservableMutableProperty<String>,
    private val alias: () -> DtoPsiAlias?,
    private val prop: DtoPsiElement,
) : DtoBaseEditor() {

    init {
        init()
    }

    override fun panel(): JPanel {
        return panel {
            row(I18n.message("editor.dto.label.alias")) {
                textField().align(Align.FILL).bindText(aliasProperty)
            }
        }
    }

    override fun save(document: Document) {
        if (aliasProperty.get().isNotEmpty()) {
            alias()?.also {
                document.replaceString(it.textRange, aliasProperty.get())
            } ?: run {
                document.insertString(prop.endOffset, " as ${aliasProperty.get()}")
            }
        } else {
            alias()?.also {
                document.replaceString(prop.endOffset, it.textRange.endOffset, "")
            }
        }
    }
}