package com.github.kylianvermeulen.tuistrunner.testlocator

import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope

class TuistTestLocator : SMTestLocator {

    companion object {
        val INSTANCE = TuistTestLocator()
        const val PROTOCOL_SUITE = "tuist-suite"
        const val PROTOCOL_TEST = "tuist-test"
    }

    override fun getLocation(
        protocol: String,
        path: String,
        project: Project,
        scope: GlobalSearchScope,
    ): List<Location<*>> {
        return when (protocol) {
            PROTOCOL_SUITE -> locateSuite(path, project, scope)
            PROTOCOL_TEST -> locateTest(path, project, scope)
            else -> emptyList()
        }
    }

    private fun locateSuite(className: String, project: Project, scope: GlobalSearchScope): List<Location<*>> {
        val fileName = "$className.swift"
        val files = FilenameIndex.getVirtualFilesByName(fileName, scope)
        return files.mapNotNull { vFile ->
            val psiFile = PsiManager.getInstance(project).findFile(vFile)
            psiFile?.let { PsiLocation.fromPsiElement(it) }
        }
    }

    private fun locateTest(path: String, project: Project, scope: GlobalSearchScope): List<Location<*>> {
        val parts = path.split("/")
        val className: String
        val methodName: String
        when (parts.size) {
            3 -> { className = parts[1]; methodName = parts[2] }
            2 -> { className = parts[0]; methodName = parts[1] }
            else -> return emptyList()
        }

        val fileName = "$className.swift"
        val files = FilenameIndex.getVirtualFilesByName(fileName, scope)

        return files.mapNotNull { vFile ->
            val psiFile = PsiManager.getInstance(project).findFile(vFile) ?: return@mapNotNull null
            val document = PsiDocumentManager.getInstance(project).getDocument(psiFile) ?: return@mapNotNull null
            val text = document.text
            val funcPattern = Regex("""\bfunc\s+${Regex.escape(methodName)}\s*\(""")
            val match = funcPattern.find(text)

            if (match != null) {
                val element = psiFile.findElementAt(match.range.first)
                if (element != null) {
                    PsiLocation.fromPsiElement(element)
                } else {
                    PsiLocation.fromPsiElement(psiFile)
                }
            } else {
                PsiLocation.fromPsiElement(psiFile)
            }
        }
    }
}
