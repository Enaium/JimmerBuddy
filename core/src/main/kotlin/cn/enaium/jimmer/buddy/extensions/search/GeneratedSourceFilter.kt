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

package cn.enaium.jimmer.buddy.extensions.search

import cn.enaium.jimmer.buddy.utility.isJimmerGeneratedFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.GeneratedSourcesFilter
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.usages.Usage
import com.intellij.usages.rules.GeneratedSourceUsageFilter
import com.intellij.usages.rules.UsageFilteringRule
import com.intellij.usages.rules.UsageFilteringRuleProvider
import com.intellij.usages.rules.UsageInFile
import com.intellij.usages.UsageTarget

/**
 * @author Enaium
 */
class GeneratedSourceFilter : GeneratedSourcesFilter(), GeneratedSourceUsageFilter {
    override fun isAvailable(): Boolean {
        return true
    }

    override fun isGeneratedSource(file: VirtualFile, project: Project): Boolean {
        return isJimmerGeneratedFile(file.path)
    }

    override fun isGeneratedSource(usage: Usage, project: Project): Boolean {
        val file = (usage as? UsageInFile)?.file ?: return false
        return isGeneratedSource(file, project)
    }
}

/**
 * @author Enaium
 */
class GeneratedSourceUsageFilteringRuleProvider : UsageFilteringRuleProvider {
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getActiveRules(project: Project): Array<UsageFilteringRule> {
        return arrayOf(GeneratedSourceUsageFilteringRule)
    }
}

private object GeneratedSourceUsageFilteringRule : UsageFilteringRule {
    override fun getRuleId(): String {
        return "JimmerBuddy.GeneratedSource"
    }

    override fun isVisible(usage: Usage, targets: Array<UsageTarget>): Boolean {
        val file = (usage as? UsageInFile)?.file ?: return true
        return !isJimmerGeneratedFile(file.path)
    }
}
