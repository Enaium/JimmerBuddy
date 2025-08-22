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

package cn.enaium.jimmer.buddy.extensions.icon

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.storage.JimmerBuddySetting
import cn.enaium.jimmer.buddy.utility.DTO_FOLDER
import cn.enaium.jimmer.buddy.utility.IMMUTABLE
import cn.enaium.jimmer.buddy.utility.hasImmutableAnnotation
import com.intellij.ide.IconProvider
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import javax.swing.Icon

/**
 * @author Enaium
 */
class JimmerIconProvider : IconProvider() {
    override fun getIcon(p0: PsiElement, p1: Int): Icon? {
        if (!JimmerBuddySetting.INSTANCE.state.icon) {
            return null
        }

        when (p0) {
            is PsiDirectory -> {
                if (p0.name == "dto" && (p0.parent?.name == "main" || p0.parent?.name == "test") && p0.parent?.parent?.name == "src") {
                    return JimmerBuddy.Icons.Nodes.DTO_FOLDER
                }
            }

            is PsiClass -> {
                if (p0.hasImmutableAnnotation()) {
                    return JimmerBuddy.Icons.IMMUTABLE
                }
            }

            is PsiFile -> {
                if (p0.getChildOfType<KtClass>()?.hasImmutableAnnotation() == true) {
                    return JimmerBuddy.Icons.IMMUTABLE
                }
            }

            is KtClass -> {
                if (p0.hasImmutableAnnotation()) {
                    return JimmerBuddy.Icons.IMMUTABLE
                }
            }
        }

        return null
    }
}
