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
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiUtil
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.ManyToMany
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.OneToMany
import org.babyfish.jimmer.sql.OneToOne
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.resolve.ImportPath

/**
 * @author Enaium
 */
class AssociationAnnotationInspection : AbstractLocalInspectionTool() {

    private val toOneQuickFixes = createToOneQuickFixes()

    private val toManyQuickFixes = createToManyQuickFixes()

    override fun visit(
        element: PsiElement,
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ) {
        if (!element.inImmutable()) {
            return
        }

        val toManyProblem = I18n.message("inspection.annotation.association.collectionToOne")
        val noToOneProblem = I18n.message("inspection.annotation.association.withoutToOne")
        val toOneProblem = I18n.message("inspection.annotation.association.nonCollectionToMany")
        val noToManyProblem = I18n.message("inspection.annotation.association.withoutToMany")

        if (element is PsiMethod) {
            val returnPsiClass = element.returnType?.let { PsiUtil.resolveGenericsClassInType(it) }
            if (returnPsiClass?.substitutor != PsiSubstitutor.EMPTY && listOf(
                    List::class.java.name,
                    Set::class.java.name,
                    Collection::class.java.name
                ).any { it == returnPsiClass?.element?.qualifiedName }
            ) {
                if (element.hasToOneAnnotation()) {
                    holder.registerProblem(element, toManyProblem, *toManyQuickFixes)
                } else if (returnPsiClass?.element?.typeParameters?.firstOrNull()
                        ?.let { returnPsiClass.substitutor.substitute(it) }
                        ?.let { PsiUtil.resolveGenericsClassInType(it) }
                        ?.element?.isEntity() == true && !element.hasToManyAnnotation()
                    && !element.isComputed() && !element.hasManyToManyViewAnnotation()
                ) {
                    holder.registerProblem(element, noToManyProblem, *toManyQuickFixes)
                }
            } else {
                if (element.hasToManyAnnotation()) {
                    holder.registerProblem(element, toOneProblem, *toOneQuickFixes)
                } else if (returnPsiClass?.element?.isEntity() == true && !element.hasToOneAnnotation() && !element.isComputed()) {
                    holder.registerProblem(element, noToOneProblem, *toOneQuickFixes)
                }
            }
        } else if (element is KtProperty) {
            val targetClass = element.findTargetClassBySyntax()
            val isTargetEntity = targetClass?.isEntityBySyntax() == true
            if (element.isCollectionBySyntax()) {
                val mappedBy = element.findMappedByBySyntax(targetClass)
                val fixes = createToManyQuickFixes(mappedBy)
                if (element.hasToOneAnnotationBySyntax()) {
                    holder.registerProblem(element, toManyProblem, *fixes)
                } else if (isTargetEntity && !element.hasToManyAnnotationBySyntax() && !element.isComputedBySyntax() && !element.hasManyToManyViewAnnotationBySyntax() && !element.hasIdViewAnnotationBySyntax()) {
                    holder.registerProblem(element, noToManyProblem, *fixes)
                }
            } else {
                if (element.hasToManyAnnotationBySyntax()) {
                    holder.registerProblem(element, toOneProblem, *createToOneQuickFixes())
                } else if (isTargetEntity && !element.hasToOneAnnotationBySyntax() && !element.isComputedBySyntax()) {
                    holder.registerProblem(element, noToOneProblem, *createToOneQuickFixes())
                }
            }
        }
    }
}

private fun createToOneQuickFixes(): Array<AddAssociationAnnotationQuickFix> {
    return arrayOf(
        AddAssociationAnnotationQuickFix(OneToOne::class.qualifiedName!!, OneToOne::class.simpleName!!),
        AddAssociationAnnotationQuickFix(ManyToOne::class.qualifiedName!!, ManyToOne::class.simpleName!!)
    )
}

private fun createToManyQuickFixes(mappedBy: String? = null): Array<AddAssociationAnnotationQuickFix> {
    return arrayOf(
        AddAssociationAnnotationQuickFix(OneToMany::class.qualifiedName!!, OneToMany::class.simpleName!!, mappedBy),
        AddAssociationAnnotationQuickFix(ManyToMany::class.qualifiedName!!, ManyToMany::class.simpleName!!, mappedBy)
    )
}

private fun KtProperty.hasAnnotationBySyntax(vararg annotationSimpleNames: String): Boolean {
    return annotationEntries.any { annotationEntry ->
        val shortName = annotationEntry.shortName?.asString()
        val typeText = annotationEntry.typeReference?.text
        annotationSimpleNames.any { annotationSimpleName ->
            shortName == annotationSimpleName || typeText == annotationSimpleName || typeText?.endsWith(".$annotationSimpleName") == true
        }
    }
}

private fun KtProperty.hasToOneAnnotationBySyntax(): Boolean {
    return hasAnnotationBySyntax(OneToOne::class.simpleName!!, ManyToOne::class.simpleName!!)
}

private fun KtProperty.hasToManyAnnotationBySyntax(): Boolean {
    return hasAnnotationBySyntax(OneToMany::class.simpleName!!, ManyToMany::class.simpleName!!)
}

