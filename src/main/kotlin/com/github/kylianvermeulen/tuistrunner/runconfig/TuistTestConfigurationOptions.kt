package com.github.kylianvermeulen.tuistrunner.runconfig

import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.openapi.components.StoredProperty

class TuistTestConfigurationOptions : RunConfigurationOptions() {

    private val _schemeName: StoredProperty<String?> =
        string("").provideDelegate(this, "schemeName")

    private val _additionalArguments: StoredProperty<String?> =
        string("").provideDelegate(this, "additionalArguments")

    var schemeName: String
        get() = _schemeName.getValue(this) ?: ""
        set(value) { _schemeName.setValue(this, value) }

    var additionalArguments: String
        get() = _additionalArguments.getValue(this) ?: ""
        set(value) { _additionalArguments.setValue(this, value) }
}
