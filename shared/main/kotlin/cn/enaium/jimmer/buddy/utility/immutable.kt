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
import org.babyfish.jimmer.ksp.Context
import org.jetbrains.kotlin.psi.KtClass

/**
 * @author Enaium
 */
fun PsiClass.toImmutableType(): org.babyfish.jimmer.apt.immutable.meta.ImmutableType {
    val (pe, typeElements, sources) = psiClassesToApt(copyOnWriteSetOf(this), copyOnWriteSetOf())
    val context = createContext(pe.elementUtils, pe.typeUtils, pe.filer)
    return context.getImmutableType(pe.elementUtils.getTypeElement(this.qualifiedName!!))
}

fun KtClass.toImmutableType(): org.babyfish.jimmer.ksp.immutable.meta.ImmutableType {
    val (resolver, environment, sources) = ktClassToKsp(copyOnWriteSetOf(this), copyOnWriteSetOf())
    val context = Context(resolver, environment)
    val classDeclarationByName = resolver.getClassDeclarationByName(this.fqName!!.asString())!!
    return context.typeOf(classDeclarationByName)
}