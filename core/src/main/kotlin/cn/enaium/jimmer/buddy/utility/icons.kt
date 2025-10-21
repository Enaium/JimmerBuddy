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

import cn.enaium.jimmer.buddy.JimmerBuddy
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/**
 * @author Enaium
 */
val JimmerBuddy.Icons.LOGO: Icon
    get() = IconLoader.getIcon("/icons/logo.svg", JimmerBuddy::class.java)
val JimmerBuddy.Icons.LOGO_NORMAL: Icon
    get() = IconLoader.getIcon("/icons/normal.svg", JimmerBuddy::class.java)
val JimmerBuddy.Icons.IMMUTABLE: Icon
    get() = IconLoader.getIcon("/icons/immutable.svg", JimmerBuddy::class.java)
val JimmerBuddy.Icons.PROP: Icon
    get() = IconLoader.getIcon("/icons/prop.svg", JimmerBuddy::class.java)
val JimmerBuddy.Icons.DTO_FILE: Icon
    get() = IconLoader.getIcon("/icons/dto_file.svg", JimmerBuddy::class.java)

val JimmerBuddy.Icons.Nodes.DTO_MARK: Icon
    get() = IconLoader.getIcon("/icons/dto_mark.svg", JimmerBuddy::class.java)
val JimmerBuddy.Icons.Nodes.DTO_TYPE: Icon
    get() = IconLoader.getIcon("/icons/dto_type.svg", JimmerBuddy::class.java)
val JimmerBuddy.Icons.Nodes.DTO_PROP: Icon
    get() = IconLoader.getIcon("/icons/dto_prop.svg", JimmerBuddy::class.java)

val JimmerBuddy.Icons.Databases.DB: Icon
    get() = IconLoader.getIcon("/icons/database/dbms.svg", JimmerBuddy::class.java)
val JimmerBuddy.Icons.Databases.COLUMN_GOLD_KEY: Icon
    get() = IconLoader.getIcon("/icons/database/columnGoldKey.svg", JimmerBuddy::class.java)
val JimmerBuddy.Icons.Databases.COLUMN_BLUE_KEY: Icon
    get() = IconLoader.getIcon("/icons/database/columnBlueKey.svg", JimmerBuddy::class.java)
val JimmerBuddy.Icons.Databases.INDEX: Icon
    get() = IconLoader.getIcon("/icons/database/index.svg", JimmerBuddy::class.java)