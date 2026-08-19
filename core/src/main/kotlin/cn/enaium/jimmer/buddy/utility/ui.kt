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

import com.intellij.icons.AllIcons
import com.intellij.ide.util.PackageChooserDialog
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.util.bind
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.BrowseFolderDescriptor.Companion.withPathToTextConvertor
import com.intellij.openapi.ui.BrowseFolderDescriptor.Companion.withTextToPathConvertor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.addExtension
import com.intellij.openapi.ui.getCanonicalPath
import com.intellij.openapi.ui.getPresentablePath
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.SearchTextField
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.treeStructure.Tree
import java.net.URI
import java.util.*
import javax.swing.Icon
import javax.swing.JTree
import javax.swing.event.DocumentEvent
import javax.swing.event.EventListenerList
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeModel
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath
import kotlin.io.path.*


/**
 * @author Enaium
 */
fun Row.projectLocationField(
    locationProperty: GraphProperty<String>,
    wizardContext: WizardContext,
): Cell<TextFieldWithBrowseButton> {
    val fileChooserDescriptor =
        FileChooserDescriptorFactory.createSingleLocalFileDescriptor()
            .withFileFilter { it.isDirectory }
            .withPathToTextConvertor(::getPresentablePath)
            .withTextToPathConvertor(::getCanonicalPath)
    val title = IdeBundle.message("title.select.project.file.directory", wizardContext.presentationName)
    val property = locationProperty.transform(::getPresentablePath, ::getCanonicalPath)
    return textFieldWithBrowseButton(title, wizardContext.project, fileChooserDescriptor).bindText(property)
}

fun Row.packageChooserField(
    project: Project,
    property: GraphProperty<String>,
): Cell<ExtendableTextField> {
    return cell(ExtendableTextField().apply {
        bind(property)
        addExtension(AllIcons.Nodes.Package) {
            val packageChooserDialog = PackageChooserDialog("Package Chooser", project)
            if (packageChooserDialog.showAndGet()) {
                packageChooserDialog.selectedPackage?.qualifiedName?.also {
                    property.set(it)
                }
            }
        }
    })
}

fun Row.relativeLocationField(
    project: Project,
    property: GraphProperty<String>,
): Cell<TextFieldWithBrowseButton> {
    val fileChooserDescriptor =
        FileChooserDescriptorFactory.createSingleFolderDescriptor()
            .withFileFilter { it.isDirectory }
            .withPathToTextConvertor {
                getPresentablePath(
                    Path(it).relativeTo(
                        project.guessProjectDir()?.toNioPath() ?: return@withPathToTextConvertor getPresentablePath(it)
                    ).pathString
                )
            }
            .withTextToPathConvertor {
                getCanonicalPath(
                    project.guessProjectDir()?.toNioPath()?.resolve(it)?.absolutePathString()
                        ?: return@withTextToPathConvertor getCanonicalPath(it)
                )
            }
    return textFieldWithBrowseButton("Select Source", project, fileChooserDescriptor).bindText(property)
}

fun Row.fileChooserField(
    property: GraphProperty<String>,
    extension: String,
    uri: Boolean = false
): Cell<TextFieldWithBrowseButton> {
    val fileChooserDescriptor =
        FileChooserDescriptorFactory.createSingleLocalFileDescriptor()
            .withFileFilter { it.extension == extension }
            .withPathToTextConvertor {
                if (uri) {
                    try {
                        Path(it).toUri().toString()
                    } catch (_: Throwable) {
                        getPresentablePath(it)
                    }
                } else {
                    getPresentablePath(it)
                }
            }
            .withTextToPathConvertor {
                if (uri) {
                    try {
                        URI(it).toPath().toAbsolutePath().pathString
                    } catch (_: Throwable) {
                        getCanonicalPath(it)
                    }
                } else {
                    getCanonicalPath(it)
                }
            }
    return textFieldWithBrowseButton("File Chooser", null, fileChooserDescriptor).bindText(property)
}

fun Tree.expandAll(parent: TreePath) {
    val node = parent.lastPathComponent as TreeNode
    if (node.childCount >= 0) {
        val e: Enumeration<*> = node.children()
        while (e.hasMoreElements()) {
            val n = e.nextElement() as TreeNode
            val path = parent.pathByAddingChild(n)
            expandAll(path)
        }
    }
    expandPath(parent)
}

fun Tree.createSearchField(
    matcher: (TreeNode, String) -> Boolean = { node, query -> node.matchesNameOrDescendant(query) }
): SearchTextField {
    val filteredModel = FilteredTreeModel(model as DefaultTreeModel, matcher)
    model = filteredModel
    val searchField = SearchTextField(false)
    searchField.textEditor.emptyText.text = I18n.message("toolwindow.buddy.search.placeholder")
    searchField.addDocumentListener(object : DocumentAdapter() {
        override fun textChanged(event: DocumentEvent) {
            filteredModel.filter(searchField.text)
        }
    })
    return searchField
}

fun Tree.showWaitingForIndexes() {
    emptyText.text = I18n.message("toolwindow.buddy.indexing")
}

fun Tree.showEmptyState() {
    emptyText.text = I18n.message("toolwindow.buddy.empty")
}

fun Tree.nodeStructureChanged(root: TreeNode) {
    val defaultModel = when (val currentModel = model) {
        is FilteredTreeModel -> currentModel.delegate
        is DefaultTreeModel -> currentModel
        else -> error("Unsupported tree model: ${currentModel.javaClass.name}")
    }
    defaultModel.nodeStructureChanged(root)
}

