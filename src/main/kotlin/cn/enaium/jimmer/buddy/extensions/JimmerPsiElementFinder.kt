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

package cn.enaium.jimmer.buddy.extensions

import cn.enaium.jimmer.buddy.JimmerBuddy
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.asJava.toFakeLightClass

/**
 * @author Enaium
 */
class JimmerPsiElementFinder(val project: Project) : PsiElementFinder() {
    override fun findClass(
        qualifiedName: String,
        scope: GlobalSearchScope
    ): PsiClass? {
        JimmerBuddy.init()
        return null
    }

    override fun findClasses(
        qualifiedName: String,
        scope: GlobalSearchScope
    ): Array<PsiClass> {
        return PsiClass.EMPTY_ARRAY
    }
}