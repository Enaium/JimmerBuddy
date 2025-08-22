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

package cn.enaium.jimmer.buddy.extensions.dto

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.utility.DTO_FILE
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object DtoFileType : LanguageFileType(DtoLanguage) {
    override fun getName(): String = JimmerBuddy.DTO_NAME

    override fun getDescription(): String = JimmerBuddy.DTO_NAME

    override fun getDefaultExtension(): String = "dto"

    override fun getIcon(): Icon = JimmerBuddy.Icons.DTO_FILE
}