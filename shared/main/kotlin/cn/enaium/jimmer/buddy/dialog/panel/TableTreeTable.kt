package cn.enaium.jimmer.buddy.dialog.panel

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.database.model.Column
import cn.enaium.jimmer.buddy.database.model.Table
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckboxTreeTable
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.treeStructure.treetable.TreeColumnInfo
import com.intellij.util.ui.ColumnInfo
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.DefaultCellEditor
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.JTree
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

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
        val treeTable = CheckboxTreeTable(
            root,
            DefaultNodeCell(),
            arrayOf(TreeColumnInfo("Name"), ColumnType("Type"), ColumnNullable("Nullable"), ColumnComment("Comment"))
        )
        add(JBScrollPane(treeTable), BorderLayout.CENTER)
    }

    private class ColumnType(name: String) : ColumnInfo<Any, Any>(name) {
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

    private class ColumnComment(name: String) : ColumnInfo<Any, Any>(name) {
        override fun valueOf(p0: Any?): Any? {
            return when (p0) {
                is TableNode -> p0.table.remark
                is ColumnNode -> p0.column.remark
                else -> null
            }
        }

        override fun isCellEditable(item: Any): Boolean {
            return item is DefaultNode
        }

        override fun getEditor(item: Any): TableCellEditor? {
            return if (item is TableNode) {
                DefaultCellEditor(JBTextField().apply {
                    document.addDocumentListener(object : DocumentListener {
                        override fun insertUpdate(e: DocumentEvent) {
                            item.table.remark = text
                        }

                        override fun removeUpdate(e: DocumentEvent) {
                            item.table.remark = text
                        }

                        override fun changedUpdate(e: DocumentEvent) {
                            item.table.remark = text
                        }
                    })
                })
            } else if (item is ColumnNode) {
                DefaultCellEditor(JBTextField().apply {
                    document.addDocumentListener(object : DocumentListener {
                        override fun insertUpdate(e: DocumentEvent) {
                            item.column.remark = text
                        }

                        override fun removeUpdate(e: DocumentEvent) {
                            item.column.remark = text
                        }

                        override fun changedUpdate(e: DocumentEvent) {
                            item.column.remark = text
                        }
                    })
                })
            } else {
                null
            }
        }
    }

    private class ColumnNullable(name: String) : ColumnInfo<Any, Any>(name) {
        override fun valueOf(p0: Any?): Any? {
            return (p0 as? ColumnNode)?.column?.nullable
        }

        override fun isCellEditable(item: Any): Boolean {
            return item is ColumnNode
        }

        override fun getEditor(item: Any): TableCellEditor? {
            return if (item is ColumnNode) {
                DefaultCellEditor(JBCheckBox().apply {
                    isSelected = item.column.nullable
                    addActionListener {
                        item.column.nullable = isSelected
                    }
                })
            } else {
                null
            }
        }

        override fun getRenderer(item: Any): TableCellRenderer? {
            return (item as? ColumnNode)?.column?.nullable?.let {
                object : TableCellRenderer {
                    override fun getTableCellRendererComponent(
                        table: JTable,
                        value: Any,
                        isSelected: Boolean,
                        hasFocus: Boolean,
                        row: Int,
                        column: Int
                    ): Component {
                        return JBCheckBox().apply {
                            this.isSelected = value as Boolean
                        }
                    }
                }
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