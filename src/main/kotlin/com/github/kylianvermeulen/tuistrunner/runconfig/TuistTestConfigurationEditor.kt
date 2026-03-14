package com.github.kylianvermeulen.tuistrunner.runconfig

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.github.kylianvermeulen.tuistrunner.TuistBundle
import com.github.kylianvermeulen.tuistrunner.detection.Simulator
import com.github.kylianvermeulen.tuistrunner.detection.TuistProjectService
import java.awt.Component
import javax.swing.DefaultComboBoxModel
import javax.swing.DefaultListCellRenderer
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JTextField

class TuistTestConfigurationEditor(private val project: Project) :
    SettingsEditor<TuistTestRunConfiguration>() {

    private val schemeComboBox = ComboBox<String>().apply {
        isEditable = true
    }
    private val destinationComboBox = ComboBox<Simulator?>().apply {
        renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean,
            ): Component {
                val display = (value as? Simulator)?.displayName ?: "(Default)"
                return super.getListCellRendererComponent(list, display, index, isSelected, cellHasFocus)
            }
        }
    }
    private val additionalArgsField = JTextField()
    private val testTargetField = JTextField().apply { isEditable = false }
    private val testClassField = JTextField().apply { isEditable = false }
    private val testMethodField = JTextField().apply { isEditable = false }

    init {
        loadSchemes()
        loadSimulators()
    }

    private fun loadSchemes() {
        ApplicationManager.getApplication().executeOnPooledThread {
            val service = project.service<TuistProjectService>()
            val schemes = service.availableSchemes.map { it.name }
            ApplicationManager.getApplication().invokeLater {
                val currentSelection = schemeComboBox.editor.item as? String ?: ""
                val model = DefaultComboBoxModel(schemes.toTypedArray())
                schemeComboBox.model = model
                if (currentSelection.isNotEmpty()) {
                    schemeComboBox.editor.item = currentSelection
                }
            }
        }
    }

    private fun loadSimulators() {
        ApplicationManager.getApplication().executeOnPooledThread {
            val service = project.service<TuistProjectService>()
            val simulators = service.availableSimulators
            ApplicationManager.getApplication().invokeLater {
                val currentSelection = destinationComboBox.selectedItem as? Simulator
                val model = DefaultComboBoxModel<Simulator?>()
                model.addElement(null)
                simulators.forEach { model.addElement(it) }
                destinationComboBox.model = model
                if (currentSelection != null) {
                    val restored = simulators.find { it.udid == currentSelection.udid }
                    destinationComboBox.selectedItem = restored
                }
            }
        }
    }

    override fun resetEditorFrom(configuration: TuistTestRunConfiguration) {
        schemeComboBox.editor.item = configuration.schemeName
        additionalArgsField.text = configuration.additionalArguments
        testTargetField.text = configuration.testTarget ?: ""
        testClassField.text = configuration.testClass ?: ""
        testMethodField.text = configuration.testMethod ?: ""

        val udid = configuration.destinationUdid
        if (udid != null) {
            val model = destinationComboBox.model
            for (i in 0 until model.size) {
                val sim = model.getElementAt(i)
                if (sim?.udid == udid) {
                    destinationComboBox.selectedItem = sim
                    return
                }
            }
        }
        destinationComboBox.selectedItem = null
    }

    override fun applyEditorTo(configuration: TuistTestRunConfiguration) {
        configuration.schemeName = (schemeComboBox.editor.item as? String)?.trim() ?: ""
        configuration.additionalArguments = additionalArgsField.text.trim()
        configuration.testTarget = testTargetField.text.ifBlank { null }
        configuration.testClass = testClassField.text.ifBlank { null }
        configuration.testMethod = testMethodField.text.ifBlank { null }
        configuration.destinationUdid = (destinationComboBox.selectedItem as? Simulator)?.udid
    }

    override fun createEditor(): JComponent = panel {
        row(TuistBundle.message("runconfig.editor.scheme.label")) {
            cell(schemeComboBox)
                .align(AlignX.FILL)
                .resizableColumn()
            button(TuistBundle.message("runconfig.editor.scheme.refresh")) {
                ApplicationManager.getApplication().executeOnPooledThread {
                    val service = project.service<TuistProjectService>()
                    service.refreshSchemes()
                    loadSchemes()
                }
            }
        }
        row(TuistBundle.message("runconfig.editor.destination.label")) {
            cell(destinationComboBox)
                .align(AlignX.FILL)
                .resizableColumn()
            button(TuistBundle.message("runconfig.editor.destination.refresh")) {
                ApplicationManager.getApplication().executeOnPooledThread {
                    val service = project.service<TuistProjectService>()
                    service.refreshSimulators()
                    loadSimulators()
                }
            }
        }
        row(TuistBundle.message("runconfig.editor.additionalArgs.label")) {
            cell(additionalArgsField)
                .align(AlignX.FILL)
                .resizableColumn()
        }
        row(TuistBundle.message("runconfig.editor.testTarget.label")) {
            cell(testTargetField)
                .align(AlignX.FILL)
                .resizableColumn()
        }
        row(TuistBundle.message("runconfig.editor.testClass.label")) {
            cell(testClassField)
                .align(AlignX.FILL)
                .resizableColumn()
        }
        row(TuistBundle.message("runconfig.editor.testMethod.label")) {
            cell(testMethodField)
                .align(AlignX.FILL)
                .resizableColumn()
        }
    }
}
