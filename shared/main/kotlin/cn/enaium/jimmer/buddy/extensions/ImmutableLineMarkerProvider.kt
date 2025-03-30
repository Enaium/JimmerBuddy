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
import cn.enaium.jimmer.buddy.utility.*
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.util.PsiUtil
import org.babyfish.jimmer.Formula
import org.babyfish.jimmer.sql.IdView
import org.babyfish.jimmer.sql.ManyToMany
import org.babyfish.jimmer.sql.OneToMany
import org.babyfish.jimmer.sql.OneToOne
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.findPropertyByName

/**
 * @author Enaium
 */
class ImmutableLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    ) {
        if (element is PsiClass && element.hasImmutableAnnotation() || element is KtClass && element.hasImmutableAnnotation()) {
            element.identifyingElement?.also {
                result.add(
                    NavigationGutterIconBuilder.create(JimmerBuddy.Icons.IMMUTABLE).setTargets()
                        .createLineMarkerInfo(it)
                )
            }
            if (element is PsiClass) {
                val immutableType = try {
                    element.toImmutable()
                } catch (_: Throwable) {
                    return
                }
                element.methods.forEach { method ->
                    method.identifyingElement?.also {
                        result.add(
                            NavigationGutterIconBuilder.create(JimmerBuddy.Icons.PROP)
                                .also { nav ->
                                    val targets = mutableListOf<PsiElement>()
                                    immutableType.declaredProps[method.name]?.also {
                                        val annotations = method.modifierList.annotations
                                        targets.addAll((annotations.find { it.qualifiedName == Formula::class.qualifiedName }
                                            ?.findAttributeValue("dependencies")
                                            ?.toAny(Array<String>::class.java) as? Array<*>)?.map {
                                            val dependency = it.toString()
                                            val trace = dependency.split(".")
                                            var containingClass = method.containingClass
                                            var methods = arrayOf<PsiMethod>()
                                            trace.forEachIndexed { index, item ->
                                                if (index != trace.size - 1) {
                                                    containingClass?.findMethodsByName(item, true)
                                                        ?.takeIf { it.isNotEmpty() }
                                                        ?.also { containingClass = it.first().getTarget() }
                                                } else {
                                                    methods = containingClass?.findMethodsByName(item, true)
                                                        ?: arrayOf<PsiMethod>()
                                                }
                                            }
                                            methods.toList()
                                        }?.flatten()?.toList() ?: emptyList())
                                        annotations.find { it.qualifiedName == IdView::class.qualifiedName }
                                            ?.findAttributeValue("value")?.toAny(String::class.java)?.toString()
                                            ?.takeIf { it.isNotBlank() }?.also {
                                                targets.addAll(element.findMethodsByName(it, false).toList())
                                            } ?: run {

                                            method.name.takeIf { it.endsWith("Id") }?.also {
                                                targets.addAll(
                                                    element.findMethodsByName(
                                                        it.substring(0, it.length - 2),
                                                        false
                                                    )
                                                )
                                            }
                                        }
                                    }?.let {
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
                                    }?.also { psiClass ->
                                        targets.add(psiClass)
                                        method.let {
                                            it.modifierList.annotations.find { it.qualifiedName == OneToMany::class.qualifiedName || it.qualifiedName == ManyToMany::class.qualifiedName || it.qualifiedName == OneToOne::class.qualifiedName }
                                        }?.findAttributeValue("mappedBy")?.toAny(String::class.java)
                                            ?.toString()?.also {
                                                targets.addAll(psiClass.findMethodsByName(it, false))
                                            }
                                    }
                                    nav.setTargets(targets)
                                }
                                .createLineMarkerInfo(it)
                        )
                    }
                }
            } else if (element is KtClass) {
                val immutableType = try {
                    element.toImmutable()
                } catch (_: Throwable) {
                    return
                }
                element.getProperties().forEach { property ->
                    property.identifyingElement?.also {
                        result.add(
                            NavigationGutterIconBuilder.create(JimmerBuddy.Icons.PROP).setTargets()
                                .also { nav ->
                                    val targets = mutableListOf<PsiElement>()
                                    immutableType.declaredProperties[property.name]?.also {
                                        val annotations = property.annotations()
                                        targets.addAll(
                                            (annotations
                                                .find { it.fqName == Formula::class.qualifiedName }?.arguments?.find { it.name == "dependencies" }?.value as? List<*>)?.mapNotNull {
                                                val dependency = it.toString()
                                                val trace = dependency.split(".")
                                                var containingClass = property.containingClass()
                                                var ktProperty: KtNamedDeclaration? = null
                                                trace.forEachIndexed { index, item ->
                                                    if (index != trace.size - 1) {
                                                        containingClass?.findPropertyByName(item, true)?.also {
                                                            containingClass = (it as KtProperty).getTarget()
                                                        }
                                                    } else {
                                                        ktProperty = containingClass?.findPropertyByName(item, true)
                                                    }
                                                }
                                                ktProperty
                                            } ?: emptyList())


                                        annotations.find { it.fqName == IdView::class.qualifiedName }?.arguments?.find { it.name == "value" }?.value?.toString()
                                            ?.also {
                                                element.findPropertyByName(it)?.also {
                                                    targets.add(it)
                                                }
                                            } ?: run {
                                            it.name.takeIf { it.endsWith("Id") }?.also {
                                                element.findPropertyByName(it.substring(0, it.length - 2))?.also {
                                                    targets.add(it)
                                                }
                                            }
                                        }
                                    }?.let {
                                        if (it.targetType?.let { it.isImmutable || it.isEntity || it.isEmbeddable } == true) {
                                            property.typeReference?.type()
                                        } else {
                                            null
                                        }
                                    }?.let {
                                        if (it.arguments.isNotEmpty()) {
                                            it.arguments.firstOrNull()?.ktClass
                                        } else {
                                            it.ktClass
                                        }
                                    }?.also { ktClass ->
                                        targets.add(ktClass)
                                        property.annotations()
                                            .find { it.fqName == OneToMany::class.qualifiedName || it.fqName == ManyToMany::class.qualifiedName || it.fqName == OneToOne::class.qualifiedName }?.arguments?.find { it.name == "mappedBy" }?.value?.toString()
                                            ?.also {
                                                ktClass.findPropertyByName(it, false)?.also {
                                                    targets.add(it)
                                                }
                                            }
                                    }
                                    nav.setTargets(targets)
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