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

package org.babyfish.jimmer.apt

import org.babyfish.jimmer.dto.compiler.DtoModifier
import java.util.*
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic

/**
 * @author Enaium
 */
fun createContext(
    elements: Elements,
    types: Types,
    filer: Filer,
    keepIsPrefix: Boolean = false,
    includes: Array<String> = emptyArray(),
    excludes: Array<String> = emptyArray(),
    immutablesTypeName: String? = null,
    tablesTypeName: String? = null,
    tableExesTypeName: String? = null,
    fetchersTypeName: String? = null,
    hibernateValidatorEnhancement: Boolean = false
): Context {
    return Context(
        elements,
        types,
        filer,
        keepIsPrefix,
        includes,
        excludes,
        immutablesTypeName,
        tablesTypeName,
        tableExesTypeName,
        fetchersTypeName,
        hibernateValidatorEnhancement,
        true,
        Modifier.PRIVATE
    )
}

@Suppress("UNCHECKED_CAST")
fun createAptOption(
    options: Map<String, String>,
    elements: Elements,
    types: Types,
    filer: Filer
): AptOption {
    val jimmerProcessor = JimmerProcessor()
    jimmerProcessor.init(object : ProcessingEnvironment {
        override fun getOptions(): Map<String, String> {
            return options + mapOf("jimmer.buddy.ignoreResourceGeneration" to "true")
        }

        override fun getMessager(): Messager {
            return object : Messager {
                override fun printMessage(kind: Diagnostic.Kind?, msg: CharSequence?) {

                }

                override fun printMessage(
                    kind: Diagnostic.Kind?,
                    msg: CharSequence?,
                    e: Element?
                ) {

                }

                override fun printMessage(
                    kind: Diagnostic.Kind?,
                    msg: CharSequence?,
                    e: Element?,
                    a: AnnotationMirror?
                ) {

                }

                override fun printMessage(
                    kind: Diagnostic.Kind?,
                    msg: CharSequence?,
                    e: Element?,
                    a: AnnotationMirror?,
                    v: AnnotationValue?
                ) {

                }
            }
        }

        override fun getFiler(): Filer {
            return filer
        }

        override fun getElementUtils(): Elements {
            return elements
        }

        override fun getTypeUtils(): Types {
            return types
        }

        override fun getSourceVersion(): SourceVersion? {
            TODO("Not yet implemented")
        }

        override fun getLocale(): Locale? {
            TODO("Not yet implemented")
        }
    })
    val javaProcessClass = JimmerProcessor::class.java
    return AptOption(
        javaProcessClass.getDeclaredField("context").also { it.isAccessible = true }.get(jimmerProcessor) as Context,
        javaProcessClass.getDeclaredField("dtoDirs").also { it.isAccessible = true }
            .get(jimmerProcessor) as Collection<String>,
        javaProcessClass.getDeclaredField("dtoTestDirs").also { it.isAccessible = true }
            .get(jimmerProcessor) as Collection<String>,
        javaProcessClass.getDeclaredField("defaultNullableInputModifier").also { it.isAccessible = true }
            .get(jimmerProcessor) as DtoModifier,
        javaProcessClass.getDeclaredField("checkedException").also { it.isAccessible = true }
            .get(jimmerProcessor) as Boolean,
        javaProcessClass.getDeclaredField("ignoreJdkWarning").also { it.isAccessible = true }
            .get(jimmerProcessor) as Boolean,
        javaProcessClass.getDeclaredField("dtoFieldModifier").also { it.isAccessible = true }
            .get(jimmerProcessor) as Modifier
    )
}

data class AptOption(
    val context: Context,
    val dtoDirs: Collection<String>,
    val dtoTestDirs: Collection<String>,
    val defaultNullableInputModifier: DtoModifier,
    val checkedException: Boolean,
    val ignoreJdkWarning: Boolean,
    val dtoFieldModifier: Modifier
)