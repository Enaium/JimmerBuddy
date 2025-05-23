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

package cn.enaium.jimmer.buddy.service

import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeReference

/**
 * @author Enaium
 */
interface PsiService {
    data class Annotation(val fqName: String?, val arguments: List<Argument>) {
        data class Argument(val name: String, val value: Any?)

        fun findArgument(name: String): Argument? {
            return arguments.find { it.name == name }
        }
    }

    data class Type(
        val fqName: String?,
        val nullable: Boolean,
        val ktClass: KtClass?,
        val arguments: List<Type> = emptyList(),
    )

    fun annotations(ktClass: KtClass): List<Annotation>
    fun annotations(ktProperty: KtProperty): List<Annotation>
    fun type(ktTypeReference: KtTypeReference): Type
    fun receiver(ktLambdaExpression: KtLambdaExpression): KtClass?
}