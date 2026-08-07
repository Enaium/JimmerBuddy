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

import cn.enaium.jimmer.buddy.extensions.dto.completion.getTrace
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiDtoFragment
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiExportStatement
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiImportStatement
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiQualifiedName
import com.google.devtools.ksp.getClassDeclarationByName
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.util.PsiTreeUtil
import org.babyfish.jimmer.apt.MetaException
import org.babyfish.jimmer.apt.createContext
import org.babyfish.jimmer.apt.immutable.meta.ImmutableProp
import org.babyfish.jimmer.apt.immutable.meta.ImmutableType
import org.babyfish.jimmer.ksp.Context
import org.babyfish.jimmer.sql.IdView
import org.babyfish.jimmer.sql.ManyToMany
import org.babyfish.jimmer.sql.ManyToManyView
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElementOfType
import java.util.concurrent.ConcurrentHashMap

/**
 * @author Enaium
 */
class CommonImmutableType(
    val name: String,
    val qualifiedName: String,
    val superTypes: List<CommonImmutableType>,
    val props: MutableList<CommonImmutableProp>,
    val declaredProps: MutableList<CommonImmutableProp>,
    val isEmbedded: Boolean,
) {
    override fun toString(): String = name
}

data class CommonImmutableProp(
    val name: String,
    val declaringType: CommonImmutableType,
    val targetType: CommonImmutableType?,
    val typeName: String,
    val simpleTypeName: String,
    val isId: Boolean,
    val isKey: Boolean,
    val isEmbedded: Boolean,
    val isAssociation: Boolean,
    val isList: Boolean,
    val isTransient: Boolean,
    val isFormula: Boolean,
    val hasTransientResolver: Boolean,
    val isRecursive: Boolean,
    val isIdView: Boolean,
    val isManyToMany: Boolean,
    val isManyToManyView: Boolean,
    val isLogicalDeleted: Boolean,
    val isNullable: Boolean,
    val isExcludedFromAllScalars: Boolean,
) {
    companion object {
        fun CommonImmutableProp.type(): PropType {
            return if (isId) PropType.ID
            else if (isKey) PropType.KEY
            else if (isEmbedded) PropType.EMBEDDED
            else if (isFormula) PropType.FORMULA
            else if (isTransient) {
                if (hasTransientResolver) PropType.CALCULATION else PropType.TRANSIENT
            } else if (isRecursive) PropType.RECURSIVE
            else if (isAssociation) PropType.ASSOCIATION
            else if (isList) PropType.LIST
            else if (isLogicalDeleted) PropType.LOGICAL_DELETED
            else if (isNullable) PropType.NULLABLE
            else PropType.PROPERTY
        }

        fun CommonImmutableProp.isAutoScalar(): Boolean {
            return !isFormula &&
                    !isTransient &&
                    !isIdView &&
                    !isManyToManyView &&
                    !isList &&
                    !isAssociation &&
                    !isLogicalDeleted &&
                    !isExcludedFromAllScalars
        }

        fun CommonImmutableProp.isComputed(): Boolean {
            return isFormula || isTransient
        }
    }

    override fun toString(): String = name
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

@Service(Service.Level.PROJECT)
class CommonImmutableTypeCache(val project: Project) {
    private val cache = ConcurrentHashMap<String, CommonImmutableType>()

    fun get(qualifiedName: String): CommonImmutableType? {
        cache[qualifiedName]?.let { return it }
        val built = buildTypeSync(qualifiedName) ?: return null
        cache[qualifiedName] = built
        return built
    }

    fun getAll(): Collection<CommonImmutableType> = cache.values

    fun initialize() {
        PsiManager.getInstance(project).addPsiTreeChangeListener(object : com.intellij.psi.PsiTreeChangeAdapter() {
            override fun childrenChanged(event: com.intellij.psi.PsiTreeChangeEvent) {
                handleChange(event)
            }

            override fun childAdded(event: com.intellij.psi.PsiTreeChangeEvent) {
                handleChange(event)
            }

            override fun childRemoved(event: com.intellij.psi.PsiTreeChangeEvent) {
                handleChange(event)
            }

            override fun childReplaced(event: com.intellij.psi.PsiTreeChangeEvent) {
                handleChange(event)
            }

            private fun handleChange(event: com.intellij.psi.PsiTreeChangeEvent) {
                val parent = event.parent ?: event.child ?: return
                if (parent is PsiClass || parent is KtClass) {
                    val qualifiedName = (parent as? PsiClass)?.qualifiedName
                        ?: (parent as? KtClass)?.fqName?.asString()
                        ?: return
                    cache.remove(qualifiedName)
                }
            }
        }, project)
    }

    private fun buildTypeSync(qualifiedName: String): CommonImmutableType? {
        return try {
            val psiClass = JavaPsiFacade.getInstance(project).findClass(qualifiedName, project.allScope())
            val ktClass = KotlinFullClassNameIndex[qualifiedName, project, project.allScope()].firstOrNull() as? KtClass

            val nav = when {
                ktClass != null -> ktClass
                psiClass != null -> psiClass.navigationElement
                else -> return null
            }

            when (nav) {
                is KtClass -> {
                    if (nav.isImmutable()) {
                        nav.toImmutable()?.toCommonImmutableType()
                    } else {
                        null
                    }
                }

                is PsiClass -> {
                    if (nav.isImmutable()) {
                        nav.toImmutable()?.toCommonImmutableType()
                    } else {
                        null
                    }
                }

                else -> null
            }
        } catch (e: MetaException) {
            null
        } catch (e: org.babyfish.jimmer.ksp.MetaException) {
            null
        }
    }

    companion object {
        fun getInstance(project: Project): CommonImmutableTypeCache =
            project.getService(CommonImmutableTypeCache::class.java)
    }
}

fun PsiClass.toImmutable(): ImmutableType? {
    return try {
        val (pe, typeElements, sources) = project.psiClassesToApt(setOf(this))
        val context = createContext(pe.elementUtils, pe.typeUtils, pe.filer)
        context.getImmutableType(pe.elementUtils.getTypeElement(this.qualifiedName!!))
    } catch (e: Exception) {
        null
    }
}

fun KtClass.toImmutable(): org.babyfish.jimmer.ksp.immutable.meta.ImmutableType? {
    return try {
        val (resolver, environment, sources) = project.ktClassesToKsp(copyOnWriteSetOf(this))
        val context = Context(resolver, environment)
        val classDeclarationByName = resolver.getClassDeclarationByName(this.fqName!!.asString())!!
        context.typeOf(classDeclarationByName)
    } catch (e: Exception) {
        null
    }
}

fun ImmutableType.toCommonImmutableType(
    memo: MutableMap<String, CommonImmutableType> = mutableMapOf()
): CommonImmutableType {
    val qn = this.qualifiedName
    memo[qn]?.let { return it }
    val superTypes = this.superTypes.map { it.toCommonImmutableType(memo) }
    val type = CommonImmutableType(
        name = this.name,
        qualifiedName = this.qualifiedName,
        superTypes = superTypes,
        props = mutableListOf(),
        declaredProps = mutableListOf(),
        isEmbedded = this.isEmbeddable
    )
    memo[qn] = type
    type.props.addAll(this.props.map { (_, prop) -> prop.toCommonImmutableProp(type, memo) })
    type.declaredProps.addAll(this.declaredProps.map { (_, prop) -> prop.toCommonImmutableProp(type, memo) })
    return type
}

fun ImmutableProp.toCommonImmutableProp(
    declaringType: CommonImmutableType,
    memo: MutableMap<String, CommonImmutableType> = mutableMapOf()
): CommonImmutableProp {
    return CommonImmutableProp(
        name = name,
        declaringType = declaringType,
        targetType = context().getImmutableType(elementType)?.toCommonImmutableType(memo),
        typeName = typeName.toString(),
        simpleTypeName = typeName.simplify(),
        isId = isId,
        isKey = isKey,
        isEmbedded = isEmbedded,
        isAssociation = isAssociation(true),
        isList = isList,
        isTransient = isTransient,
        isFormula = isFormula,
        hasTransientResolver = hasTransientResolver(),
        isRecursive = isRecursive,
        isIdView = getAnnotation(IdView::class.java) != null,
        isManyToMany = getAnnotation(ManyToMany::class.java) != null,
        isManyToManyView = getAnnotation(ManyToManyView::class.java) != null,
        isLogicalDeleted = isLogicalDeleted,
        isNullable = isNullable,
        isExcludedFromAllScalars = isExcludedFromAllScalars,
    )
}

fun org.babyfish.jimmer.ksp.immutable.meta.ImmutableType.toCommonImmutableType(
    memo: MutableMap<String, CommonImmutableType> = mutableMapOf()
): CommonImmutableType {
    val qn = this.qualifiedName
    memo[qn]?.let { return it }
    val superTypes = this.superTypes.map { it.toCommonImmutableType(memo) }
    val type = CommonImmutableType(
        name = this.name,
        qualifiedName = this.qualifiedName,
        superTypes = superTypes,
        props = mutableListOf(),
        declaredProps = mutableListOf(),
        isEmbedded = this.isEmbeddable
    )
    memo[qn] = type
    type.props.addAll(this.properties.map { (_, prop) -> prop.toCommonImmutableProp(type, memo) })
    type.declaredProps.addAll(this.declaredProperties.map { (_, prop) -> prop.toCommonImmutableProp(type, memo) })
    return type
}

fun org.babyfish.jimmer.ksp.immutable.meta.ImmutableProp.toCommonImmutableProp(
    declaringType: CommonImmutableType,
    memo: MutableMap<String, CommonImmutableType> = mutableMapOf()
): CommonImmutableProp {
    return CommonImmutableProp(
        name = name,
        declaringType = declaringType,
        targetType = targetType?.toCommonImmutableType(memo),
        typeName = typeName().toString(),
        simpleTypeName = typeName().simplify(),
        isId = isId,
        isKey = isKey,
        isEmbedded = isEmbedded,
        isAssociation = isAssociation(true),
        isList = isList,
        isTransient = isTransient,
        isFormula = isFormula,
        hasTransientResolver = hasTransientResolver(),
        isRecursive = isRecursive,
        isIdView = annotation(IdView::class) != null,
        isManyToMany = annotation(ManyToMany::class) != null,
        isManyToManyView = annotation(ManyToManyView::class) != null,
        isLogicalDeleted = isLogicalDeleted,
        isNullable = isNullable,
        isExcludedFromAllScalars = isExcludedFromAllScalars,
    )
}

fun findCurrentImmutableType(element: PsiElement): CommonImmutableType? {
    val project = element.project
    val trace = element.parent?.let { getTrace(it) } ?: emptyList()
    val typeName = resolveTypeName(element) ?: return null

    val cache = CommonImmutableTypeCache.getInstance(project)
    var currentImmutable = cache.get(typeName) ?: return null

    trace.forEach { trace ->
        if (trace.firstOrNull()?.isUpperCase() == true) {
            currentImmutable.psi(project)?.toUElementOfType<UClass>()?.javaPsi?.also { psi ->
                ClassInheritorsSearch.search(psi, element.project.allScope(), false).find { it.name == trace }
                    ?.also {
                        when (val nav = it.navigationElement) {
                            is PsiClass -> {
                                nav.qualifiedName?.let { qn -> cache.get(qn) }?.also {
                                    currentImmutable = it
                                }
                            }

                            is KtClass -> {
                                nav.fqName?.asString()?.let { qn -> cache.get(qn) }?.also {
                                    currentImmutable = it
                                }
                            }

                            else -> {}
                        }
                    }
            }
        } else {
            currentImmutable.props.find { it.name == trace }?.targetType?.also {
                currentImmutable = it
            }
        }
    }
    return currentImmutable
}

private fun resolveTypeName(element: PsiElement): String? {
    val fragment = PsiTreeUtil.getParentOfType(element, DtoPsiDtoFragment::class.java)
    if (fragment != null) {
        val forQualifiedName = fragment.qualifiedName?.text ?: return PsiTreeUtil.findChildOfType(
            element.containingFile, DtoPsiExportStatement::class.java
        )?.let { PsiTreeUtil.findChildOfType(it, DtoPsiQualifiedName::class.java) }?.text
        if (forQualifiedName.contains(".")) {
            return forQualifiedName
        }
        val file = element.containingFile
        val importStatements = PsiTreeUtil.getChildrenOfType(file, DtoPsiImportStatement::class.java) ?: return null
        for (importStatement in importStatements) {
            val importQName = importStatement.qualifiedName.name()
            if (importStatement.importedTypeList.isEmpty()) {
                if (importQName.substringAfterLast(".") == forQualifiedName) {
                    return importQName
                }
            }
        }
        return null
    }
    return PsiTreeUtil.findChildOfType(element.containingFile, DtoPsiExportStatement::class.java)
        ?.let { PsiTreeUtil.findChildOfType(it, DtoPsiQualifiedName::class.java) }?.text
}

fun CommonImmutableType.psi(project: Project): PsiElement? {
    val psiClass = JavaPsiFacade.getInstance(project).findClass(qualifiedName, project.allScope())
    val ktClass = KotlinFullClassNameIndex[qualifiedName, project, project.allScope()].firstOrNull() as? KtClass
    return if (project.workspace().isJavaProject) psiClass
    else if (project.workspace().isKotlinProject) ktClass
    else null
}

fun CommonImmutableProp.psi(project: Project): PsiElement? {
    val psiClass = JavaPsiFacade.getInstance(project).findClass(declaringType.qualifiedName, project.allScope())
    val ktClass =
        KotlinFullClassNameIndex[declaringType.qualifiedName, project, project.allScope()].firstOrNull() as? KtClass
    return if (project.workspace().isJavaProject) psiClass?.methods?.find { it.name == name }
    else if (project.workspace().isKotlinProject) ktClass?.getProperties()?.find { it.name == name }
    else null
}