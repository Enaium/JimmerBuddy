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

package cn.enaium.jimmer.buddy.extensions.insight.template.postfix

import cn.enaium.jimmer.buddy.utility.isImmutable
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateProvider
import com.intellij.codeInsight.template.postfix.templates.StringBasedPostfixTemplate
import com.intellij.codeInsight.template.postfix.util.JavaPostfixTemplatesUtils.selectorAllExpressionsWithCurrentOffset
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceExpression

/**
 * @author Enaium
 */
open class JavaImmediatePostfixTemplate(
    provider: PostfixTemplateProvider,
    name: String,
    val method: String
) : StringBasedPostfixTemplate(
    name,
    "sql.${method}(\$expr.class)",
    selectorAllExpressionsWithCurrentOffset { expression ->
        if (expression is PsiReferenceExpression) {
            return@selectorAllExpressionsWithCurrentOffset (expression.advancedResolve(true).element as? PsiClass)?.isImmutable() == true
        }
        return@selectorAllExpressionsWithCurrentOffset false
    },
    provider
), DumbAware {
    override fun getTemplateString(element: PsiElement): String? {
        return "sql.$method(\$expr$.class, \$END$);"
    }
}