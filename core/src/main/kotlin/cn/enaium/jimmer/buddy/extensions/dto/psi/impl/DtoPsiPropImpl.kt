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

package cn.enaium.jimmer.buddy.extensions.dto.psi.impl

import cn.enaium.jimmer.buddy.extensions.dto.completion.getTrace
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiNamedElement
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiProp
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiRoot
import cn.enaium.jimmer.buddy.utility.findPropertyByName
import cn.enaium.jimmer.buddy.utility.isJavaProject
import cn.enaium.jimmer.buddy.utility.isKotlinProject
import cn.enaium.jimmer.buddy.utility.toCommonImmutableType
import cn.enaium.jimmer.buddy.utility.toImmutable
import com.intellij.lang.ASTNode
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.util.findParentOfType
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.psi.KtClass

class DtoPsiPropImpl(node: ASTNode) : DtoPsiNamedElement(node), DtoPsiProp {
    override val value: String
        get() = node.text

    override fun getName(): String = value

    override fun reference(): PsiElement? {
        val trace = getTrace(this)
        val typeName = this.findParentOfType<DtoPsiRoot>()?.qualifiedName() ?: return null

        val commonImmutable = if (project.isJavaProject()) {
            JavaPsiFacade.getInstance(project).findClass(typeName, project.allScope())?.toImmutable()
                ?.toCommonImmutableType() ?: return null
        } else if (project.isKotlinProject()) {
            (KotlinFullClassNameIndex[typeName, project, project.allScope()].firstOrNull() as? KtClass)?.toImmutable()
                ?.toCommonImmutableType() ?: return null
        } else {
            return null
        }

        var currentImmutable = commonImmutable

        trace.forEach { trace ->
            currentImmutable.props().find { it.name() == trace }?.targetType()?.also {
                currentImmutable = it
            }
        }
        val prop = currentImmutable.props().find { it.name() == value } ?: return null

        JavaPsiFacade.getInstance(project).findClass(currentImmutable.qualifiedName(), project.allScope())
            ?.also { klass ->
                klass.allMethods.find { it.name == prop.name() }?.also { method ->
                    return method
                }
            }

        KotlinFullClassNameIndex[currentImmutable.qualifiedName(), project, project.allScope()].firstOrNull()
            ?.also { ktClass ->
                (ktClass as KtClass).findPropertyByName(prop.name(), true)?.also {
                    return it
                }
            }

        return null
    }
}