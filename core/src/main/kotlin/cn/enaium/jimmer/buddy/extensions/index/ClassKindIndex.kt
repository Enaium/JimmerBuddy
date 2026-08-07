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

package cn.enaium.jimmer.buddy.extensions.index

import cn.enaium.jimmer.buddy.JimmerBuddy
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.psi.PsiClass
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import java.io.DataInput
import java.io.DataOutput

/**
 * @author Enaium
 */
class ClassKindIndex : FileBasedIndexExtension<String, ClassKindIndex.Kind>() {

    val immutableAnnotationRegex =
        Regex("^@(?:org\\.babyfish\\.jimmer\\.sql\\.)?(?:Entity|Immutable)\\b", setOf(RegexOption.MULTILINE))
    val errorFamilyAnnotationRegex =
        Regex("^@(?:org\\.babyfish\\.jimmer\\.errro\\.)?ErrorFamily\\b", setOf(RegexOption.MULTILINE))

    override fun getName(): ID<String, Kind> {
        return JimmerBuddy.Indexes.CLASS_KIND
    }

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return DefaultFileTypeSpecificInputFilter(JavaFileType.INSTANCE, KotlinFileType.INSTANCE)
    }

    override fun dependsOnFileContent(): Boolean {
        return true
    }

    override fun getIndexer(): DataIndexer<String, Kind, FileContent> {
        return DataIndexer<String, Kind, FileContent> { file ->
            when (file.fileType) {
                JavaFileType.INSTANCE -> {
                    file.psiFile.getChildrenOfType<PsiClass>()
                        .mapNotNull {
                            it.qualifiedName to when {
                                it.isAnnotationType -> Kind.ANNOTATION
                                it.isEnum -> if (file.contentAsText.contains(errorFamilyAnnotationRegex)) {
                                    Kind.ERROR_FAMILY
                                } else {
                                    Kind.ENUM
                                }

                                it.isInterface ->
                                    if (file.contentAsText.contains(immutableAnnotationRegex)) {
                                        Kind.IMMUTABLE
                                    } else {
                                        Kind.INTERFACE
                                    }

                                else -> return@mapNotNull null
                            }
                        }.associate { it }
                }

                KotlinFileType.INSTANCE -> {
                    file.psiFile.getChildrenOfType<KtClass>()
                        .mapNotNull {
                            it.fqName!!.asString() to when {
                                it.isAnnotation() -> Kind.ANNOTATION
                                it.isEnum() -> if (file.contentAsText.contains(errorFamilyAnnotationRegex)) {
                                    Kind.ERROR_FAMILY
                                } else {
                                    Kind.ENUM
                                }

                                it.isInterface() -> {
                                    if (file.contentAsText.contains(immutableAnnotationRegex)) {
                                        Kind.IMMUTABLE
                                    } else {
                                        Kind.INTERFACE
                                    }
                                }

                                else -> return@mapNotNull null
                            }
                        }.associate { it }
                }

                else -> {
                    emptyMap()
                }
            }
        }
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> {
        return EnumeratorStringDescriptor()
    }

    override fun getValueExternalizer(): DataExternalizer<Kind> {
        return KindExternalizer
    }

    object KindExternalizer : DataExternalizer<Kind> {
        override fun save(
            output: DataOutput,
            kind: Kind
        ) {
            output.writeByte(kind.value.toInt())
        }

        override fun read(input: DataInput): Kind {
            val value = input.readByte().toUByte()
            return Kind.entries.find { it.value == value }
                ?: throw IllegalArgumentException("unknown kind")
        }
    }

    override fun getVersion(): Int {
        return 0
    }

    enum class Kind(val value: UByte) {
        ANNOTATION(0u),
        ENUM(1u),
        INTERFACE(2u),
        IMMUTABLE(3u),
        ERROR_FAMILY(4u)
    }
}