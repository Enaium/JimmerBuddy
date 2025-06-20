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

package cn.enaium.jimmer.buddy.utility

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.extensions.dto.completion.getTrace
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiRoot
import com.google.devtools.ksp.getClassDeclarationByName
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.util.findParentOfType
import org.babyfish.jimmer.apt.createContext
import org.babyfish.jimmer.apt.immutable.meta.ImmutableProp
import org.babyfish.jimmer.apt.immutable.meta.ImmutableType
import org.babyfish.jimmer.ksp.Context
import org.babyfish.jimmer.sql.IdView
import org.babyfish.jimmer.sql.ManyToManyView
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.psi.KtClass

/**
 * @author Enaium
 */
fun PsiClass.toImmutable(): ImmutableType {
    val (pe, typeElements, sources) = psiClassesToApt(copyOnWriteSetOf(this), copyOnWriteSetOf())
    val context = createContext(pe.elementUtils, pe.typeUtils, pe.filer)
    return context.getImmutableType(pe.elementUtils.getTypeElement(this.qualifiedName!!))
}

fun ImmutableProp.toCommonImmutableProp(): CommonImmutableType.CommonImmutableProp {
    return CommonImmutableType.CommonImmutableProp(
        { name },
        { declaringType.toCommonImmutableType() },
        { context().getImmutableType(elementType)?.toCommonImmutableType() },
        { typeName.toString() },
        { typeName.simplify() },
        { isId },
        isKey = { isKey },
        isEmbedded = { isEmbedded },
        { isAssociation(false) },
        isList = { isList },
        isTransient = { isTransient },
        hasTransientResolver = { hasTransientResolver() },
        isRecursive = { isRecursive },
        isFormula = { isFormula },
        isIdView = { getAnnotation(IdView::class.java) != null },
        isManyToManyView = { getAnnotation(ManyToManyView::class.java) != null },
        isLogicalDeleted = { isLogicalDeleted },
        isNullable = { isNullable },
        isExcludedFromAllScalars = { isExcludedFromAllScalars },
    )
}

fun KtClass.toImmutable(): org.babyfish.jimmer.ksp.immutable.meta.ImmutableType {
    val (resolver, environment, sources) = ktClassToKsp(copyOnWriteSetOf(this), copyOnWriteSetOf())
    val context = Context(resolver, environment)
    val classDeclarationByName = resolver.getClassDeclarationByName(this.fqName!!.asString())!!
    return context.typeOf(classDeclarationByName)
}

fun org.babyfish.jimmer.ksp.immutable.meta.ImmutableType.toCommonImmutableType(): CommonImmutableType {
    return CommonImmutableType(
        { this.name },
        { this.qualifiedName },
        { this.superTypes.map { it.toCommonImmutableType() } },
        {
            this.properties.map { (name, prop) ->
                prop.toCommonImmutableProp()
            }
        },
        {
            this.declaredProperties.map { (name, prop) ->
                prop.toCommonImmutableProp()
            }
        }
    )
}

fun org.babyfish.jimmer.ksp.immutable.meta.ImmutableProp.toCommonImmutableProp(): CommonImmutableType.CommonImmutableProp {
    return CommonImmutableType.CommonImmutableProp(
        { name },
        { declaringType.toCommonImmutableType() },
        { targetType?.toCommonImmutableType() },
        { typeName().toString() },
        { typeName().simplify() },
        { isId },
        isKey = { isKey },
        isEmbedded = { isEmbedded },
        { isAssociation(it) },
        isList = { isList },
        isTransient = { isTransient },
        isFormula = { isFormula },
        hasTransientResolver = { hasTransientResolver() },
        isRecursive = { isRecursive },
        isIdView = { annotation(IdView::class) != null },
        isManyToManyView = { annotation(ManyToManyView::class) != null },
        isLogicalDeleted = { isLogicalDeleted },
        isNullable = { isNullable },
        isExcludedFromAllScalars = { isExcludedFromAllScalars }
    )
}

fun ImmutableType.toCommonImmutableType(): CommonImmutableType {
    return CommonImmutableType(
        { this.name },
        { this.qualifiedName },
        { this.superTypes.map { it.toCommonImmutableType() } },
        {
            this.props.map { (name, prop) ->
                prop.toCommonImmutableProp()
            }
        },
        {
            this.declaredProps.map { (name, prop) ->
                prop.toCommonImmutableProp()
            }
        }
    )
}

data class CommonImmutableType(
    val name: () -> String,
    val qualifiedName: () -> String,
    val superTypes: () -> List<CommonImmutableType>,
    val props: () -> List<CommonImmutableProp>,
    val declaredProps: () -> List<CommonImmutableProp>,
) {
    data class CommonImmutableProp(
        val name: () -> String,
        val declaringType: () -> CommonImmutableType,
        val targetType: () -> CommonImmutableType?,
        val typeName: () -> String,
        val simpleTypeName: () -> String,
        val isId: () -> Boolean,
        val isKey: () -> Boolean,
        val isEmbedded: () -> Boolean,
        val isAssociation: (entityLevel: Boolean) -> Boolean,
        val isList: () -> Boolean,
        val isTransient: () -> Boolean,
        val isFormula: () -> Boolean,
        val hasTransientResolver: () -> Boolean,
        val isRecursive: () -> Boolean,
        val isIdView: () -> Boolean,
        val isManyToManyView: () -> Boolean,
        val isLogicalDeleted: () -> Boolean,
        val isNullable: () -> Boolean,
        val isExcludedFromAllScalars: () -> Boolean,
    ) {
        companion object {
            fun CommonImmutableProp.type(): PropType {
                return if (isId()) {
                    PropType.ID
                } else if (isKey()) {
                    PropType.KEY
                } else if (isEmbedded()) {
                    PropType.EMBEDDED
                } else if (isFormula()) {
                    PropType.FORMULA
                } else if (isTransient()) {
                    if (hasTransientResolver()) PropType.CALCULATION else PropType.TRANSIENT
                } else if (isRecursive()) {
                    PropType.RECURSIVE
                } else if (isAssociation(true)) {
                    PropType.ASSOCIATION
                } else if (isList()) {
                    PropType.LIST
                } else if (isLogicalDeleted()) {
                    PropType.LOGICAL_DELETED
                } else if (isNullable()) {
                    PropType.NULLABLE
                } else {
                    PropType.PROPERTY
                }
            }
        }

        override fun toString(): String {
            return name()
        }
    }

    override fun toString(): String {
        return name()
    }
}

enum class PropType(val description: String) {
    ID("Id"),
    KEY("Key"),
    EMBEDDED("Embedded"),
    FORMULA("Formula"),
    CALCULATION("Calculation"),
    TRANSIENT("Transient"),
    RECURSIVE("Recursive"),
    ASSOCIATION("Association"),
    LIST("List"),
    LOGICAL_DELETED("LogicalDeleted"),
    NULLABLE("Nullable"),
    PROPERTY("Property")
}

fun findCurrentImmutableType(element: PsiElement): CommonImmutableType? {
    val project = element.project
    val trace = getTrace(element)
    val typeName =
        element.findParentOfType<DtoPsiRoot>()?.qualifiedName() ?: return null
    try {
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
        return currentImmutable
    } catch (e: Throwable) {
        JimmerBuddy.getWorkspace(project).log.error(e)
        return null
    }
}