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

package cn.enaium.jimmer.buddy.extensions.console

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.dialog.ExecuteSqlDialog
import cn.enaium.jimmer.buddy.utility.invokeLater
import com.intellij.codeInsight.hints.presentation.InputHandler
import com.intellij.execution.filters.ConsoleFilterProvider
import com.intellij.execution.filters.Filter
import com.intellij.execution.impl.InlayProvider
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import java.awt.Cursor
import java.awt.Graphics
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.MouseEvent

/**
 * @author Enaium
 */
class ExecuteSqlFilter : Filter, ConsoleFilterProvider {

    val sql = "SQL: "
    val start = "Execute SQL===>"
    val end = "<===Execute SQL"

    override fun applyFilter(line: String, offset: Int): Filter.Result? {
        if (line.contains(start)) {
            return Filter.Result(
                listOf(
                    Result(offset),
                    Filter.ResultItem(0, 0, null)
                )
            )
        }
        return null
    }

    override fun getDefaultFilters(project: Project): Array<out Filter> {
        return arrayOf(this)
    }

    inner class Result(val offset: Int) : Filter.Result(offset, offset, null), InlayProvider {
        override fun createInlayRenderer(editor: Editor): EditorCustomElementRenderer {
            return Renderer(editor, offset)
        }
    }

    inner class Renderer(val editor: Editor, val offset: Int) : EditorCustomElementRenderer, InputHandler {

        var hovered = false

        override fun calcWidthInPixels(inlay: Inlay<*>): Int {
            return 24
        }

        private fun getExecuteSql(): String {
            val sqlOffset = editor.document.text.indexOf(sql, offset)
            val endOffset = editor.document.text.indexOf(end, offset)
            val text = editor.document.getText(TextRange(sqlOffset, endOffset))
            return text.lines().let { it.subList(0, it.size - 3) }.joinToString("\n").substring(5)
        }

        override fun mouseClicked(event: MouseEvent, translated: Point) {
            invokeLater {
                editor.project?.also { project ->
                    ExecuteSqlDialog(project, getExecuteSql()).show()
                }
            }
        }

        override fun mouseMoved(event: MouseEvent, translated: Point) {
            hovered = true
            (editor as EditorImpl).setCustomCursor(this, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
        }

        override fun mouseExited() {
            hovered = false
            (editor as EditorImpl).setCustomCursor(this, null)
        }

        override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
            JimmerBuddy.Icons.LOGO_NORMAL.paintIcon(inlay.editor.component, g, targetRegion.x, targetRegion.y)
        }
    }
}