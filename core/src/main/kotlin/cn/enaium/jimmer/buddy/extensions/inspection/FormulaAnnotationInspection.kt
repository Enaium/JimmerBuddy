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
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.findParentOfType
import org.babyfish.jimmer.Formula
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.toUElementOfType

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
                                dependency = dependency.substringBeforeLast(".")
                                dependencies.add(dependency)
                            }
                            dependencies
                        }?.flatten()?.toSet()

                val trace = element.getImmutableTrace().takeIf { it.isNotEmpty() } ?: return
                val dependency = trace.joinToString(".")
                if (dependencies?.contains(dependency) != true) {
                    holder.registerProblem(
                        element,
                        I18n.message("inspection.annotation.formula.dependencyNotFound"),
                        AddFormulaDependencyQuickFix(dependency)
                    )
                }
            }
        }
    }

    private fun ktDependencies(element: PsiElement, holder: ProblemsHolder) {
        if (element is KtNameReferenceExpression && element.findParentOfType<KtPropertyAccessor>() != null) {
            (element.reference?.resolve() as? KtProperty)?.containingClass()?.isImmutable() != true && return
            element.findParentOfType<KtProperty>()?.also { property ->
                val dependencies = (property.annotations().find { it.fqName == Formula::class.qualifiedName }
                    ?.findArgument("dependencies")?.value as? List<*>)?.map { it.toString() }?.map {
                    val dependencies = mutableListOf<String>()
                    dependencies.add(it)
                    var dependency = it
                    while (dependency.contains(".")) {
                        dependency = dependency.substringBeforeLast(".")
                        dependencies.add(dependency)
                    }
                    dependencies
                }?.flatten()?.toSet()

                val parent = element.parent
                if (parent is KtQualifiedExpression) {
                    val trace = parent.getImmutableTrace().takeIf { it.isNotEmpty() } ?: return
                    val dependency = trace.joinToString(".")
                    if (dependencies?.contains(dependency) != true) {
                        holder.registerProblem(
                            parent,
                            I18n.message("inspection.annotation.formula.dependencyNotFound"),
                            AddFormulaDependencyQuickFix(dependency)
                        )
                    }
                } else if (element.findParentOfType<KtQualifiedExpression>() == null) {
                    val dependency = element.getReferencedName()
                    if (dependencies?.contains(dependency) != true) {
                        holder.registerProblem(
                            parent,
                            I18n.message("inspection.annotation.formula.dependencyNotFound"),
                            AddFormulaDependencyQuickFix(dependency)
                        )
                    }
                }
            }
        }
    }
}

private class AddFormulaDependencyQuickFix(private val dependency: String) : LocalQuickFix {
    override fun getFamilyName(): String {
        return I18n.message("inspection.annotation.formula.addDependency.family")
    }

    override fun getName(): String {
        return I18n.message("inspection.annotation.formula.addDependency", dependency)
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        descriptor.psiElement.getParentOfType<PsiMethod>(true)?.also { method ->
            addJavaDependency(project, method)
            return
        }

        descriptor.psiElement.getParentOfType<KtProperty>(true)?.also { property ->
            addKotlinDependency(project, property)
        }
    }

    private fun addJavaDependency(project: Project, method: PsiMethod) {
        val annotation = method.modifierList.annotations
            .find { it.hasQualifiedName(Formula::class.qualifiedName!!) }
            ?: return
        val dependencies = annotation.readJavaDependencies().addDependency()
        val annotationText = "@${Formula::class.qualifiedName}(dependencies = ${dependencies.toJavaArrayText()})"
        val annotationTemplate = JavaPsiFacade.getElementFactory(project)
            .createAnnotationFromText(annotationText, annotation)
        val value = annotationTemplate.findAttributeValue("dependencies")
            ?: return
        annotation.setDeclaredAttributeValue("dependencies", value)
    }

    private fun addKotlinDependency(project: Project, property: KtProperty) {
        val annotation = property.annotationEntries
            .find { it.toUElementOfType<UAnnotation>()?.qualifiedName == Formula::class.qualifiedName }
            ?: return
        val dependencies = annotation.readKotlinDependencies().addDependency()
        val newText = annotation.replaceKotlinDependencies(dependencies)
        val newAnnotation = KtPsiFactory(project).createAnnotationEntry(newText)
        annotation.replace(newAnnotation)
    }

    private fun Collection<String>.addDependency(): List<String> {
        return (this + dependency).distinct()
    }
}

private fun PsiAnnotation.readJavaDependencies(): List<String> {
    val value = findAttributeValue("dependencies") ?: return emptyList()
    return when (value) {
        is PsiArrayInitializerMemberValue -> value.initializers.mapNotNull { it.toDependencyText() }
        else -> listOfNotNull(value.toDependencyText())
    }
}

private fun PsiAnnotationMemberValue.toDependencyText(): String? {
    return (this as? PsiLiteralExpression)?.value?.toString()
}

private fun KtAnnotationEntry.readKotlinDependencies(): List<String> {
    return valueArguments
        .find { it.getArgumentName()?.asName?.asString() == "dependencies" }
        ?.getArgumentExpression()
        ?.text
        ?.let { dependencyStringRegex.findAll(it).map { match -> match.groupValues[1] }.toList() }
        ?: emptyList()
}

private fun KtAnnotationEntry.replaceKotlinDependencies(dependencies: List<String>): String {
    val dependenciesText = dependencies.toKotlinArrayText()
    val dependencyArgument = valueArguments
        .find { it.getArgumentName()?.asName?.asString() == "dependencies" }
    val expression = dependencyArgument?.getArgumentExpression()
    if (expression != null) {
        val startOffset = expression.textRange.startOffset - textRange.startOffset
        val endOffset = expression.textRange.endOffset - textRange.startOffset
        return text.replaceRange(startOffset, endOffset, dependenciesText)
    }

    val argumentList = valueArgumentList ?: return "$text(dependencies = $dependenciesText)"
    val startOffset = argumentList.textRange.startOffset - textRange.startOffset + 1
    val endOffset = argumentList.textRange.endOffset - textRange.startOffset - 1
    if (valueArguments.isEmpty()) {
        return text.replaceRange(startOffset, endOffset, "dependencies = $dependenciesText")
    }

    return text.replaceRange(endOffset, endOffset, ", dependencies = $dependenciesText")
}

private fun List<String>.toJavaArrayText(): String {
    return joinToString(prefix = "{", postfix = "}") { "\"${it.escapeAnnotationString()}\"" }
}

private fun List<String>.toKotlinArrayText(): String {
    return joinToString(prefix = "[", postfix = "]") { "\"${it.escapeAnnotationString()}\"" }
}

private fun String.escapeAnnotationString(): String {
    return replace("\\", "\\\\").replace("\"", "\\\"")
}

private val dependencyStringRegex = Regex(""""([^"\\]*(?:\\.[^"\\]*)*)"""")

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
