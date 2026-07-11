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

package cn.enaium.jimmer.buddy.extensions.editor.panel

import cn.enaium.jimmer.buddy.utility.I18n
import cn.enaium.jimmer.buddy.utility.endOffset
import cn.enaium.jimmer.buddy.utility.runReadOnly
import cn.enaium.jimmer.buddy.utility.startOffset
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.panel
import org.babyfish.jimmer.Formula
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.sql.*
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.*
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JTextField
import kotlin.reflect.KClass

/**
 * @author Enaium
 */
class ImmutableInfo(private val project: Project, private val file: VirtualFile) : JPanel() {

    private val noSelected = JBLabel(I18n.message("editor.dto.label.noSelected"), JBLabel.CENTER)
    private val contentPanel = JPanel(BorderLayout())

    /** Currently displayed element, stored so we can refresh after annotation changes. */
    private var currentElement: SmartPsiElementPointer<PsiElement>? = null

    init {
        layout = BorderLayout()
        contentPanel.add(noSelected, BorderLayout.CENTER)
        add(JBScrollPane(contentPanel).apply {
            horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        }, BorderLayout.CENTER)
    }

    fun show(element: SmartPsiElementPointer<PsiElement>?) {
        contentPanel.removeAll()
        currentElement = element

        if (element == null) {
            contentPanel.add(noSelected, BorderLayout.CENTER)
        } else {
            runReadOnly {
                val infoPanel = try {
                    when (val resolved = resolveElementInCurrentFile(element.element ?: return@runReadOnly)) {
                        is PsiClass -> buildClassInfo(resolved)
                        is KtClass -> buildKtClassInfo(resolved)
                        is PsiMethod -> buildJavaPropertyInfo(resolved)
                        is KtProperty -> buildKotlinPropertyInfo(resolved)
                        else -> noSelected
                    }
                } catch (_: Exception) {
                    noSelected
                }
                contentPanel.add(JBScrollPane(infoPanel).apply {
                    horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                }, BorderLayout.CENTER)
            }
        }

        contentPanel.revalidate()
        contentPanel.repaint()
    }

    /**
     * Resolve the equivalent element in the current PSI file.
     * The element from [ImmutableTree] may belong to a stale [com.intellij.psi.FileViewProvider].
     * We extract the element name and type from the (possibly stale) element, then find the
     * matching element in the current PSI file obtained via [PsiManager].
     */
    private fun resolveElementInCurrentFile(element: PsiElement): PsiElement? {
        val currentFile = PsiManager.getInstance(project).findFile(file) ?: return null

        return when (element) {
            is PsiClass -> {
                val name = element.name ?: return null
                (currentFile as? PsiJavaFile)?.classes?.find { it.name == name }
            }

            is KtClass -> {
                val name = element.name ?: return null
                findKtClassInFile(currentFile as? KtFile ?: return null, name)
            }

            is PsiMethod -> {
                val methodName = element.name
                val parentClassName = element.containingClass?.name ?: return null
                val freshParent = (currentFile as? PsiJavaFile)?.classes?.find { it.name == parentClassName }
                    ?: return null
                freshParent.methods.find { it.name == methodName }
            }

            is KtProperty -> {
                val propName = element.name ?: return null
                val parentClass = PsiTreeUtil.getParentOfType(element, KtClass::class.java, true)
                    ?: return null
                val parentClassName = parentClass.name ?: return null
                val freshParent = findKtClassInFile(currentFile as? KtFile ?: return null, parentClassName)
                    ?: return null
                freshParent.getProperties().find { it.name == propName }
            }

            else -> null
        }
    }

    /**
     * Find a [KtClass] by name in the given file, including nested classes.
     */
    private fun findKtClassInFile(ktFile: KtFile, name: String): KtClass? {
        return PsiTreeUtil.collectElementsOfType(ktFile, KtClass::class.java)
            .find { it.name == name }
    }

    // ==================== Class Info ====================

    private fun buildClassInfo(psiClass: PsiClass): JPanel {
        return panel {
            row(I18n.message("editor.immutable.label.name")) {
                label(psiClass.name ?: "")
            }

            val typeDescription = when {
                psiClass.hasAnno(Entity::class) -> I18n.message("editor.immutable.type.entity")
                psiClass.hasAnno(MappedSuperclass::class) -> I18n.message("editor.immutable.type.mappedSuperclass")
                psiClass.hasAnno(Embeddable::class) -> I18n.message("editor.immutable.type.embeddable")
                psiClass.hasAnno(Immutable::class) -> I18n.message("editor.immutable.type.immutable")
                else -> I18n.message("editor.immutable.type.unknown")
            }

            row(I18n.message("editor.immutable.label.type")) {
                label(typeDescription)
            }

            val annotations = mutableListOf<String>()
            if (psiClass.hasAnno(Immutable::class)) annotations.add("@Immutable")
            if (psiClass.hasAnno(Entity::class)) annotations.add("@Entity")
            if (psiClass.hasAnno(MappedSuperclass::class)) annotations.add("@MappedSuperclass")
            if (psiClass.hasAnno(Embeddable::class)) annotations.add("@Embeddable")

            if (annotations.isNotEmpty()) {
                row(I18n.message("editor.immutable.label.annotations")) {
                    label(annotations.joinToString(", "))
                }
            }

            // @Table — only for @Entity
            if (psiClass.hasAnno(Entity::class)) {
                addJavaTableRow(psiClass)
            }
        }
    }

