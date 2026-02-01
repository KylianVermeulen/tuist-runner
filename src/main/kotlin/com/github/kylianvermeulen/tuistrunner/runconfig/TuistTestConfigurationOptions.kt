package com.github.kylianvermeulen.tuistrunner.runconfig

import com.intellij.execution.configurations.LocatableRunConfigurationOptions
import com.intellij.openapi.components.StoredProperty

class TuistTestConfigurationOptions : LocatableRunConfigurationOptions() {

    private val _schemeName: StoredProperty<String?> =
        string("").provideDelegate(this, "schemeName")

    private val _additionalArguments: StoredProperty<String?> =
        string("").provideDelegate(this, "additionalArguments")

    private val _testTarget: StoredProperty<String?> =
        string(null as String?).provideDelegate(this, "testTarget")

    private val _testClass: StoredProperty<String?> =
        string(null as String?).provideDelegate(this, "testClass")

    private val _testMethod: StoredProperty<String?> =
        string(null as String?).provideDelegate(this, "testMethod")

    var schemeName: String
        get() = _schemeName.getValue(this) ?: ""
        set(value) { _schemeName.setValue(this, value) }

    var additionalArguments: String
        get() = _additionalArguments.getValue(this) ?: ""
        set(value) { _additionalArguments.setValue(this, value) }

    var testTarget: String?
        get() = _testTarget.getValue(this)
        set(value) { _testTarget.setValue(this, value) }

    var testClass: String?
        get() = _testClass.getValue(this)
        set(value) { _testClass.setValue(this, value) }

    var testMethod: String?
        get() = _testMethod.getValue(this)
        set(value) { _testMethod.setValue(this, value) }
}
