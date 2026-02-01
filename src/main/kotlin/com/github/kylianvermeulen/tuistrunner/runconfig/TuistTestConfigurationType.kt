package com.github.kylianvermeulen.tuistrunner.runconfig

import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.icons.AllIcons
import com.github.kylianvermeulen.tuistrunner.TuistBundle

class TuistTestConfigurationType : ConfigurationTypeBase(
    ID,
    TuistBundle.message("runconfig.type.name"),
    TuistBundle.message("runconfig.type.description"),
    AllIcons.RunConfigurations.TestState.Run,
) {
    init {
        addFactory(TuistTestConfigurationFactory(this))
    }

    companion object {
        const val ID = "TuistTestRunConfiguration"
    }
}