    private fun buildKtClassInfo(ktClass: KtClass): JPanel {
        return panel {
            row(I18n.message("editor.immutable.label.name")) {
                label(ktClass.name ?: "")
            }

            val typeDescription = when {
                ktClass.hasKtAnno(Entity::class) -> I18n.message("editor.immutable.type.entity")
                ktClass.hasKtAnno(MappedSuperclass::class) -> I18n.message("editor.immutable.type.mappedSuperclass")
                ktClass.hasKtAnno(Embeddable::class) -> I18n.message("editor.immutable.type.embeddable")
                ktClass.hasKtAnno(Immutable::class) -> I18n.message("editor.immutable.type.immutable")
                else -> I18n.message("editor.immutable.type.unknown")
            }

            row(I18n.message("editor.immutable.label.type")) {
                label(typeDescription)
            }

            val annotations = mutableListOf<String>()
            if (ktClass.hasKtAnno(Immutable::class)) annotations.add("@Immutable")
            if (ktClass.hasKtAnno(Entity::class)) annotations.add("@Entity")
            if (ktClass.hasKtAnno(MappedSuperclass::class)) annotations.add("@MappedSuperclass")
            if (ktClass.hasKtAnno(Embeddable::class)) annotations.add("@Embeddable")

            if (annotations.isNotEmpty()) {
                row(I18n.message("editor.immutable.label.annotations")) {
                    label(annotations.joinToString(", "))
                }
            }

            // @Table — only for @Entity
            if (ktClass.hasKtAnno(Entity::class)) {
                addKotlinTableRow(ktClass)
            }
        }
    }

    // ==================== Java Property Info ====================

    private fun buildJavaPropertyInfo(method: PsiMethod): JPanel {
        return panel {
            row(I18n.message("editor.immutable.label.name")) {
                label(method.name)
            }

            val returnType = method.returnType?.presentableText ?: ""
            row(I18n.message("editor.immutable.label.propType")) {
                label(returnType)
            }

            // Primary Key: @Id + @GeneratedValue
            if (method.hasAnno(Id::class)) {
                row(I18n.message("editor.immutable.label.id")) {
                    label(I18n.message("editor.immutable.label.primaryKey"))
                }
                addJavaGeneratedValueRow(method)
            }

            // Business Key: @Key (only for table column properties)
            val isTableColumn = !method.hasAnno(IdView::class) && !method.hasAnno(Formula::class)
                    && !method.hasAnno(Transient::class) && !method.hasAnno(OneToOne::class)
                    && !method.hasAnno(ManyToOne::class) && !method.hasAnno(OneToMany::class)
                    && !method.hasAnno(ManyToMany::class)
            if (isTableColumn) {
                addJavaKeyRow(method)
                addJavaColumnRow(method)
            }

            // View Property: @IdView
            if (method.hasAnno(IdView::class)) {
                row(I18n.message("editor.immutable.label.idView")) {
                    label(I18n.message("editor.immutable.label.viewProperty"))
                }
            }

            // Computed: @Formula (simple)
            if (method.hasAnno(Formula::class)) {
                row(I18n.message("editor.immutable.label.formula")) {
                    label(I18n.message("editor.immutable.label.simpleComputed"))
                }
            }

            // Computed: @Transient (complex)
            if (method.hasAnno(Transient::class)) {
                row(I18n.message("editor.immutable.label.transient")) {
                    label(I18n.message("editor.immutable.label.complexComputed"))
                }
            }

            // Association mappings
            val hasOneToOne = method.hasAnno(OneToOne::class)
            val hasManyToOne = method.hasAnno(ManyToOne::class)
            val hasToOne = hasOneToOne || hasManyToOne
            val hasOneToMany = method.hasAnno(OneToMany::class)
            val hasManyToMany = method.hasAnno(ManyToMany::class)
            val hasToMany = hasOneToMany || hasManyToMany

            if (hasToOne || hasToMany) {
                val assocTypes = mutableListOf<String>()
                if (hasOneToOne) assocTypes.add("@OneToOne")
                if (hasManyToOne) assocTypes.add("@ManyToOne")
                if (hasOneToMany) assocTypes.add("@OneToMany")
                if (hasManyToMany) assocTypes.add("@ManyToMany")

                row(I18n.message("editor.immutable.label.association")) {
                    label(assocTypes.joinToString(", "))
                }

                // JoinColumn
                if (method.hasAnno(JoinColumn::class)) {
                    row(I18n.message("editor.immutable.label.joinColumn")) {
                        label("@JoinColumn")
                    }
                }

                // OnDissociate — only for @ManyToOne
                if (hasManyToOne) {
                    addJavaOnDissociateRow(method)
                }
            }
        }
    }

    // ==================== Kotlin Property Info ====================
    // NOTE: All annotation checks use direct PSI access (annotationEntries)
    // instead of PsiService261 to avoid the Kotlin FIR analysis API's
    // ProhibitedAnalysisException ("Analysis is not allowed: Called in the EDT thread").

