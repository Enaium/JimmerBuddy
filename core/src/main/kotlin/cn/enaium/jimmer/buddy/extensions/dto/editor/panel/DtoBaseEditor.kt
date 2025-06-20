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

import com.intellij.openapi.editor.Document
import java.awt.BorderLayout
import javax.swing.JPanel

/**
 * @author Enaium
 */
abstract class DtoBaseEditor : JPanel(BorderLayout()) {

    private val panel by lazy { panel() }

    fun init() {
        add(panel, BorderLayout.CENTER)
    }

    abstract fun panel(): JPanel

    abstract fun save(document: Document)
}