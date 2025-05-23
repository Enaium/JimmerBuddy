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

package cn.enaium.jimmer.buddy.extensions.reference

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/**
 * @author Enaium
 */
class JimmerReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(register: PsiReferenceRegistrar) {
        val pattern =
            PlatformPatterns.or(
                PlatformPatterns.psiElement(PsiLiteral::class.java)
                    .inside(PsiAnnotation::class.java),
                PlatformPatterns.psiElement(KtStringTemplateExpression::class.java)
                    .inside(KtAnnotationEntry::class.java)
            )
        register.registerReferenceProvider(pattern, MappedByPsiReferenceProvider)
        register.registerReferenceProvider(pattern, IdViewPsiReferenceProvider)
        register.registerReferenceProvider(pattern, FormulaPsiReferenceProvider)
        register.registerReferenceProvider(pattern, FetchByPsiReferenceProvider)
        register.registerReferenceProvider(pattern, OrderedPropPsiReferenceProvider)
    }
}