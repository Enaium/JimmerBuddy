/*
 * Copyright 2026 Enaium
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

package cn.enaium.jimmer.buddy.extensions.insight

import cn.enaium.jimmer.buddy.utility.workspace
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider
import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.SharedBypassCollector
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

/**
 * @author Enaium
 */
abstract class AbstractInlayHintsProvider : InlayHintsProvider {
    final override fun createCollector(
        file: PsiFile,
        editor: Editor
    ): SharedBypassCollector {
        return object : SharedBypassCollector {
            override fun collectFromElement(
                element: PsiElement,
                sink: InlayTreeSink
            ) {
                !element.project.workspace().isJimmerProject && return
                this@AbstractInlayHintsProvider.collectFromElement(element, sink)
            }
        }
    }

    abstract fun collectFromElement(
        element: PsiElement,
        sink: InlayTreeSink
    )
}