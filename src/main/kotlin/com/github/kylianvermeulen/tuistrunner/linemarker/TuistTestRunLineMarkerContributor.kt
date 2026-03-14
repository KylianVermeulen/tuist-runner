package com.github.kylianvermeulen.tuistrunner.linemarker

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.github.kylianvermeulen.tuistrunner.runconfig.SwiftTestContextParser

class TuistTestRunLineMarkerContributor : RunLineMarkerContributor() {

    override fun getInfo(element: PsiElement): Info? = null

    override fun getSlowInfo(element: PsiElement): Info? {
        val file = element.containingFile ?: return null
        if (!file.name.endsWith(".swift")) return null

        val fileText = file.text ?: return null
        val elements = SwiftTestContextParser.findAllTestElements(fileText)
        if (elements.isEmpty()) return null

        val elementOffset = element.textOffset

        for (testElement in elements) {
            when (testElement) {
                is SwiftTestContextParser.TestElement.TestClass -> {
                    if (elementOffset == testElement.offset) {
                        return Info(
                            AllIcons.RunConfigurations.TestState.Run_run,
                            ExecutorAction.getActions(0),
                        ) { "Run '${testElement.className}'" }
                    }
                }
                is SwiftTestContextParser.TestElement.TestMethod -> {
                    if (elementOffset == testElement.offset) {
                        return Info(
                            AllIcons.RunConfigurations.TestState.Run,
                            ExecutorAction.getActions(0),
                        ) { "Run '${testElement.className}.${testElement.methodName}'" }
                    }
                }
            }
        }

        return null
    }
}
