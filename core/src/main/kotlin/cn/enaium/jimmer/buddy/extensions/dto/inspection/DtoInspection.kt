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

package cn.enaium.jimmer.buddy.extensions.dto.inspection

import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiRoot
import cn.enaium.jimmer.buddy.utility.*
import com.google.devtools.ksp.getClassDeclarationByName
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.editor.Document
import com.intellij.psi.*
import org.babyfish.jimmer.apt.createContext
import org.babyfish.jimmer.apt.dto.AptDtoCompiler
import org.babyfish.jimmer.dto.compiler.DtoAstException
import org.babyfish.jimmer.dto.compiler.DtoFile
import org.babyfish.jimmer.dto.compiler.DtoModifier
import org.babyfish.jimmer.dto.compiler.OsFile
import org.babyfish.jimmer.ksp.Context
import org.babyfish.jimmer.ksp.KspDtoCompiler
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import java.io.Reader
import javax.lang.model.element.TypeElement
import kotlin.io.path.absolutePathString
import kotlin.io.path.name

class DtoInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitFile(file: PsiFile) {
                val project = file.project
                val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return
                val path = file.virtualFile.toNioPath()
                val typeName =
                    file.getChildOfType<DtoPsiRoot>()?.qualifiedName() ?: return
                if (project.workspace().isJavaProject) {
                    val typeClass =
                        JavaPsiFacade.getInstance(project).findClass(typeName, project.allScope()) ?: return
                    val (pe, rootElements, sources) =
                        project.psiClassesToApt(setOf(typeClass))
                    val context = createContext(
                        pe.elementUtils,
                        pe.typeUtils,
                        pe.filer,
                        false,
                        emptyArray(),
                        emptyArray(),
                        null,
                        null,
                        null,
                        null,
                        false
                    )
                    val elements = pe.elementUtils
                    val dtoFile = DtoFile(object : OsFile {
                        override fun getAbsolutePath(): String {
                            return path.absolutePathString()
                        }

                        override fun openReader(): Reader {
                            return file.text.reader()
                        }
                    }, findProjectDir(path)?.absolutePathString() ?: "", "", emptyList(), path.name)
                    try {
                        val compiler = AptDtoCompiler(dtoFile, elements, DtoModifier.STATIC)
                        val typeElement: TypeElement = elements.getTypeElement(compiler.sourceTypeName) ?: return
                        registerProblem(file, document, holder) {
                            compiler.compile(context.getImmutableType(typeElement))
                        }
                    } catch (_: Throwable) {

                    }
                } else if (project.workspace().isKotlinProject) {
                    val typeClass =
                        KotlinFullClassNameIndex[typeName, project, project.allScope()].firstOrNull() as? KtClass
                            ?: return
                    val (resolver, environment, sources) =
                        project.ktClassesToKsp(setOf(typeClass))
                    val context = Context(resolver, environment)
                    val dtoFile = DtoFile(object : OsFile {
                        override fun getAbsolutePath(): String {
                            return path.absolutePathString()
                        }

                        override fun openReader(): Reader {
                            return file.text.reader()
                        }
                    }, findProjectDir(path)?.name ?: "", "", emptyList(), path.name)
                    try {
                        val compiler = KspDtoCompiler(dtoFile, context.resolver, DtoModifier.STATIC)
                        val classDeclarationByName =
                            resolver.getClassDeclarationByName(compiler.sourceTypeName) ?: return
                        registerProblem(file, document, holder) {
                            compiler.compile(context.typeOf(classDeclarationByName))
                        }
                    } catch (_: Throwable) {

                    }
                }
            }
        }
    }

    fun registerProblem(element: PsiElement, document: Document, holder: ProblemsHolder, block: () -> Unit) {
        try {
            block()
        } catch (dtoAst: DtoAstException) {
            val line = dtoAst.lineNumber - 1
            val column = dtoAst.colNumber
            element.findElementAt(document.getLineStartOffset(line) + column)?.also { current ->
                dtoAst.message?.also { message ->
                    holder.registerProblem(current, message.substringAfter(" : "))
                }
            }
        } catch (_: Throwable) {
        }
    }
}