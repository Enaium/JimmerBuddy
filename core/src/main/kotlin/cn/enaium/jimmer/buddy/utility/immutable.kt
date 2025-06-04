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

import com.google.devtools.ksp.getClassDeclarationByName
import com.intellij.psi.PsiClass
import org.babyfish.jimmer.apt.createContext
import org.babyfish.jimmer.apt.immutable.meta.ImmutableType
import org.babyfish.jimmer.ksp.Context
import org.babyfish.jimmer.sql.IdView
import org.babyfish.jimmer.sql.ManyToManyView
import org.jetbrains.kotlin.psi.KtClass

/**
 * @author Enaium
 */
fun PsiClass.toImmutable(): ImmutableType {
    val (pe, typeElements, sources) = psiClassesToApt(copyOnWriteSetOf(this), copyOnWriteSetOf())
    val context = createContext(pe.elementUtils, pe.typeUtils, pe.filer)
    return context.getImmutableType(pe.elementUtils.getTypeElement(this.qualifiedName!!))
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
                CommonImmutableType.CommonImmutableProp(
                    { name },
                    { prop.declaringType.toCommonImmutableType() },
                    { prop.targetType?.toCommonImmutableType() },
                    { prop.typeName().simplify() },
                    { prop.isId },
                    isKey = { prop.isKey },
                    isEmbedded = { prop.isEmbedded },
                    { prop.isAssociation(it) },
                    isList = { prop.isList },
                    isTransient = { prop.isTransient },
                    isFormula = { prop.isFormula },
                    hasTransientResolver = { prop.hasTransientResolver() },
                    isRecursive = { prop.isRecursive },
                    isIdView = { prop.annotation(IdView::class) != null },
                    isManyToManyView = { prop.annotation(ManyToManyView::class) != null },
                    isLogicalDeleted = { prop.isLogicalDeleted },
                    isNullable = { prop.isNullable },
                    isExcludedFromAllScalars = { prop.isExcludedFromAllScalars }
                )
            }
        }
    )
}

fun ImmutableType.toCommonImmutableType(): CommonImmutableType {
    return CommonImmutableType(
        { this.name },
        { this.qualifiedName },
        { this.superTypes.map { it.toCommonImmutableType() } },
        {
            this.props.map { (name, prop) ->
                CommonImmutableType.CommonImmutableProp(
                    { name },
                    { prop.declaringType.toCommonImmutableType() },
                    { prop.context().getImmutableType(prop.elementType)?.toCommonImmutableType() },
                    { prop.typeName.simplify() },
                    { prop.isId },
                    isKey = { prop.isKey },
                    isEmbedded = { prop.isEmbedded },
                    { prop.isAssociation(false) },
                    isList = { prop.isList },
                    isTransient = { prop.isTransient },
                    hasTransientResolver = { prop.hasTransientResolver() },
                    isRecursive = { prop.isRecursive },
                    isFormula = { prop.isFormula },
                    isIdView = { prop.getAnnotation(IdView::class.java) != null },
                    isManyToManyView = { prop.getAnnotation(ManyToManyView::class.java) != null },
                    isLogicalDeleted = { prop.isLogicalDeleted },
                    isNullable = { prop.isNullable },
                    isExcludedFromAllScalars = { prop.isExcludedFromAllScalars },
                )
            }
        }
    )
}

data class CommonImmutableType(
    val name: () -> String,
    val qualifiedName: () -> String,
    val superTypes: () -> List<CommonImmutableType>,
    val props: () -> List<CommonImmutableProp>
) {
    data class CommonImmutableProp(
        val name: () -> String,
        val declaringType: () -> CommonImmutableType,
        val targetType: () -> CommonImmutableType?,
        val typeName: () -> String,
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