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

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.storage.JimmerBuddySetting
import cn.enaium.jimmer.buddy.utility.I18n
import cn.enaium.jimmer.buddy.utility.LOGO_NORMAL
import cn.enaium.jimmer.buddy.utility.isGeneratedFile
import cn.enaium.jimmer.buddy.utility.isJimmerGeneratedFile
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.GeneratedSourcesFilter
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.usages.Usage
import com.intellij.usages.UsageTarget
import com.intellij.usages.UsageView
import com.intellij.usages.rules.UsageFilteringRule
import com.intellij.usages.rules.UsageFilteringRuleProvider
import com.intellij.usages.rules.UsageInFile
import java.util.EnumSet

/**
 * @author Enaium
 */
class GeneratedSourceFilter : GeneratedSourcesFilter() {
    override fun isGeneratedSource(file: VirtualFile, project: Project): Boolean {
        if (!isGeneratedSourceSearchFilterEnabled()) {
            return false
        }

        return isJimmerGeneratedFile(file.path)
    }
}

/**
 * @author Enaium
 */
class GeneratedSourceUsageFilteringRuleProvider : UsageFilteringRuleProvider {
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getActiveRules(project: Project): Array<UsageFilteringRule> {
        if (!isGeneratedSourceSearchFilterEnabled()) {
            return UsageFilteringRule.EMPTY_ARRAY
        }

        return arrayOf(JimmerUsageVisibilityFilteringRule)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun createFilteringActions(view: UsageView): Array<AnAction> {
        if (!isGeneratedSourceSearchFilterEnabled()) {
            return AnAction.EMPTY_ARRAY
        }

        return arrayOf(
            DefaultActionGroup(I18n.message("usage.filter.jimmer"), true).apply {
                templatePresentation.description = I18n.message("usage.filter.jimmer.description")
                templatePresentation.icon = JimmerBuddy.Icons.LOGO_NORMAL
                JimmerUsageFileKind.entries.forEach { kind ->
                    add(JimmerUsageFilterAction(kind))
                }
            }
        )
    }

    companion object {
        private val visibleKinds: EnumSet<JimmerUsageFileKind> = EnumSet.noneOf(JimmerUsageFileKind::class.java)

        private fun isVisible(kind: JimmerUsageFileKind): Boolean {
            return visibleKinds.contains(kind)
        }

        private fun setVisible(project: Project?, kind: JimmerUsageFileKind, visible: Boolean) {
            if (visible) {
                visibleKinds.add(kind)
            } else {
                visibleKinds.remove(kind)
            }
            project?.messageBus?.syncPublisher(UsageFilteringRuleProvider.RULES_CHANGED)?.run()
        }

        internal fun isUsageVisible(path: String): Boolean {
            return JimmerUsageFileKind.of(path)?.let { visibleKinds.contains(it) } ?: true
        }
    }

    private class JimmerUsageFilterAction(
        private val kind: JimmerUsageFileKind
    ) : DumbAwareToggleAction(
        I18n.message(kind.textKey),
        I18n.message(kind.descriptionKey),
        null
    ) {
        override fun isSelected(e: AnActionEvent): Boolean {
            return isVisible(kind)
        }

        override fun setSelected(e: AnActionEvent, state: Boolean) {
            setVisible(e.project, kind, state)
        }
    }
}

private object JimmerUsageVisibilityFilteringRule : UsageFilteringRule {
    override fun getRuleId(): String {
        return "JimmerBuddy.FilterGenerated.Visibility"
    }

    override fun isVisible(usage: Usage, targets: Array<UsageTarget>): Boolean {
        if (!isGeneratedSourceSearchFilterEnabled()) {
            return true
        }

        val file = (usage as? UsageInFile)?.file ?: return true
        return GeneratedSourceUsageFilteringRuleProvider.isUsageVisible(file.path)
    }
}

private enum class JimmerUsageFileKind(
    val textKey: String,
    val descriptionKey: String,
    private val suffixes: Set<String>,
    private val generatedOnly: Boolean = true,
    private val dtoFile: Boolean = false
) {
    PROPS("usage.filter.jimmer.props", "usage.filter.jimmer.props.description", setOf("Props")),
    DTO("usage.filter.jimmer.dto", "usage.filter.jimmer.dto.description", emptySet(), generatedOnly = false, dtoFile = true),
    DRAFT("usage.filter.jimmer.draft", "usage.filter.jimmer.draft.description", setOf("Draft", "DraftImpl", "DraftInterceptor")),
    VIEW("usage.filter.jimmer.view", "usage.filter.jimmer.view.description", setOf("View")),
    FETCHER("usage.filter.jimmer.fetcher", "usage.filter.jimmer.fetcher.description", setOf("Fetcher", "FetcherDsl")),
    TABLE("usage.filter.jimmer.table", "usage.filter.jimmer.table.description", setOf("Table", "TableEx")),
    INPUT("usage.filter.jimmer.input", "usage.filter.jimmer.input.description", setOf("Input")),
    SPECIFICATION("usage.filter.jimmer.specification", "usage.filter.jimmer.specification.description", setOf("Specification", "Spec"));

    companion object {
        fun of(path: String): JimmerUsageFileKind? {
            return entries.firstOrNull { kind -> kind.matches(path) }
        }
    }

    private fun matches(path: String): Boolean {
        val normalizedPath = path.replace('\\', '/')
        val fileName = normalizedPath.substringAfterLast('/')
        if (dtoFile && fileName.endsWith(".dto", ignoreCase = true)) {
            return true
        }

        if (generatedOnly && !isGeneratedFile(normalizedPath)) {
            return false
        }

        val sourceName = fileName.substringBeforeLast('.', fileName)
        return suffixes.any { sourceName.endsWith(it) }
    }
}

internal fun isGeneratedSourceSearchFilterEnabled(): Boolean {
    return JimmerBuddySetting.INSTANCE.state.filterGeneratedFilesInSearch
}
