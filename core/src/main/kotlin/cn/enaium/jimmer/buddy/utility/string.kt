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

import java.util.*

/**
 * @author Enaium
 */
fun String.snakeToCamelCase(
    firstCharUppercase: Boolean = true,
): String {
    return this.split("_")
        .joinToString("") {
            it.replaceFirstChar { firstChar -> firstChar.uppercase(Locale.getDefault()) }
        }.let {
            if (!firstCharUppercase) {
                it.replaceFirstChar { firstChar -> firstChar.lowercase(Locale.getDefault()) }
            } else {
                it
            }
        }
}

fun String.firstCharLowercase(): String {
    return this.replaceFirstChar { firstChar -> firstChar.lowercase(Locale.getDefault()) }
}

fun String.firstCharUppercase(): String {
    return this.replaceFirstChar { firstChar -> firstChar.uppercase(Locale.getDefault()) }
}

fun String.toPlural(): String {
    return if (this.matches(Regex(".*(s|x|z|sh|ch)$"))) {
        "${this}es"
    } else if (this.matches(Regex(".*[^aeiou]y$"))) {
        "${this.substring(0, this.length - 1)}ies"
    } else if (this.matches(Regex(".*[^aeiou]o$"))) {
        "${this}es"
    } else if (this.matches(Regex(".*[^aeiou]f$"))) {
        "${this.substring(0, this.length - 1)}ves"
    } else if (this.matches(Regex(".*[^aeiou]fe$"))) {
        "${this.substring(0, this.length - 2)}ves"
    } else {
        "${this}s"
    }
}

fun String.subMiddle(left: String, right: String): String {
    return this.substringAfter(left).substringBeforeLast(right)
}

fun String.camelToSnakeCase(): String {
    if (this.isEmpty()) return ""

    val result = StringBuilder()
    result.append(this[0].lowercaseChar())

    for (i in 1 until this.length) {
        val current = this[i]
        val previous = this[i - 1]

        if (current.isUpperCase()) {
            if (previous.isLowerCase() ||
                (i < this.length - 1 && this[i + 1].isLowerCase())
            ) {
                result.append('_')
            }
            result.append(current.lowercaseChar())
        } else {
            result.append(current)
        }
    }

    return result.toString()
}