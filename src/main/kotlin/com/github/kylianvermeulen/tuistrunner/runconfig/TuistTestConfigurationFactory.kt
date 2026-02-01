package com.github.kylianvermeulen.tuistrunner.runconfig

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project

class TuistTestConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {

    override fun getId(): String = TuistTestConfigurationType.ID

    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        TuistTestRunConfiguration(project, this, "Tuist Test")

    override fun getOptionsClass(): Class<TuistTestConfigurationOptions> =
        TuistTestConfigurationOptions::class.java
}
