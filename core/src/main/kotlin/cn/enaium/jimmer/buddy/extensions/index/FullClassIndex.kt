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
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType

/**
 * @author Enaium
 */
class FullClassIndex : ScalarIndexExtension<String>() {
    override fun getName(): ID<String, Void> {
        return JimmerBuddy.Indexes.FULL_CLASS
    }

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return DefaultFileTypeSpecificInputFilter(JavaFileType.INSTANCE, KotlinFileType.INSTANCE)
    }

    override fun dependsOnFileContent(): Boolean {
        return true
    }

    override fun getIndexer(): DataIndexer<String, Void, FileContent> {
        return DataIndexer<String, Void, FileContent> { file ->
            if (file.fileType == JavaFileType.INSTANCE) {
                file.psiFile.getChildrenOfType<PsiClass>()
                    .associate { Pair(it.qualifiedName, null) }
            } else {
                file.psiFile.getChildrenOfType<KtClass>()
                    .associate { Pair(it.fqName!!.asString(), null) }
            }
        }
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> {
        return EnumeratorStringDescriptor()
    }

    override fun getVersion(): Int {
        return 0
    }
}