package com.github.kylianvermeulen.tuistrunner.runconfig

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.components.service
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.github.kylianvermeulen.tuistrunner.detection.TuistProjectService

class TuistTestRunConfigurationProducer : LazyRunConfigurationProducer<TuistTestRunConfiguration>() {

    override fun getConfigurationFactory(): ConfigurationFactory =
        TuistTestConfigurationType().configurationFactories[0]

    override fun setupConfigurationFromContext(
        configuration: TuistTestRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>,
    ): Boolean {
        val element = context.psiLocation ?: return false
        val file = element.containingFile ?: return false
        if (!file.name.endsWith(".swift")) return false

        val project = context.project
        val service = project.service<TuistProjectService>()
        if (!service.isTuistProject) return false

        val fileText = file.text ?: return false
        val testContext = SwiftTestContextParser.detectTestContext(fileText, element.textOffset) ?: return false

        // Resolve scheme
        val schemes = service.availableSchemes
        if (schemes.isEmpty()) return false

        val scheme = schemes.firstOrNull { scheme ->
            scheme.testTargets.any { target ->
                target.contains(testContext.className, ignoreCase = true)
            }
        } ?: schemes[0]

        // Resolve target name: try to find a test target matching the class name
        val testTarget = scheme.testTargets.firstOrNull { target ->
            target.contains(testContext.className, ignoreCase = true)
        } ?: scheme.testTargets.firstOrNull()

        configuration.schemeName = scheme.name
        configuration.testTarget = testTarget
        configuration.testClass = testContext.className
        configuration.testMethod = testContext.methodName
        configuration.name = configuration.suggestedName()

        return true
    }

    override fun isConfigurationFromContext(
        configuration: TuistTestRunConfiguration,
        context: ConfigurationContext,
    ): Boolean {
        val element = context.psiLocation ?: return false
        val file = element.containingFile ?: return false
        if (!file.name.endsWith(".swift")) return false

        val fileText = file.text ?: return false
        val testContext = SwiftTestContextParser.detectTestContext(fileText, element.textOffset) ?: return false

        return configuration.testClass == testContext.className &&
            configuration.testMethod == testContext.methodName
    }
}
