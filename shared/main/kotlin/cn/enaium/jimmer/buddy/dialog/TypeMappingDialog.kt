package cn.enaium.jimmer.buddy.dialog

import cn.enaium.jimmer.buddy.storage.JimmerBuddySetting
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.borderPanel
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor

/**
 * @author Enaium
 */
class TypeMappingDialog : DialogWrapper(false) {

    val listTableModel =
        ListTableModel<Node>(
            ColumnType("Type") { node -> node.type },
            ColumnType("Java") { node -> node.javaType },
            ColumnType("Kotlin") { node -> node.kotlinType },
        ).apply {
            addRows(JimmerBuddySetting.INSTANCE.state.typeMapping.map {
                Node(
                    it.key,
                    it.value.javaType,
                    it.value.kotlinType
                )
            })
        }

    init {
        title = "Type Mapping"
        setSize(800, 600)
        init()
    }

    override fun createCenterPanel(): JComponent {
        return borderPanel {
            addToTop(borderPanel {
                addToRight(JButton("New").apply {
                    addActionListener {
                        listTableModel.addRow(Node("", "", ""))
                    }
                })
            })
            addToCenter(JBScrollPane(object : JBTable(listTableModel) {
                override fun getCellEditor(row: Int, column: Int): TableCellEditor? {
                    return DefaultCellEditor(JBTextField().apply {
                        fun change(text: String) {
                            when (getColumnName(column)) {
                                "Type" -> listTableModel.getItem(row).type = text.toString()
                                "Java" -> listTableModel.getItem(row).javaType = text.toString()
                                "Kotlin" -> listTableModel.getItem(row).kotlinType = text.toString()
                            }
                        }

                        document.addDocumentListener(object : DocumentListener {
                            override fun insertUpdate(e: DocumentEvent) {
                                change(text)
                            }

                            override fun removeUpdate(e: DocumentEvent) {
                                change(text)
                            }

                            override fun changedUpdate(e: DocumentEvent) {
                                change(text)
                            }
                        })
                    })
                }
            }.apply {
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        if (SwingUtilities.isRightMouseButton(e)) {
                            JBPopupMenu().apply {
                                add(JMenuItem("Remove").apply {
                                    addActionListener {
                                        listTableModel.removeRow(selectedRow)
                                    }
                                })
                            }.show(this@apply, e.x, e.y)
                        }
                    }
                })
            }))
        }
    }

    override fun doOKAction() {
        JimmerBuddySetting.INSTANCE.state.typeMapping =
            listTableModel.items.associate { it.type to JimmerBuddySetting.JavaToKotlin(it.javaType, it.kotlinType) }
        super.doOKAction()
    }

    private class ColumnType(
        name: String,
        val onRender: (node: Node) -> String
    ) :
        ColumnInfo<Any, Any>(name) {
        override fun valueOf(p0: Any): Any? {
            return if (p0 is Node) {
                onRender(p0)
            } else {
                null
            }
        }

        override fun isCellEditable(item: Any?): Boolean {
            return true
        }
    }

    data class Node(
        var type: String,
        var javaType: String,
        var kotlinType: String
    ) : DefaultTableModel()
}