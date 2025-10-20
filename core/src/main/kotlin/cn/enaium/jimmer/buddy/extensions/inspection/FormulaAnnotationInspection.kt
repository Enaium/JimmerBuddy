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

package cn.enaium.jimmer.buddy.extensions.inspection

import cn.enaium.jimmer.buddy.utility.*
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import com.intellij.psi.util.findParentOfType
import org.babyfish.jimmer.Formula
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

/**
 * @author Enaium
 */
class FormulaAnnotationInspection : AbstractLocalInspectionTool() {
    override fun visit(
        element: PsiElement,
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ) {
        if (!element.inImmutable()) {
            return
        }

        if (element.isAnnotation(Formula::class.qualifiedName!!)) {
            if (element is PsiAnnotation) {
                javaAnnotation(element, holder)
            }
            if (element is KtAnnotationEntry) {
                ktAnnotation(element, holder)
            }
        }

        element.findParentOfType<PsiClass>()?.also { psiClass ->
            javaDependencies(element, holder)
        }
        element.findParentOfType<KtClass>()?.also { ktClass ->
            ktDependencies(element, holder)
        }
    }

    private fun javaDependencies(element: PsiElement, holder: ProblemsHolder) {
        if (element is PsiMethodCallExpression && element.findParentOfType<PsiMethod>() != null) {
            val resolveMethod = element.resolveMethod()
            resolveMethod?.containingClass?.isImmutable() != true && return
            element.findParentOfType<PsiMethod>()?.also { method ->
                val dependencies =
                    method.modifierList.annotations.find { it.hasQualifiedName(Formula::class.qualifiedName!!) }
                        ?.findAttributeValue("dependencies")?.let {
                            it.toAny(Array<String>::class.java) as? Array<*> ?: it.toAny(String::class.java)
                                ?.let { string -> arrayOf(string.toString()) }
                        }?.map { it.toString() }
                        ?.takeIf { it.isNotEmpty() }?.map {
                            val dependencies = mutableListOf<String>()
                            dependencies.add(it)
                            var dependency = it
                            while (dependency.contains(".")) {
                                dependency = it.substringBeforeLast(".")
                                dependencies.add(dependency)
                            }
                            dependencies
                        }?.flatten()?.toSet()

                 val trace = element.getImmutableTrace().takeIf { it.isNotEmpty() } ?: return
                if (dependencies?.contains(trace.joinToString(".")) != true) {
                    holder.registerProblem(element, I18n.message("inspection.annotation.formula.dependencyNotFound"))
                }
            }
        }
    }

    private fun ktDependencies(element: PsiElement, holder: ProblemsHolder) {
        if (element is KtNameReferenceExpression && element.findParentOfType<KtPropertyAccessor>() != null) {
            (element.reference?.resolve() as? KtProperty)?.containingClass()?.isImmutable() != true && return
            element.findParentOfType<KtProperty>()?.also { property ->
                val dependencies = (property.annotations().find { it.fqName == Formula::class.qualifiedName }
                    ?.findArgument("dependencies")?.value as? List<String>)?.map {
                    val dependencies = mutableListOf<String>()
                    dependencies.add(it)
                    var dependency = it
                    while (dependency.contains(".")) {
                        dependency = it.substringBeforeLast(".")
                        dependencies.add(dependency)
                    }
                    dependencies
                }?.flatten()?.toSet()

                val parent = element.parent
                if (parent is KtQualifiedExpression) {
                    val trace = parent.getImmutableTrace().takeIf { it.isNotEmpty() } ?: return
                    if (dependencies?.contains(trace.joinToString(".")) != true) {
                        holder.registerProblem(parent, I18n.message("inspection.annotation.formula.dependencyNotFound"))
                    }
                } else if (element.findParentOfType<KtQualifiedExpression>() == null) {
                    if (dependencies?.contains(element.getReferencedName()) != true) {
                        holder.registerProblem(parent, I18n.message("inspection.annotation.formula.dependencyNotFound"))
                    }
                }
            }
        }
    }
}

private fun ktAnnotation(
    element: KtAnnotationEntry,
    holder: ProblemsHolder
) {
    element.getParentOfType<KtProperty>(true)?.also { propertyElement ->
        val dependencies =
            (propertyElement.annotations().find { it.fqName == Formula::class.qualifiedName }
                ?.findArgument("dependencies")?.value as? List<*>)?.map { it.toString() }
                ?: run {
                    propertyElement.getter?.also {
                        holder.registerProblem(
                            element,
                            I18n.message("inspection.annotation.formula.dependenciesEmpty")
                        )
                    }
                    return@also
                }
        dependencies.forEach { dependency ->
            val trace = dependency.split(".")
            var containingClass = element.containingClass()
            trace.forEach {
                containingClass?.findPropertyByName(it, true)?.also {
                    containingClass = (it as KtProperty).getTarget()
                } ?: run {
                    holder.registerProblem(
                        element,
                        I18n.message("inspection.annotation.formula.dependencyNotExist", it)
                    )
                    return@also
                }
            }
        }
    }
}

private fun javaAnnotation(
    element: PsiAnnotation,
    holder: ProblemsHolder
) {
    element.getParentOfType<PsiMethod>(true).also { methodElement ->
        val dependencies = (element.findAttributeValue("dependencies")
            ?.let {
                it.toAny(Array<String>::class.java) as? Array<*> ?: it.toAny(String::class.java)
                    ?.let { string -> arrayOf(string.toString()) }
            })?.map { it.toString() }
            ?.takeIf { it.isNotEmpty() } ?: run {
            methodElement?.body?.also {
                holder.registerProblem(
                    element,
                    I18n.message("inspection.annotation.formula.dependenciesEmpty")
                )
            }
            return@also
        }

        dependencies.forEach { dependency ->
            val trace = dependency.split(".")
            var containingClass = methodElement?.containingClass

            trace.forEach {
                containingClass?.findMethodsByName(it, true)?.takeIf { it.isNotEmpty() }?.also {
                    containingClass = it.first().getTarget()
                } ?: run {
                    holder.registerProblem(
                        element,
                        I18n.message("inspection.annotation.formula.dependencyNotExist", it)
                    )
                    return@also
                }
            }
        }
    }
}