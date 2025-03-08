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

import javax.annotation.processing.Filer
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

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
        true
    )
}