fun String.matchesFuzzy(query: String): Boolean {
    return fuzzyMatchIndices(query).isNotEmpty()
}

fun JTree.searchTextComponent(text: String, icon: Icon?, selected: Boolean): SimpleColoredComponent {
    val component = SimpleColoredComponent()
    component.icon = icon
    val regularAttributes = if (selected) {
        SimpleTextAttributes.SELECTED_SIMPLE_CELL_ATTRIBUTES
    } else {
        SimpleTextAttributes.REGULAR_ATTRIBUTES
    }
    val highlightAttributes = SimpleTextAttributes.merge(regularAttributes, SEARCH_MATCH_ATTRIBUTES)
    val matchedIndices = text.fuzzyMatchIndices(searchQuery())
    component.appendHighlighted(text, matchedIndices, regularAttributes, highlightAttributes)
    return component
}

private val SEARCH_MATCH_ATTRIBUTES = SimpleTextAttributes(SimpleTextAttributes.STYLE_SEARCH_MATCH, null)

private fun String.fuzzyMatchIndices(query: String): Set<Int> {
    if (query.isEmpty()) {
        return emptySet()
    }
    val directStart = indexOf(query, ignoreCase = true)
    if (directStart >= 0) {
        return (directStart until directStart + query.length).toSet()
    }
    val candidate = lowercase(Locale.ROOT)
    val expected = query.lowercase(Locale.ROOT)
    val indices = linkedSetOf<Int>()
    var queryIndex = 0
    for ((candidateIndex, character) in candidate.withIndex()) {
        if (character == expected[queryIndex]) {
            indices.add(candidateIndex)
            queryIndex++
            if (queryIndex == expected.length) {
                return indices
            }
        }
    }
    return emptySet()
}

private fun JTree.searchQuery(): String {
    return (model as? FilteredTreeModel)?.query.orEmpty()
}

private fun SimpleColoredComponent.appendHighlighted(
    text: String,
    matchedIndices: Set<Int>,
    regularAttributes: SimpleTextAttributes,
    highlightAttributes: SimpleTextAttributes
) {
    if (text.isEmpty()) {
        return
    }
    var segmentStart = 0
    var highlighted = 0 in matchedIndices
    for (index in 1..text.length) {
        val nextHighlighted = index in matchedIndices
        if (index < text.length && highlighted == nextHighlighted) {
            continue
        }
        val attributes = if (highlighted) highlightAttributes else regularAttributes
        append(text.substring(segmentStart, index), attributes)
        segmentStart = index
        highlighted = nextHighlighted
    }
}

private fun TreeNode.matchesNameOrDescendant(query: String): Boolean {
    if (toString().matchesFuzzy(query)) {
        return true
    }
    for (index in 0 until childCount) {
        if (getChildAt(index).matchesNameOrDescendant(query)) {
            return true
        }
    }
    return false
}

private class FilteredTreeModel(
    val delegate: DefaultTreeModel,
    private val matcher: (TreeNode, String) -> Boolean
) : TreeModel {
    private val listeners = EventListenerList()
    var query = ""
        private set
    private var visibleRootChildren = rootChildren()

    init {
        delegate.addTreeModelListener(object : TreeModelListener {
            override fun treeNodesChanged(event: TreeModelEvent) {
                refresh()
            }

            override fun treeNodesInserted(event: TreeModelEvent) {
                refresh()
            }

            override fun treeNodesRemoved(event: TreeModelEvent) {
                refresh()
            }

            override fun treeStructureChanged(event: TreeModelEvent) {
                refresh()
            }
        })
    }

    fun filter(value: String) {
        val normalized = value.trim()
        if (query == normalized) {
            return
        }
        query = normalized
        refresh()
    }

    override fun getRoot(): Any {
        return delegate.root
    }

    override fun getChild(parent: Any, index: Int): Any {
        if (parent === delegate.root) {
            return visibleRootChildren[index]
        }
        return delegate.getChild(parent, index)
    }

    override fun getChildCount(parent: Any): Int {
        if (parent === delegate.root) {
            return visibleRootChildren.size
        }
        return delegate.getChildCount(parent)
    }

    override fun isLeaf(node: Any): Boolean {
        if (node === delegate.root) {
            return visibleRootChildren.isEmpty()
        }
        return delegate.isLeaf(node)
    }

    override fun valueForPathChanged(path: TreePath, newValue: Any) {
        delegate.valueForPathChanged(path, newValue)
    }

    override fun getIndexOfChild(parent: Any, child: Any): Int {
        if (parent === delegate.root) {
            return visibleRootChildren.indexOf(child)
        }
        return delegate.getIndexOfChild(parent, child)
    }

    override fun addTreeModelListener(listener: TreeModelListener) {
        listeners.add(TreeModelListener::class.java, listener)
    }

    override fun removeTreeModelListener(listener: TreeModelListener) {
        listeners.remove(TreeModelListener::class.java, listener)
    }

    private fun refresh() {
        visibleRootChildren = rootChildren()
        val event = TreeModelEvent(this, arrayOf(delegate.root))
        listeners.getListeners(TreeModelListener::class.java).forEach { listener ->
            listener.treeStructureChanged(event)
        }
    }

    private fun rootChildren(): List<TreeNode> {
        val root = delegate.root as? TreeNode ?: return emptyList()
        val children = root.children().toList().filterIsInstance<TreeNode>()
        if (query.isEmpty()) {
            return children
        }
        return runReadOnly {
            children.filter { child -> matcher(child, query) }
        }
    }
}
