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
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.util.Processor
import org.jetbrains.kotlin.asJava.toFakeLightClass

/**
 * @author Enaium
 */
class JimmerPsiShortNamesCache(val project: Project) : PsiShortNamesCache() {
    override fun getClassesByName(
        name: String,
        scope: GlobalSearchScope
    ): Array<PsiClass> {
        if (JimmerBuddy.isJavaProject(project)) {
            JimmerBuddy.init()
            return JimmerBuddy.javaAllPsiClassCache.filter { it.key.substringAfterLast(".") == name }.values.toTypedArray()
        } else if (JimmerBuddy.isKotlinProject(project)) {
            JimmerBuddy.init()
            return JimmerBuddy.kotlinAllKtClassCache.filter { it.key.substringAfterLast(".") == name }.values.mapNotNull {
                it.toFakeLightClass()
            }.toTypedArray()
        }
        return emptyArray()
    }

    override fun getAllClassNames(): Array<String> {
        if (JimmerBuddy.isJavaProject(project)) {
            JimmerBuddy.init()
            return JimmerBuddy.javaAllPsiClassCache.keys.map { it.substringAfterLast(".") }.toTypedArray()
        } else if (JimmerBuddy.isKotlinProject(project)) {
            JimmerBuddy.init()
            return JimmerBuddy.kotlinAllKtClassCache.keys.map { it.substringAfterLast(".") }.toTypedArray()
        }
        return emptyArray()
    }

    override fun getMethodsByName(
        name: String,
        scope: GlobalSearchScope
    ): Array<PsiMethod> {
        return PsiMethod.EMPTY_ARRAY
    }

    override fun getMethodsByNameIfNotMoreThan(
        name: String,
        scope: GlobalSearchScope,
        maxCount: Int
    ): Array<PsiMethod> {
        return PsiMethod.EMPTY_ARRAY
    }

    override fun getFieldsByNameIfNotMoreThan(
        name: String,
        scope: GlobalSearchScope,
        maxCount: Int
    ): Array<PsiField> {
        return PsiField.EMPTY_ARRAY
    }

    override fun processMethodsWithName(
        name: String,
        scope: GlobalSearchScope,
        processor: Processor<in PsiMethod>
    ): Boolean {
        return true
    }

    override fun getAllMethodNames(): Array<String> {
        return emptyArray()
    }

    override fun getFieldsByName(
        name: String,
        scope: GlobalSearchScope
    ): Array<PsiField> {
        return PsiField.EMPTY_ARRAY
    }

    override fun getAllFieldNames(): Array<String> {
        return emptyArray()
    }
}