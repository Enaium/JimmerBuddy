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

import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiAliasPattern
import cn.enaium.jimmer.buddy.utility.I18n
import cn.enaium.jimmer.buddy.utility.replaceString
import com.intellij.openapi.editor.Document
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import javax.swing.JPanel

/**
 * @author Enaium
 */
class DtoAliasPatternEditor(
    private val originProperty: ObservableMutableProperty<String>,
    private val replacementProperty: ObservableMutableProperty<String>,
    private val aliasPattern: DtoPsiAliasPattern
) : DtoBaseEditor() {

    init {
        init()
    }

    override fun panel(): JPanel {
        return panel {
            row(I18n.message("editor.dto.label.origin")) {
                textField().align(Align.FILL).bindText(originProperty)
            }
            row {
                label("->")
            }
            row(I18n.message("editor.dto.label.replacement")) {
                textField().align(Align.FILL).bindText(replacementProperty)
            }
        }
    }

    override fun save(document: Document) {
        val origin = originProperty.get()
        val replacement = replacementProperty.get()
        if (origin.matches(Regex("^[A-Za-z]+$|^\\^$")) && replacement.matches(Regex("^[A-Za-z]+$|^\\$$"))) {
            document.replaceString(aliasPattern.textRange, "as($origin -> $replacement)")
        }
    }
}