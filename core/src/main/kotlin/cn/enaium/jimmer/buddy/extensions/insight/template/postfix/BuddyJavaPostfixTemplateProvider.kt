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

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplate

/**
 * @author Enaium
 */
class BuddyJavaPostfixTemplateProvider : AbstractBuddyPostfixTemplateProvider() {

    val builtinTemplates = setOf<PostfixTemplate>(
        JavaProducePostfixTemplate(this),
        JavaIsLoadedPostfixTemplate(this),
        JavaFromStringPostfixTemplate(this),
        JavaUnloadPostfixTemplate(this),
        JavaShowPostfixTemplate(this),
        JavaHidePostfixTemplate(this),
        JavaSetPostfixTemplate(this),
        JavaFindByIdPostfixTemplate(this),
        JavaFindByIdsPostfixTemplate(this),
        JavaFindMapByIdsPostfixTemplate(this),
        JavaFindOneByIdPostfixTemplate(this),
        JavaDeleteByIdPostfixTemplate(this),
        JavaDeleteByIdsPostfixTemplate(this),
    )

    override fun getTemplates(): Set<PostfixTemplate?> {
        return builtinTemplates
    }
}