private fun KtProperty.hasManyToManyViewAnnotationBySyntax(): Boolean {
    return hasAnnotationBySyntax(org.babyfish.jimmer.sql.ManyToManyView::class.simpleName!!)
}

private fun KtProperty.isComputedBySyntax(): Boolean {
    return hasAnnotationBySyntax(org.babyfish.jimmer.sql.Transient::class.simpleName!!, org.babyfish.jimmer.Formula::class.simpleName!!)
}

private fun KtProperty.hasIdViewAnnotationBySyntax(): Boolean {
    return hasAnnotationBySyntax(org.babyfish.jimmer.sql.IdView::class.simpleName!!)
}

private fun KtProperty.rawTypeNameBySyntax(): String? {
    val typeText = typeReference?.text ?: return null
    return typeText
        .trim()
        .substringBefore("<")
        .removeSuffix("?")
        .trim()
        .takeIf { it.isNotBlank() }
}

private fun KtProperty.targetTypeNameBySyntax(): String? {
    val typeText = typeReference?.text ?: return null
    val withoutNullability = typeText.trim().removeSuffix("?")
    val targetText = if (withoutNullability.contains("<")) {
        withoutNullability.substringAfterLast("<").substringBefore(">")
    } else {
        withoutNullability
    }

    return targetText
        .trim()
        .removeSuffix("?")
        .takeIf { it.isNotBlank() }
}

private fun KtProperty.isCollectionBySyntax(): Boolean {
    val rawTypeName = rawTypeNameBySyntax()?.substringAfterLast(".") ?: return false
    return rawTypeName in setOf("Collection", "List", "Set", "MutableCollection", "MutableList", "MutableSet")
}

private fun KtProperty.findTargetClassBySyntax(): KtClass? {
    val targetTypeName = targetTypeNameBySyntax() ?: return null
    return findTargetClassBySyntax(targetTypeName)
}

private fun KtProperty.findMappedByBySyntax(targetClass: KtClass? = null): String? {
    val sourceClass = containingClass() ?: return null
    val resolvedTargetClass = targetClass ?: findTargetClassBySyntax() ?: return null

    return resolvedTargetClass.declarations.filterIsInstance<KtProperty>().firstOrNull { property ->
        property.referencesClassBySyntax(sourceClass) && property.isOwningOppositeSideBySyntax(name ?: return@firstOrNull false)
    }?.name
}

private fun KtProperty.findTargetClassBySyntax(targetTypeName: String): KtClass? {
    val ktFile = containingKtFile
    val targetSimpleName = targetTypeName.substringAfterLast(".")
    val sameFileClass = ktFile.declarations
        .filterIsInstance<KtClass>()
        .firstOrNull { it.name == targetSimpleName }
    if (sameFileClass != null) {
        return sameFileClass
    }

    if (targetTypeName.contains(".")) {
        return project.findKtClass(targetTypeName) ?: ktFile.findClassByFileNameBySyntax(targetTypeName)
    }

    val importedClass = ktFile.findImportedClassBySyntax(targetSimpleName)
    if (importedClass != null) {
        return importedClass
    }

    val samePackageClass = ktFile.findSamePackageClassBySyntax(targetSimpleName)
    if (samePackageClass != null) {
        return samePackageClass
    }

    return ktFile.findWildcardImportedClassBySyntax(targetSimpleName)
}

private fun KtFile.findImportedClassBySyntax(targetSimpleName: String): KtClass? {
    val importedClassName = importDirectives.firstNotNullOfOrNull { importDirective ->
        val importedName = importDirective.importedFqName?.asString()
        importedName?.takeIf {
            !importDirective.isAllUnder && it.substringAfterLast(".") == targetSimpleName
        }
    } ?: return null

    return project.findKtClass(importedClassName) ?: findClassByFileNameBySyntax(importedClassName)
}

private fun KtFile.findSamePackageClassBySyntax(targetSimpleName: String): KtClass? {
    val packageName = packageFqName.asString().takeIf { it.isNotBlank() } ?: return null
    val qualifiedName = "$packageName.$targetSimpleName"
    return project.findKtClass(qualifiedName) ?: findClassByFileNameBySyntax(qualifiedName)
}

private fun KtFile.findWildcardImportedClassBySyntax(targetSimpleName: String): KtClass? {
    return importDirectives.firstNotNullOfOrNull { importDirective ->
        val importedPackage = importDirective.importedFqName?.asString()
        if (!importDirective.isAllUnder || importedPackage == null) {
            null
        } else {
            val qualifiedName = "$importedPackage.$targetSimpleName"
            project.findKtClass(qualifiedName) ?: findClassByFileNameBySyntax(qualifiedName)
        }
    }
}

private fun KtFile.findClassByFileNameBySyntax(qualifiedName: String): KtClass? {
    val targetSimpleName = qualifiedName.substringAfterLast(".")
    return FilenameIndex.getFilesByName(project, "$targetSimpleName.kt", GlobalSearchScope.allScope(project))
        .filterIsInstance<KtFile>()
        .firstNotNullOfOrNull { ktFile ->
            ktFile.getChildOfType<KtClass>()?.takeIf { ktClass ->
                ktClass.name == targetSimpleName && ktClass.fqName?.asString() == qualifiedName
            }
        }
}

