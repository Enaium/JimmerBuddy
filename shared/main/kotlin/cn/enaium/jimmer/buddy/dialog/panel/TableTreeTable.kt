package cn.enaium.jimmer.buddy.dialog.panel

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.database.model.*
import com.intellij.icons.AllIcons
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
import javax.swing.*
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
            table.columns.takeIf { it.isNotEmpty() }?.also { columns ->
                tableNode.add(FolderNode(FolderNode.Type.COLUMN).apply {
                    columns.forEach { column ->
                        add(ColumnNode(column))
                    }
                })
            }
            table.primaryKeys.takeIf { it.isNotEmpty() }?.also { keys ->
                tableNode.add(FolderNode(FolderNode.Type.KEY).apply {
                    keys.forEach { key ->
                        add(KeyNode(key))
                    }
                })
            }
            table.foreignKeys.takeIf { it.isNotEmpty() }?.also { foreignKeys ->
                tableNode.add(FolderNode(FolderNode.Type.FOREIGN_KEY).apply {
                    foreignKeys.forEach { foreignKey ->
                        add(ForeignKeyNode(foreignKey))
                    }
                })
            }
            table.uniqueKeys.takeIf { it.isNotEmpty() }?.also { indexes ->
                tableNode.add(FolderNode(FolderNode.Type.INDEX).apply {
                    indexes.forEach { index ->
                        add(IndexNode(index))
                    }
                })
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
            return item is TableNode || item is ColumnNode
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
                    horizontalAlignment = SwingUtilities.CENTER
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
                        column: Int,
                    ): Component {
                        return JBCheckBox().apply {
                            this.isSelected = value as Boolean
                            horizontalAlignment = SwingUtilities.CENTER
                        }
                    }
                }
            }
        }

        override fun getWidth(table: JTable?): Int {
            return 70
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
            hasFocus: Boolean,
        ) {
            textRenderer.icon = when (value) {
                is TableNode -> JimmerBuddy.Icons.Database.TABLE
                is FolderNode -> AllIcons.Modules.SourceRoot
                is ColumnNode -> if (tables.find { it.name == value.column.tableName }?.primaryKeys?.any { it.column.name == value.column.name } == true) {
                    JimmerBuddy.Icons.Database.COLUMN_GOLD_KEY
                } else if (tables.find { it.name == value.column.tableName }?.foreignKeys?.any { it.column.name == value.column.name } == true) {
                    JimmerBuddy.Icons.Database.COLUMN_BLUE_KEY
                } else {
                    JimmerBuddy.Icons.Database.COLUMN
                }

                is KeyNode -> JimmerBuddy.Icons.Database.COLUMN_GOLD_KEY
                is ForeignKeyNode -> JimmerBuddy.Icons.Database.COLUMN_BLUE_KEY
                is IndexNode -> JimmerBuddy.Icons.Database.INDEX
                else -> null
            }
            textRenderer.append(value.toString())
        }
    }

    fun getResult(): Set<Table> {
        return tables.mapNotNull { table ->
            root.children().toList().filterIsInstance<TableNode>()
                .find { it.table.name == table.name && it.choose() }
                ?.let { tableNode ->
                    val children = tableNode.children().toList()
                    table.copy(
                        columns = table.columns.filter { column ->
                            children.filterIsInstance<FolderNode>().find { it.type == FolderNode.Type.COLUMN }
                                ?.children()?.toList()?.filterIsInstance<ColumnNode>()
                                ?.any { it.column.name == column.name && it.choose() } == true
                        }.toSet(),
                        primaryKeys = table.primaryKeys.filter { key ->
                            children.filterIsInstance<FolderNode>().find { it.type == FolderNode.Type.KEY }
                                ?.children()?.toList()?.filterIsInstance<KeyNode>()
                                ?.any { it.primaryKey.name == key.name && it.choose() } == true
                        }.toSet(),
                        foreignKeys = table.foreignKeys.filter { foreignKey ->
                            children.filterIsInstance<FolderNode>().find { it.type == FolderNode.Type.FOREIGN_KEY }
                                ?.children()?.toList()?.filterIsInstance<ForeignKeyNode>()
                                ?.any { it.foreignKey.name == foreignKey.name && it.choose() } == true
                        }.toMutableSet(),
                        uniqueKeys = table.uniqueKeys.filter { index ->
                            children.filterIsInstance<FolderNode>().find { it.type == FolderNode.Type.INDEX }
                                ?.children()?.toList()?.filterIsInstance<IndexNode>()
                                ?.any { it.uniqueKey.name == index.name && it.choose() } == true
                        }.toSet()
                    )
                }
        }.toSet()
    }

    private open class DefaultNode() : CheckedTreeNode() {
        init {
            isChecked = true
        }

        fun choose(parent: DefaultNode = this): Boolean {
            if (parent.isChecked()) {
                return true
            }
            val children = parent.children()
            while (children.hasMoreElements()) {
                val node = children.nextElement() as DefaultNode
                if (choose(node)) {
                    return true
                }
            }
            return false
        }
    }

    private class FolderNode(val type: Type) : DefaultNode() {
        enum class Type(val title: String) {
            COLUMN("columns"), KEY("keys"), FOREIGN_KEY("foreign keys"), INDEX("indexes")
        }

        override fun toString(): String {
            return type.title
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

    private class KeyNode(val primaryKey: PrimaryKey) : DefaultNode() {
        override fun toString(): String {
            return primaryKey.name
        }
    }

    private class ForeignKeyNode(val foreignKey: ForeignKey) : DefaultNode() {
        override fun toString(): String {
            return foreignKey.name
        }
    }

    private class IndexNode(val uniqueKey: UniqueKey) : DefaultNode() {
        override fun toString(): String {
            return uniqueKey.name
        }
    }
}