    private fun buildKotlinPropertyInfo(property: KtProperty): JPanel {
        return panel {
            row(I18n.message("editor.immutable.label.name")) {
                label(property.name ?: "")
            }

            val typeText = property.typeReference?.text ?: ""
            row(I18n.message("editor.immutable.label.propType")) {
                label(typeText)
            }

            // Primary Key: @Id + @GeneratedValue
            if (property.hasKtAnno(Id::class)) {
                row(I18n.message("editor.immutable.label.id")) {
                    label(I18n.message("editor.immutable.label.primaryKey"))
                }
                addKotlinGeneratedValueRow(property)
            }

            // Business Key: @Key (only for table column properties)
            val isTableColumn = !property.hasKtAnno(IdView::class) && !property.hasKtAnno(Formula::class)
                    && !property.hasKtAnno(Transient::class) && !property.hasKtAnno(OneToOne::class)
                    && !property.hasKtAnno(ManyToOne::class) && !property.hasKtAnno(OneToMany::class)
                    && !property.hasKtAnno(ManyToMany::class)
            if (isTableColumn) {
                addKotlinKeyRow(property)
                addKotlinColumnRow(property)
            }

            // View Property: @IdView
            if (property.hasKtAnno(IdView::class)) {
                row(I18n.message("editor.immutable.label.idView")) {
                    label(I18n.message("editor.immutable.label.viewProperty"))
                }
            }

            // Computed: @Formula (simple)
            if (property.hasKtAnno(Formula::class)) {
                row(I18n.message("editor.immutable.label.formula")) {
                    label(I18n.message("editor.immutable.label.simpleComputed"))
                }
            }

            // Computed: @Transient (complex)
            if (property.hasKtAnno(Transient::class)) {
                row(I18n.message("editor.immutable.label.transient")) {
                    label(I18n.message("editor.immutable.label.complexComputed"))
                }
            }

            // Association mappings
            val hasToOne = property.hasKtAnno(OneToOne::class) || property.hasKtAnno(ManyToOne::class)
            val hasToMany = property.hasKtAnno(OneToMany::class) || property.hasKtAnno(ManyToMany::class)

            if (hasToOne || hasToMany) {
                val assocTypes = mutableListOf<String>()
                if (property.hasKtAnno(OneToOne::class)) assocTypes.add("@OneToOne")
                if (property.hasKtAnno(ManyToOne::class)) assocTypes.add("@ManyToOne")
                if (property.hasKtAnno(OneToMany::class)) assocTypes.add("@OneToMany")
                if (property.hasKtAnno(ManyToMany::class)) assocTypes.add("@ManyToMany")

                row(I18n.message("editor.immutable.label.association")) {
                    label(assocTypes.joinToString(", "))
                }

                // JoinColumn
                if (property.hasKtAnno(JoinColumn::class)) {
                    row(I18n.message("editor.immutable.label.joinColumn")) {
                        label("@JoinColumn")
                    }
                }

                // OnDissociate — only for @ManyToOne
                if (property.hasKtAnno(ManyToOne::class)) {
                    addKotlinOnDissociateRow(property)
                }
            }
        }
    }

    // ==================== Java GeneratedValue ====================

    private fun Panel.addJavaGeneratedValueRow(method: PsiMethod) {
        val genValueAnno = method.findAnnotation(GeneratedValue::class)
        val hasGeneratedValue = genValueAnno != null
        val currentStrategy = readJavaGeneratedValueStrategy(genValueAnno)

        row {
            cell(JBCheckBox(I18n.message("editor.immutable.label.generatedValue"), hasGeneratedValue).apply {
                addActionListener {
                    if (isSelected) {
                        addJavaGeneratedValue(method)
                    } else {
                        removeJavaGeneratedValue(method)
                    }
                    refreshPanel()
                }
            })
            if (hasGeneratedValue) {
                val comboBox = ComboBox(GenerationType.entries.map { it.name }.toTypedArray())
                comboBox.selectedItem = currentStrategy
                comboBox.addActionListener {
                    val newStrategy = comboBox.selectedItem as? String ?: return@addActionListener
                    updateJavaGeneratedValueStrategy(method, newStrategy)
                }
                cell(comboBox).align(Align.FILL)
            }
        }
    }

    private fun readJavaGeneratedValueStrategy(annotation: PsiAnnotation?): String {
        if (annotation == null) return GenerationType.AUTO.name
        val value = annotation.findAttributeValue("strategy") ?: annotation.findAttributeValue("value")
        val text = value?.text ?: return GenerationType.AUTO.name
        return GenerationType.entries.find { text.endsWith(it.name) }?.name ?: GenerationType.AUTO.name
    }

