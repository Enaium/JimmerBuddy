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

package cn.enaium.jimmer.buddy.extensions.dto.pattern

import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement

/**
 * @author Enaium
 */
open class DtoPsiPattern<T : PsiElement, Self : PsiElementPattern<T, Self>>(klass: Class<T>) :
    PsiElementPattern<T, Self>(klass) {
    override fun getParent(element: PsiElement): PsiElement? {
        return element.parent
    }

    class Capture<T : PsiElement>(klass: Class<T>) : DtoPsiPattern<T, Capture<T>>(klass)
}