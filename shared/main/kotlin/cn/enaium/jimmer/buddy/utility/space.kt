package cn.enaium.jimmer.buddy.utility

import com.intellij.formatting.SpacingBuilder
import com.intellij.psi.tree.IElementType

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

fun SpacingBuilder.RuleBuilder.emptyLine(count: Int): SpacingBuilder {
    return this.spacing(0, 0, count + 1, false, 0)
}

fun SpacingBuilder.around(elementType: IElementType, left: Int, right: Int): SpacingBuilder {
    return before(elementType).spaces(left).after(elementType).spaces(right)
}