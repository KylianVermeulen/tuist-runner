package com.github.kylianvermeulen.tuistrunner.runconfig

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.github.kylianvermeulen.tuistrunner.TuistBundle
import com.github.kylianvermeulen.tuistrunner.detection.TuistProjectService
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.JTextField

class TuistTestConfigurationEditor(private val project: Project) :
    SettingsEditor<TuistTestRunConfiguration>() {

    private val schemeComboBox = ComboBox<String>().apply {
        isEditable = true
    }
    private val additionalArgsField = JTextField()

    init {
        loadSchemes()
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

    override fun resetEditorFrom(configuration: TuistTestRunConfiguration) {
        schemeComboBox.editor.item = configuration.schemeName
        additionalArgsField.text = configuration.additionalArguments
    }

    override fun applyEditorTo(configuration: TuistTestRunConfiguration) {
        configuration.schemeName = (schemeComboBox.editor.item as? String)?.trim() ?: ""
        configuration.additionalArguments = additionalArgsField.text.trim()
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
        row(TuistBundle.message("runconfig.editor.additionalArgs.label")) {
            cell(additionalArgsField)
                .align(AlignX.FILL)
                .resizableColumn()
        }
    }
}
