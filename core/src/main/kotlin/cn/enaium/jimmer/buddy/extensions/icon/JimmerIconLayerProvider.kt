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
import cn.enaium.jimmer.buddy.utility.DTO_MARK
import com.intellij.ide.IconLayerProvider
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiDirectory
import javax.swing.Icon

/**
 * @author Enaium
 */
class JimmerIconLayerProvider : IconLayerProvider {
    override fun getLayerIcon(element: Iconable, isLocked: Boolean): Icon? {
        if (element is PsiDirectory) {
            if (element.name == "dto" && (element.parent?.name == "main" || element.parent?.name == "test") && element.parent?.parent?.name == "src") {
                return JimmerBuddy.Icons.Nodes.DTO_MARK
            }
        }

        return null
    }

    override fun getLayerDescription(): String {
        return "DTO DIR"
    }
}