    private fun updateJavaGeneratedValueStrategy(method: PsiMethod, newStrategy: String) {
        WriteCommandAction.runWriteCommandAction(project) {
            val document = getDocument(method) ?: return@runWriteCommandAction
            val annotation = method.findAnnotation(GeneratedValue::class) ?: return@runWriteCommandAction
            val strategyValue = annotation.findAttributeValue("strategy")
                ?: annotation.findAttributeValue("value")
            if (strategyValue != null) {
                val oldText = strategyValue.text
                val oldStrategy = GenerationType.entries.find { oldText.endsWith(it.name) }?.name
                if (oldStrategy != null && oldStrategy != newStrategy) {
                    val newText = oldText.replace(oldStrategy, newStrategy)
                    document.replaceString(
                        strategyValue.textRange.startOffset,
                        strategyValue.textRange.endOffset,
                        newText
                    )
                }
            }
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
    }

    private fun addJavaGeneratedValue(method: PsiMethod) {
        addJavaAnnotation(
            method,
            "@${GeneratedValue::class.simpleName}(strategy = ${GenerationType::class.simpleName}.${GenerationType.AUTO.name})"
        )
    }

    private fun removeJavaGeneratedValue(method: PsiMethod) {
        method.findAnnotation(GeneratedValue::class)?.let { removeJavaAnnotation(method, it) }
    }

    // ==================== Kotlin GeneratedValue ====================

    private fun Panel.addKotlinGeneratedValueRow(property: KtProperty) {
        val genValueAnno = property.findAnnotationEntry(GeneratedValue::class)
        val hasGeneratedValue = genValueAnno != null
        val currentStrategy = readKotlinGeneratedValueStrategy(genValueAnno)

        row {
            cell(JBCheckBox(I18n.message("editor.immutable.label.generatedValue"), hasGeneratedValue).apply {
                addActionListener {
                    if (isSelected) {
                        addKotlinGeneratedValue(property)
                    } else {
                        removeKotlinGeneratedValue(property)
                    }
                    refreshPanel()
                }
            })
            if (hasGeneratedValue) {
                val comboBox = ComboBox(GenerationType.entries.map { it.name }.toTypedArray())
                comboBox.selectedItem = currentStrategy
                comboBox.addActionListener {
                    val newStrategy = comboBox.selectedItem as? String ?: return@addActionListener
                    updateKotlinGeneratedValueStrategy(property, newStrategy)
                }
                cell(comboBox).align(Align.FILL)
            }
        }
    }

    private fun readKotlinGeneratedValueStrategy(annotationEntry: KtAnnotationEntry?): String {
        if (annotationEntry == null) return GenerationType.AUTO.name
        val valueArg = findKotlinAnnotationArg(annotationEntry, "strategy")
            ?: findKotlinAnnotationArg(annotationEntry, "value")
            ?: return GenerationType.AUTO.name
        val text = valueArg.text
        return GenerationType.entries.find { text.endsWith(it.name) }?.name ?: GenerationType.AUTO.name
    }

    private fun updateKotlinGeneratedValueStrategy(property: KtProperty, newStrategy: String) {
        WriteCommandAction.runWriteCommandAction(project) {
            val document = getDocument(property) ?: return@runWriteCommandAction
            val annotationEntry = property.findAnnotationEntry(GeneratedValue::class)
                ?: return@runWriteCommandAction
            val valueArg = findKotlinAnnotationArg(annotationEntry, "strategy")
                ?: findKotlinAnnotationArg(annotationEntry, "value")
            if (valueArg != null) {
                val oldText = valueArg.text
                val oldStrategy = GenerationType.entries.find { oldText.endsWith(it.name) }?.name
                if (oldStrategy != null && oldStrategy != newStrategy) {
                    val newText = oldText.replace(oldStrategy, newStrategy)
                    document.replaceString(
                        valueArg.startOffset,
                        valueArg.endOffset,
                        newText
                    )
                }
            }
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
    }

    private fun addKotlinGeneratedValue(property: KtProperty) {
        addKotlinAnnotation(
            property,
            "@${GeneratedValue::class.simpleName}(strategy = ${GenerationType::class.simpleName}.${GenerationType.AUTO.name})"
        )
    }

    private fun removeKotlinGeneratedValue(property: KtProperty) {
        property.findAnnotationEntry(GeneratedValue::class)?.let { removeKotlinAnnotation(property, it) }
    }

    // ==================== Key ====================

    private fun Panel.addJavaKeyRow(method: PsiMethod) {
        val hasKey = method.hasAnno(Key::class)
        row {
            cell(JBCheckBox(I18n.message("editor.immutable.label.businessKey"), hasKey).apply {
                addActionListener {
                    if (isSelected) {
                        addJavaKey(method)
                    } else {
                        removeJavaKey(method)
                    }
                    refreshPanel()
                }
            })
        }
    }

    private fun addJavaKey(method: PsiMethod) {
        addJavaAnnotation(method, "@${Key::class.simpleName!!}")
    }

    private fun removeJavaKey(method: PsiMethod) {
        method.findAnnotation(Key::class)?.let { removeJavaAnnotation(method, it) }
    }

    private fun Panel.addKotlinKeyRow(property: KtProperty) {
        val hasKey = property.hasKtAnno(Key::class)
        row {
            cell(JBCheckBox(I18n.message("editor.immutable.label.businessKey"), hasKey).apply {
                addActionListener {
                    if (isSelected) {
                        addKotlinKey(property)
                    } else {
                        removeKotlinKey(property)
                    }
                    refreshPanel()
                }
            })
        }
    }

    private fun addKotlinKey(property: KtProperty) {
        addKotlinAnnotation(property, "@${Key::class.simpleName!!}")
    }

    private fun removeKotlinKey(property: KtProperty) {
        property.findAnnotationEntry(Key::class)?.let { removeKotlinAnnotation(property, it) }
    }

    // ==================== Column ====================

    private fun Panel.addJavaColumnRow(method: PsiMethod) {
        val columnAnno = method.findAnnotation(Column::class)
        val hasColumn = columnAnno != null
        val currentName = readAnnotationStringValue(columnAnno, "name")

        row {
            cell(JBCheckBox("@Column", hasColumn).apply {
                addActionListener {
                    if (isSelected) {
                        addJavaAnnotation(method, "@${Column::class.simpleName}")
                    } else {
                        columnAnno?.let { removeJavaAnnotation(method, it) }
                    }
                    refreshPanel()
                }
            })
            if (hasColumn) {
                val textField = JTextField(currentName, 20)
                textField.addActionListener {
                    updateAnnotationStringValue(method, Column::class, "name", textField.text)
                }
                textField.addFocusListener(object : java.awt.event.FocusAdapter() {
                    override fun focusLost(e: java.awt.event.FocusEvent?) {
                        updateAnnotationStringValue(method, Column::class, "name", textField.text)
                    }
                })
                cell(textField).align(Align.FILL)
            }
        }
    }

    private fun Panel.addKotlinColumnRow(property: KtProperty) {
        val columnEntry = property.findAnnotationEntry(Column::class)
        val hasColumn = columnEntry != null
        val currentName = readKotlinAnnotationStringValue(columnEntry, "name")

        row {
            cell(JBCheckBox("@Column", hasColumn).apply {
                addActionListener {
                    if (isSelected) {
                        addKotlinAnnotation(property, "@${Column::class.simpleName}")
                    } else {
                        columnEntry?.let { removeKotlinAnnotation(property, it) }
                    }
                    refreshPanel()
                }
            })
            if (hasColumn) {
                val textField = JTextField(currentName, 20)
                textField.addActionListener {
                    updateKotlinAnnotationStringValue(property, Column::class, "name", textField.text)
                }
                textField.addFocusListener(object : java.awt.event.FocusAdapter() {
                    override fun focusLost(e: java.awt.event.FocusEvent?) {
                        updateKotlinAnnotationStringValue(property, Column::class, "name", textField.text)
                    }
                })
                cell(textField).align(Align.FILL)
            }
        }
    }

    // ==================== Table ====================

    private fun Panel.addJavaTableRow(psiClass: PsiClass) {
        val tableAnno = psiClass.findAnnotation(Table::class)
        val hasTable = tableAnno != null
        val currentName = readAnnotationStringValue(tableAnno, "name")

        row {
            cell(JBCheckBox("@Table", hasTable).apply {
                addActionListener {
                    if (isSelected) {
                        val snakeName = runReadOnly { psiClass.name }?.camelToSnake() ?: ""
                        addJavaAnnotation(psiClass, "@${Table::class.simpleName}(name = \"$snakeName\")")
                    } else {
                        tableAnno?.let { removeJavaAnnotation(psiClass, it) }
                    }
                    refreshPanel()
                }
            })
            if (hasTable) {
                val textField = JTextField(currentName, 20)
                textField.addActionListener {
                    updateAnnotationStringValue(psiClass, Table::class, "name", textField.text)
                }
                textField.addFocusListener(object : java.awt.event.FocusAdapter() {
                    override fun focusLost(e: java.awt.event.FocusEvent?) {
                        updateAnnotationStringValue(psiClass, Table::class, "name", textField.text)
                    }
                })
                cell(textField).align(Align.FILL)
            }
        }
    }

    private fun Panel.addKotlinTableRow(ktClass: KtClass) {
        val tableEntry = ktClass.findAnnotationEntry(Table::class)
        val hasTable = tableEntry != null
        val currentName = readKotlinAnnotationStringValue(tableEntry, "name")

        row {
            cell(JBCheckBox("@Table", hasTable).apply {
                addActionListener {
                    if (isSelected) {
                        val snakeName = runReadOnly { ktClass.name }?.camelToSnake() ?: ""
                        addKotlinAnnotation(ktClass, "@${Table::class.simpleName}(name = \"$snakeName\")")
                    } else {
                        tableEntry?.let { removeKotlinAnnotation(ktClass, it) }
                    }
                    refreshPanel()
                }
            })
            if (hasTable) {
                val textField = JTextField(currentName, 20)
                textField.addActionListener {
                    updateKotlinAnnotationStringValue(ktClass, Table::class, "name", textField.text)
                }
                textField.addFocusListener(object : java.awt.event.FocusAdapter() {
                    override fun focusLost(e: java.awt.event.FocusEvent?) {
                        updateKotlinAnnotationStringValue(ktClass, Table::class, "name", textField.text)
                    }
                })
                cell(textField).align(Align.FILL)
            }
        }
    }

    // ==================== Java OnDissociate ====================
    // Only shown for @ManyToOne properties. Uses a checkbox to toggle the annotation
    // and a ComboBox to change the DissociateAction value.

    private fun Panel.addJavaOnDissociateRow(method: PsiMethod) {
        val onDissociateAnno = method.findAnnotation(OnDissociate::class)
        val hasOnDissociate = onDissociateAnno != null
        val currentAction = readJavaDissociateAction(onDissociateAnno)

        row {
            cell(JBCheckBox(I18n.message("editor.immutable.label.onDissociate"), hasOnDissociate).apply {
                addActionListener {
                    if (isSelected) {
                        addJavaOnDissociate(method)
                    } else {
                        removeJavaOnDissociate(method)
                    }
                    refreshPanel()
                }
            })
            if (hasOnDissociate) {
                val comboBox = ComboBox(DissociateAction.entries.map { it.name }.toTypedArray())
                comboBox.selectedItem = currentAction
                comboBox.addActionListener {
                    val newAction = comboBox.selectedItem as? String ?: return@addActionListener
                    updateJavaDissociateAction(method, newAction)
                }
                cell(comboBox).align(Align.FILL)
            }
        }
    }

    private fun readJavaDissociateAction(annotation: PsiAnnotation?): String {
        if (annotation == null) return DissociateAction.NONE.name
        val value = annotation.findAttributeValue("value")
            ?: annotation.findAttributeValue("action")
        val text = value?.text ?: return DissociateAction.NONE.name
        return DissociateAction.entries.find { text.endsWith(it.name) }?.name ?: DissociateAction.NONE.name
    }

    private fun updateJavaDissociateAction(method: PsiMethod, newAction: String) {
        WriteCommandAction.runWriteCommandAction(project) {
            val document = getDocument(method) ?: return@runWriteCommandAction
            val annotation = method.findAnnotation(OnDissociate::class) ?: return@runWriteCommandAction
            val actionValue = annotation.findAttributeValue("value")
                ?: annotation.findAttributeValue("action")
            if (actionValue != null) {
                val oldText = actionValue.text
                val oldAction = DissociateAction.entries.find { oldText.endsWith(it.name) }?.name
                if (oldAction != null && oldAction != newAction) {
                    val newText = oldText.replace(oldAction, newAction)
                    document.replaceString(
                        actionValue.textRange.startOffset,
                        actionValue.textRange.endOffset,
                        newText
                    )
                }
            }
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
    }

    private fun addJavaOnDissociate(method: PsiMethod) {
        addJavaAnnotation(
            method,
            "@${OnDissociate::class.simpleName}(${DissociateAction::class.simpleName}.${DissociateAction.NONE.name})"
        )
    }

    private fun removeJavaOnDissociate(method: PsiMethod) {
        method.findAnnotation(OnDissociate::class)?.let { removeJavaAnnotation(method, it) }
    }

    // ==================== Kotlin OnDissociate ====================
    // Only shown for @ManyToOne properties. Uses a checkbox to toggle the annotation
    // and a ComboBox to change the DissociateAction value.

    private fun Panel.addKotlinOnDissociateRow(property: KtProperty) {
        val onDissociateAnno = property.findAnnotationEntry(OnDissociate::class)
        val hasOnDissociate = onDissociateAnno != null
        val currentAction = readKotlinDissociateAction(onDissociateAnno)

        row {
            cell(JBCheckBox(I18n.message("editor.immutable.label.onDissociate"), hasOnDissociate).apply {
                addActionListener {
                    if (isSelected) {
                        addKotlinOnDissociate(property)
                    } else {
                        removeKotlinOnDissociate(property)
                    }
                    refreshPanel()
                }
            })
            if (hasOnDissociate) {
                val comboBox = ComboBox(DissociateAction.entries.map { it.name }.toTypedArray())
                comboBox.selectedItem = currentAction
                comboBox.addActionListener {
                    val newAction = comboBox.selectedItem as? String ?: return@addActionListener
                    updateKotlinDissociateAction(property, newAction)
                }
                cell(comboBox).align(Align.FILL)
            }
        }
    }

    private fun readKotlinDissociateAction(annotationEntry: KtAnnotationEntry?): String {
        if (annotationEntry == null) return DissociateAction.NONE.name
        val valueArg = findKotlinAnnotationArg(annotationEntry, "value")
            ?: findKotlinAnnotationArg(annotationEntry, "action")
        val text = valueArg?.text ?: return DissociateAction.NONE.name
        return DissociateAction.entries.find { text.endsWith(it.name) }?.name ?: DissociateAction.NONE.name
    }

    private fun updateKotlinDissociateAction(property: KtProperty, newAction: String) {
        WriteCommandAction.runWriteCommandAction(project) {
            val document = getDocument(property) ?: return@runWriteCommandAction
            val annotationEntry = property.findAnnotationEntry(OnDissociate::class)
                ?: return@runWriteCommandAction
            val valueArg = findKotlinAnnotationArg(annotationEntry, "value")
                ?: findKotlinAnnotationArg(annotationEntry, "action")
            if (valueArg != null) {
                val oldText = valueArg.text
                val oldAction = DissociateAction.entries.find { oldText.endsWith(it.name) }?.name
                if (oldAction != null && oldAction != newAction) {
                    val newText = oldText.replace(oldAction, newAction)
                    document.replaceString(
                        valueArg.startOffset,
                        valueArg.endOffset,
                        newText
                    )
                }
            }
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
    }

    private fun addKotlinOnDissociate(property: KtProperty) {
        addKotlinAnnotation(
            property,
            "@${OnDissociate::class.simpleName}(${DissociateAction::class.simpleName}.${DissociateAction.NONE.name})"
        )
    }

    private fun removeKotlinOnDissociate(property: KtProperty) {
        property.findAnnotationEntry(OnDissociate::class)?.let { removeKotlinAnnotation(property, it) }
    }

    private fun refreshPanel() {
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
            currentElement?.let { show(it) }
        }
    }

    // ==================== Direct PSI Annotation Helpers ====================
    // These use direct PSI tree access (annotationEntries / modifierList.annotations)
    // instead of going through PsiService261 (Kotlin FIR) or PsiAnnotation.qualifiedName
    // (Java index), both of which are prohibited on the EDT.

    // --- Kotlin: avoid FIR analysis API ---

    private fun KtProperty.hasKtAnno(annotationClass: KClass<*>): Boolean {
        val fqName = annotationClass.qualifiedName ?: return false
        val simpleName = annotationClass.simpleName ?: return false
        return annotationEntries.any { entry ->
            val typeText = entry.typeReference?.text
            typeText == simpleName || typeText == fqName
        }
    }

    private fun KtClass.hasKtAnno(annotationClass: KClass<*>): Boolean {
        val fqName = annotationClass.qualifiedName ?: return false
        val simpleName = annotationClass.simpleName ?: return false
        return annotationEntries.any { entry ->
            val typeText = entry.typeReference?.text
            typeText == simpleName || typeText == fqName
        }
    }

    // --- Java: avoid index access via PsiAnnotation.qualifiedName ---

    /** Check if a [PsiMethod] has the given annotation using only PSI tree access. */
    private fun PsiMethod.hasAnno(annotationClass: KClass<*>): Boolean {
        val simpleName = annotationClass.simpleName ?: return false
        return modifierList.annotations.any {
            it.nameReferenceElement?.referenceName == simpleName
        }
    }

    /** Check if a [PsiClass] has the given annotation using only PSI tree access. */
    private fun PsiClass.hasAnno(annotationClass: KClass<*>): Boolean {
        val simpleName = annotationClass.simpleName ?: return false
        return modifierList?.annotations?.any {
            it.nameReferenceElement?.referenceName == simpleName
        } == true
    }

    // ==================== Shared Annotation Add/Remove ====================
    // Uses PSI tree (doc comment / annotations) to determine the insertion point
    // instead of scanning document text.

    /** Insert offset right after the doc comment or last annotation of a Java method. */
    private fun PsiMethod.annotationInsertOffset(): Int {
        val docComment = this.docComment
        if (docComment != null) return docComment.textRange.endOffset
        val annotations = this.modifierList.annotations
        if (annotations.isNotEmpty()) return annotations.last().textRange.endOffset
        return this.modifierList.textRange.startOffset
    }

    /** Insert offset right after the doc comment or last annotation of a Kotlin property. */
    private fun KtProperty.annotationInsertOffset(): Int {
        val docComment = PsiTreeUtil.getChildOfType(this, KDoc::class.java)
        if (docComment != null) return docComment.textRange.endOffset
        val entries = this.annotationEntries
        if (entries.isNotEmpty()) return entries.last().textRange.endOffset
        return this.textRange.startOffset
    }

    /** Reference offset for indentation calculation (doc comment, last annotation, or element start). */
    private fun PsiMethod.indentationRefOffset(): Int =
        this.docComment?.textRange?.startOffset
            ?: this.modifierList.annotations.lastOrNull()?.textRange?.startOffset
            ?: this.textRange.startOffset

    /** Reference offset for indentation calculation (doc comment, last annotation, or element start). */
    private fun KtProperty.indentationRefOffset(): Int =
        PsiTreeUtil.getChildOfType(this, KDoc::class.java)?.textRange?.startOffset
            ?: this.annotationEntries.lastOrNull()?.textRange?.startOffset
            ?: this.textRange.startOffset

    /** Extract the indentation (whitespace) before a given offset in the document. */
    private fun indentBefore(document: Document, offset: Int): String {
        val text = document.text
        var i = offset
        while (i > 0 && text[i - 1] != '\n') i--
        return text.substring(i, offset)
    }

    // --- PsiClass / KtClass insert helpers ---

    private fun PsiClass.annotationInsertOffset(): Int {
        val docComment = this.docComment
        if (docComment != null) return docComment.textRange.endOffset
        val annotations = this.modifierList?.annotations
        if (!annotations.isNullOrEmpty()) return annotations.last().textRange.endOffset
        return this.modifierList?.textRange?.startOffset ?: this.textRange.startOffset
    }

    private fun PsiClass.indentationRefOffset(): Int =
        this.docComment?.textRange?.startOffset
            ?: this.modifierList?.annotations?.lastOrNull()?.textRange?.startOffset
            ?: this.textRange.startOffset

    private fun KtClass.annotationInsertOffset(): Int {
        val docComment = PsiTreeUtil.getChildOfType(this, KDoc::class.java)
        if (docComment != null) return docComment.textRange.endOffset
        val entries = this.annotationEntries
        if (entries.isNotEmpty()) return entries.last().textRange.endOffset
        return this.textRange.startOffset
    }

    private fun KtClass.indentationRefOffset(): Int =
        PsiTreeUtil.getChildOfType(this, KDoc::class.java)?.textRange?.startOffset
            ?: this.annotationEntries.lastOrNull()?.textRange?.startOffset
            ?: this.textRange.startOffset

    // --- String annotation value helpers ---

    private fun readAnnotationStringValue(annotation: PsiAnnotation?, attrName: String): String {
        if (annotation == null) return ""
        val value = annotation.findAttributeValue(attrName) ?: return ""
        val text = value.text
        return if (text.startsWith("\"") && text.endsWith("\"")) text.substring(1, text.length - 1) else text
    }

    private fun readKotlinAnnotationStringValue(entry: KtAnnotationEntry?, attrName: String): String {
        if (entry == null) return ""
        val valueArg = findKotlinAnnotationArg(entry, attrName) ?: return ""
        val text = valueArg.text
        return if (text.startsWith("\"") && text.endsWith("\"")) text.substring(1, text.length - 1) else text
    }

    private fun updateAnnotationStringValue(
        element: PsiElement, annotationClass: KClass<*>, attrName: String, newValue: String
    ) {
        WriteCommandAction.runWriteCommandAction(project) {
            val annotation = when (element) {
                is PsiMethod -> element.findAnnotation(annotationClass)
                is PsiClass -> element.findAnnotation(annotationClass)
                else -> null
            } ?: return@runWriteCommandAction
            val factory = JavaPsiFacade.getElementFactory(project)
            val valueExpr = factory.createExpressionFromText("\"$newValue\"", annotation)
            annotation.setDeclaredAttributeValue(attrName, valueExpr)
        }
    }

    private fun updateKotlinAnnotationStringValue(
        element: PsiElement, annotationClass: KClass<*>, attrName: String, newValue: String
    ) {
        WriteCommandAction.runWriteCommandAction(project) {
            val document = getDocument(element) ?: return@runWriteCommandAction
            val entry = when (element) {
                is KtProperty -> element.findAnnotationEntry(annotationClass)
                is KtClass -> element.findAnnotationEntry(annotationClass)
                else -> null
            } ?: return@runWriteCommandAction
            val valueArg = findKotlinAnnotationArg(entry, attrName)
            val newText = "\"$newValue\""
            if (valueArg != null) {
                // Parameter exists — replace its value
                if (valueArg.text != newText) {
                    document.replaceString(valueArg.startOffset, valueArg.endOffset, newText)
                }
            } else {
                // Parameter doesn't exist — add it
                val args = entry.valueArguments
                val insertOffset: Int
                val insertText: String
                if (args.isEmpty()) {
                    // No params: @Column → @Column(name = "x")
                    insertOffset = entry.textRange.endOffset
                    insertText = "($attrName = $newText)"
                } else {
                    // Has other params: @Column(sqlType = "int") → @Column(sqlType = "int", name = "x")
                    insertOffset = entry.textRange.endOffset - 1 // before ')'
                    insertText = ", $attrName = $newText"
                }
                document.insertString(insertOffset, insertText)
            }
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
    }

    // --- camelCase to snake_case ---

    private fun String.camelToSnake(): String =
        replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()

    // --- add/remove for PsiMethod ---

    /** Add an annotation line after doc comment / last annotation for a Java method. */
    private fun addJavaAnnotation(method: PsiMethod, annotationText: String) {
        WriteCommandAction.runWriteCommandAction(project) {
            val document = getDocument(method) ?: return@runWriteCommandAction
            val insertOffset = method.annotationInsertOffset()
            val refOffset = method.indentationRefOffset()
            val indentation = indentBefore(document, refOffset)
            document.insertString(insertOffset, "\n$indentation$annotationText")
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
    }

    /** Remove a [PsiAnnotation] from a Java method (annotation + its line). */
    private fun removeJavaAnnotation(method: PsiMethod, annotation: PsiAnnotation) {
        WriteCommandAction.runWriteCommandAction(project) {
            val document = getDocument(method) ?: return@runWriteCommandAction
            removeAnnotationLine(document, annotation)
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
    }

    // --- add/remove for PsiClass ---

    private fun addJavaAnnotation(psiClass: PsiClass, annotationText: String) {
        WriteCommandAction.runWriteCommandAction(project) {
            val document = getDocument(psiClass) ?: return@runWriteCommandAction
            val insertOffset = psiClass.annotationInsertOffset()
            val refOffset = psiClass.indentationRefOffset()
            val indentation = indentBefore(document, refOffset)
            document.insertString(insertOffset, "\n$indentation$annotationText")
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
    }

    private fun removeJavaAnnotation(psiClass: PsiClass, annotation: PsiAnnotation) {
        WriteCommandAction.runWriteCommandAction(project) {
            val document = getDocument(psiClass) ?: return@runWriteCommandAction
            removeAnnotationLine(document, annotation)
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
    }

    // --- add/remove for KtProperty ---

    /** Add an annotation line after doc comment / last annotation for a Kotlin property. */
    private fun addKotlinAnnotation(property: KtProperty, annotationText: String) {
        WriteCommandAction.runWriteCommandAction(project) {
            val document = getDocument(property) ?: return@runWriteCommandAction
            val insertOffset = property.annotationInsertOffset()
            val refOffset = property.indentationRefOffset()
            val indentation = indentBefore(document, refOffset)
            document.insertString(insertOffset, "\n$indentation$annotationText")
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
    }

    /** Remove a [KtAnnotationEntry] from a Kotlin property (annotation + its line). */
    private fun removeKotlinAnnotation(property: KtProperty, annotationEntry: KtAnnotationEntry) {
        WriteCommandAction.runWriteCommandAction(project) {
            val document = getDocument(property) ?: return@runWriteCommandAction
            removeAnnotationLine(document, annotationEntry)
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
    }

    // --- add/remove for KtClass ---

    private fun addKotlinAnnotation(ktClass: KtClass, annotationText: String) {
        WriteCommandAction.runWriteCommandAction(project) {
            val document = getDocument(ktClass) ?: return@runWriteCommandAction
            val insertOffset = ktClass.annotationInsertOffset()
            val refOffset = ktClass.indentationRefOffset()
            val indentation = indentBefore(document, refOffset)
            document.insertString(insertOffset, "\n$indentation$annotationText")
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
    }

    private fun removeKotlinAnnotation(ktClass: KtClass, annotationEntry: KtAnnotationEntry) {
        WriteCommandAction.runWriteCommandAction(project) {
            val document = getDocument(ktClass) ?: return@runWriteCommandAction
            removeAnnotationLine(document, annotationEntry)
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
    }

    /** Delete an annotation element and its containing line from the document. */
    private fun removeAnnotationLine(document: Document, element: PsiElement) {
        val text = document.text
        var deleteStart = element.textRange.startOffset
        while (deleteStart > 0 && text[deleteStart - 1] != '\n') deleteStart--
        var deleteEnd = element.textRange.endOffset
        if (deleteEnd < text.length && text[deleteEnd] == '\n') deleteEnd++
        else if (deleteEnd + 1 < text.length && text[deleteEnd] == '\r' && text[deleteEnd + 1] == '\n') deleteEnd += 2
        document.deleteString(deleteStart, deleteEnd)
    }

    // ==================== Utility Methods ====================

    private fun PsiMethod.findAnnotation(annotationClass: KClass<*>): PsiAnnotation? {
        val simpleName = annotationClass.simpleName ?: return null
        return runReadOnly { modifierList.annotations.find { it.nameReferenceElement?.referenceName == simpleName } }
    }

    private fun PsiClass.findAnnotation(annotationClass: KClass<*>): PsiAnnotation? {
        val simpleName = annotationClass.simpleName ?: return null
        return runReadOnly { modifierList?.annotations?.find { it.nameReferenceElement?.referenceName == simpleName } }
    }

    private fun KtClass.findAnnotationEntry(annotationClass: KClass<*>): KtAnnotationEntry? {
        val fqName = annotationClass.qualifiedName ?: return null
        val simpleName = annotationClass.simpleName ?: return null
        return runReadOnly {
            annotationEntries.find { entry ->
                val typeText = entry.typeReference?.text
                typeText == simpleName || typeText == fqName
            }
        }
    }

    private fun KtProperty.findAnnotationEntry(annotationClass: KClass<*>): KtAnnotationEntry? {
        val fqName = annotationClass.qualifiedName ?: return null
        val simpleName = annotationClass.simpleName ?: return null
        return runReadOnly {
            annotationEntries.find { entry ->
                val typeText = entry.typeReference?.text
                typeText == simpleName || typeText == fqName
            }
        }
    }

    private fun findKotlinAnnotationArg(entry: KtAnnotationEntry, name: String): KtExpression? {
        return entry.valueArguments.find { it.getArgumentName()?.asName?.asString() == name }
            ?.getArgumentExpression()
            ?: entry.valueArguments.firstOrNull()?.getArgumentExpression()
    }

    private fun getDocument(element: PsiElement): Document? {
        val vf = element.containingFile?.virtualFile ?: return null
        return FileDocumentManager.getInstance().getDocument(vf)
    }
}
