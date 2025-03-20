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
import cn.enaium.jimmer.buddy.JimmerBuddy.PSI_SHARED
import cn.enaium.jimmer.buddy.utility.hasImmutableAnnotation
import cn.enaium.jimmer.buddy.utility.toAny
import cn.enaium.jimmer.buddy.utility.toImmutable
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.util.PsiUtil
import org.babyfish.jimmer.sql.ManyToMany
import org.babyfish.jimmer.sql.OneToMany
import org.jetbrains.kotlin.psi.KtClass

/**
 * @author Enaium
 */
class ImmutableLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (element is PsiClass && element.hasImmutableAnnotation() || element is KtClass && element.hasImmutableAnnotation()) {
            element.identifyingElement?.also {
                result.add(
                    NavigationGutterIconBuilder.create(JimmerBuddy.Icons.IMMUTABLE).setTargets()
                        .createLineMarkerInfo(it)
                )
            }
            if (element is PsiClass) {
                val immutableType = element.toImmutable()
                element.methods.forEach { method ->
                    method.identifyingElement?.also {
                        result.add(
                            NavigationGutterIconBuilder.create(JimmerBuddy.Icons.PROP)
                                .also { nav ->
                                    immutableType.declaredProps[method.name]?.let {
                                        if (it.context().getImmutableType(it.elementType)
                                                ?.let { it.isEntity || it.isEmbeddable } == true
                                        ) {
                                            method.returnType?.let { PsiUtil.resolveGenericsClassInType(it) }
                                        } else {
                                            null
                                        }
                                    }?.let {
                                        if (it.substitutor != PsiSubstitutor.EMPTY) {
                                            it.element?.typeParameters?.firstOrNull()?.let { parameter ->
                                                PsiUtil.resolveGenericsClassInType(it.substitutor.substitute(parameter)).element
                                            }
                                        } else {
                                            it.element
                                        }
                                    }?.also {
                                        nav.setTargets(
                                            listOfNotNull(
                                                it,
                                                it.methods.find {
                                                    it.name == method.let {
                                                        it.modifierList.annotations.find { it.qualifiedName == OneToMany::class.qualifiedName || it.qualifiedName == ManyToMany::class.qualifiedName }
                                                    }?.findAttributeValue("mappedBy")?.toAny(String::class.java)
                                                        ?.toString()
                                                }
                                            )
                                        )
                                    } ?: nav.setTargets()
                                }
                                .createLineMarkerInfo(it)
                        )
                    }
                }
            } else if (element is KtClass) {
                val immutableType = element.toImmutable()
                element.getProperties().forEach { property ->
                    property.identifyingElement?.also {
                        result.add(
                            NavigationGutterIconBuilder.create(JimmerBuddy.Icons.PROP).setTargets()
                                .also { nav ->
                                    immutableType.declaredProperties[property.name]?.let {
                                        if (it.targetType?.let { it.isImmutable || it.isEntity || it.isEmbeddable } == true) {
                                            property.typeReference?.let { PSI_SHARED.type(it) }
                                        } else {
                                            null
                                        }
                                    }?.let {
                                        if (it.arguments.isNotEmpty()) {
                                            it.arguments.firstOrNull()?.ktClass
                                        } else {
                                            it.ktClass
                                        }
                                    }?.also {
                                        nav.setTargets(
                                            listOfNotNull(
                                                it,
                                                it.getProperties().find {
                                                    it.name == property.let {
                                                        PSI_SHARED.annotations(it)
                                                            .find { it.fqName == OneToMany::class.qualifiedName || it.fqName == ManyToMany::class.qualifiedName }
                                                    }?.arguments?.find { it.name == "mappedBy" }?.value?.toString()
                                                }
                                            )
                                        )
                                    } ?: nav.setTargets()
                                }
                                .createLineMarkerInfo(it)
                        )
                    }
                }
            }
        }
        super.collectNavigationMarkers(element, result)
    }
}