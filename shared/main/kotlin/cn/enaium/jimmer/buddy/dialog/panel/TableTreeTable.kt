package cn.enaium.jimmer.buddy.dialog.panel

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.database.model.Column
import cn.enaium.jimmer.buddy.database.model.Table
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckboxTreeTable
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.treetable.TreeColumnInfo
import com.intellij.util.ui.ColumnInfo
import java.awt.BorderLayout
import javax.swing.ComboBoxModel
import javax.swing.DefaultCellEditor
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.table.TableCellEditor

/**
 * @author Enaium
 */
class TableTreeTable(val tables: Set<Table>) : JPanel() {
    private val root = DefaultNode()

    init {
        layout = BorderLayout()
        tables.forEach { table ->
            val tableNode = TableNode(table)
            table.columns.forEach { column ->
                tableNode.add(ColumnNode(column))
            }
            root.add(tableNode)
        }
        val treeTable = CheckboxTreeTable(root, DefaultNodeCell(), arrayOf(TreeColumnInfo("Name"), ColumnType("Type")))
        add(JBScrollPane(treeTable), BorderLayout.CENTER)
    }

    private class ColumnType(name: String) : ColumnInfo<Any, Any>(name) {
        override fun getColumnClass(): Class<*> {
            return ComboBoxModel::class.java
        }

        override fun valueOf(p0: Any): Any? {
            return (p0 as? ColumnNode)?.column?.type
        }

        override fun isCellEditable(item: Any): Boolean {
            return item is ColumnNode
        }

        override fun getEditor(item: Any): TableCellEditor? {
            return if (item is ColumnNode) {
                DefaultCellEditor(ComboBox<String>().apply {
                    selectedItem = item.column.type
                    cn.enaium.jimmer.buddy.utility.ColumnType.entries.forEach {
                        addItem(it.name.lowercase())
                    }
                    addActionListener {
                        selectedItem?.also {
                            item.column.type = it.toString()
                        }
                    }
                })
            } else {
                null
            }
        }
    }

    private inner class DefaultNodeCell() : CheckboxTree.CheckboxTreeCellRenderer() {
        override fun customizeRenderer(
            tree: JTree,
            value: Any,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ) {
            textRenderer.icon = when (value) {
                is TableNode -> JimmerBuddy.Icons.Database.TABLE
                is ColumnNode -> if (tables.find { it.name == value.column.tableName }?.primaryKeys?.any { it.column.name == value.column.name } == true) {
                    JimmerBuddy.Icons.Database.COLUMN_GOLD_KEY
                } else if (tables.find { it.name == value.column.tableName }?.foreignKeys?.any { it.column.name == value.column.name } == true) {
                    JimmerBuddy.Icons.Database.COLUMN_BLUE_KEY
                } else {
                    JimmerBuddy.Icons.Database.COLUMN
                }

                else -> null
            }
            textRenderer.append(value.toString())
        }
    }

    fun getResult(): Set<Table> {
        return tables.mapNotNull { table ->
            root.children().toList().filterIsInstance<TableNode>()
                .find { it.table.name == table.name && it.isChecked() }
                ?.let { tableNode ->
                    table.copy(columns = table.columns.filter { column ->
                        tableNode.children().toList().filterIsInstance<ColumnNode>()
                            .any { it.column.name == column.name && it.isChecked() }
                    }.toSet())
                }
        }.toSet()
    }

    private open class DefaultNode() : CheckedTreeNode() {
        init {
            isChecked = true
        }
    }

    private class TableNode(val table: Table) : DefaultNode() {
        override fun toString(): String {
            return table.name
        }
    }

    private class ColumnNode(val column: Column) : DefaultNode() {
        override fun toString(): String {
            return column.name
        }
    }
}