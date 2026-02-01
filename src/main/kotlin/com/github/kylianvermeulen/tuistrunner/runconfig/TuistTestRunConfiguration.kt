package com.github.kylianvermeulen.tuistrunner.runconfig

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.github.kylianvermeulen.tuistrunner.TuistBundle
import com.github.kylianvermeulen.tuistrunner.execution.TuistTestCommandLineState

class TuistTestRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String,
) : LocatableConfigurationBase<TuistTestConfigurationOptions>(project, factory, name) {

    override fun getOptions(): TuistTestConfigurationOptions =
        super.getOptions() as TuistTestConfigurationOptions

    var schemeName: String
        get() = options.schemeName
        set(value) { options.schemeName = value }

    var additionalArguments: String
        get() = options.additionalArguments
        set(value) { options.additionalArguments = value }

    var testTarget: String?
        get() = options.testTarget
        set(value) { options.testTarget = value }

    var testClass: String?
        get() = options.testClass
        set(value) { options.testClass = value }

    var testMethod: String?
        get() = options.testMethod
        set(value) { options.testMethod = value }

    fun buildOnlyTestingArgument(): String? {
        val parts = listOfNotNull(testTarget, testClass, testMethod)
        return if (parts.isEmpty()) null else parts.joinToString("/")
    }

    override fun suggestedName(): String {
        val cls = testClass
        val method = testMethod
        return when {
            cls != null && method != null -> "$cls.$method"
            cls != null -> cls
            else -> schemeName.ifBlank { "Tuist Test" }
        }
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        TuistTestConfigurationEditor(project)

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
        TuistTestCommandLineState(environment, this)

    override fun checkConfiguration() {
        if (schemeName.isBlank()) {
            throw RuntimeConfigurationError(TuistBundle.message("runconfig.validation.scheme.empty"))
        }
    }
}
