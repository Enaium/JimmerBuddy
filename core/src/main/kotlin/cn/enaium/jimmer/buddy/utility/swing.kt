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

import com.intellij.ui.treeStructure.Tree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode

/**
 * @author Enaium
 */
fun Tree.sortByName(root: DefaultMutableTreeNode) {
    val sortedBy = root.children().toList().sortedBy {
        it.toString()
    }
    root.removeAllChildren()
    for (child in sortedBy) {
        root.add(child as MutableTreeNode)
    }
    (this.model as DefaultTreeModel).nodeStructureChanged(root)
}

fun Tree.sortByChildCount(root: DefaultMutableTreeNode) {
    val sortedBy = root.children().toList().sortedBy {
        (it as DefaultMutableTreeNode).childCount
    }
    root.removeAllChildren()
    for (child in sortedBy) {
        root.add(child as MutableTreeNode)
    }
    (this.model as DefaultTreeModel).nodeStructureChanged(root)
}