private fun KtClass.isEntityBySyntax(): Boolean {
    return isInterface() && annotationEntries.any { annotationEntry ->
        val shortName = annotationEntry.shortName?.asString()
        val typeText = annotationEntry.typeReference?.text
        shortName == Entity::class.simpleName ||
                typeText == Entity::class.simpleName ||
                typeText == Entity::class.qualifiedName ||
                typeText?.endsWith(".${Entity::class.simpleName}") == true
    }
}

private fun KtProperty.referencesClassBySyntax(ktClass: KtClass): Boolean {
    val typeName = targetTypeNameBySyntax() ?: return false
    if (typeName.substringAfterLast(".") != ktClass.name) {
        return false
    }

    return !typeName.contains(".") || typeName == ktClass.fqName?.asString()
}

private fun KtProperty.isOwningOppositeSideBySyntax(sourceName: String): Boolean {
    val mappedBy = annotationEntries.firstNotNullOfOrNull { annotationEntry ->
        annotationEntry.valueArgumentList?.arguments?.firstOrNull { argument ->
            argument.getArgumentName()?.asName?.asString() == "mappedBy"
        }?.getArgumentExpression()?.text?.trim('"')?.takeIf { it.isNotBlank() }
    }
    return mappedBy != sourceName
}

private class AddAssociationAnnotationQuickFix(
    private val annotationQualifiedName: String,
    private val annotationSimpleName: String,
    private val mappedByBySyntax: String? = null
) : LocalQuickFix {
    override fun getName(): String {
        return I18n.message("intention.annotation.association.add", annotationSimpleName)
    }

    override fun getFamilyName(): String {
        return name
    }

    override fun applyFix(
        project: Project,
        descriptor: ProblemDescriptor
    ) {
        val element = descriptor.psiElement
        if (element is PsiMethod) {
            addJavaAnnotation(project, element)
        } else if (element is KtProperty) {
            addKotlinAnnotation(project, element)
        }
    }

    private fun addJavaAnnotation(project: Project, method: PsiMethod) {
        if (method.modifierList.annotations.any { it.qualifiedName == annotationQualifiedName }) {
            return
        }

        val annotation = method.modifierList.addAnnotation(annotationQualifiedName)
        val mappedBy = if (annotationSimpleName == ManyToOne::class.simpleName) {
            null
        } else {
            method.findMappedBy()
        }
        if (mappedBy != null) {
            annotation.setDeclaredAttributeValue(
                "mappedBy",
                PsiElementFactory.getInstance(project).createExpressionFromText("\"$mappedBy\"", annotation)
            )
        }
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(annotation)
    }

    private fun addKotlinAnnotation(project: Project, property: KtProperty) {
        if (property.hasAnnotationBySyntax()) {
            return
        }

        val mappedBy = if (annotationSimpleName == ManyToOne::class.simpleName) {
            null
        } else {
            mappedByBySyntax ?: property.findMappedByBySyntax()
        }
        val annotationText = if (mappedBy == null) {
            "@$annotationSimpleName"
        } else {
            "@$annotationSimpleName(mappedBy = \"$mappedBy\")"
        }
        property.addAnnotationEntry(KtPsiFactory(project).createAnnotationEntry(annotationText))
        property.containingKtFile.addImportIfMissing(project, annotationQualifiedName)
    }

    private fun KtProperty.hasAnnotationBySyntax(): Boolean {
        return annotationEntries.any { annotationEntry ->
            annotationEntry.shortName?.asString() == annotationSimpleName ||
                    annotationEntry.typeReference?.text == annotationQualifiedName ||
                    annotationEntry.typeReference?.text?.endsWith(".$annotationSimpleName") == true
        }
    }

    private fun KtFile.addImportIfMissing(project: Project, qualifiedName: String) {
        val packageName = qualifiedName.substringBeforeLast(".")
        if (importDirectives.any { importDirective ->
                importDirective.importedFqName?.asString() == qualifiedName ||
                        importDirective.importedFqName?.asString() == packageName && importDirective.isAllUnder
            }) {
            return
        }

        importList?.add(KtPsiFactory(project).createImportDirective(ImportPath(FqName(qualifiedName), false)))
    }

    private fun PsiMethod.findMappedBy(): String? {
        val target = getTarget() ?: return null
        val source = containingClass ?: return null
        return target.allMethods.firstOrNull { method ->
            method.getTarget() == source && method.isOwningOppositeSide(name)
        }?.name
    }

    private fun PsiMethod.isOwningOppositeSide(sourceName: String): Boolean {
        val mappedBy = modifierList.annotations.firstNotNullOfOrNull { annotation ->
            annotation.findAttributeValue("mappedBy")?.toAny(String::class.java)?.toString()?.takeIf { it.isNotBlank() }
        }
        return mappedBy != sourceName
    }
}
