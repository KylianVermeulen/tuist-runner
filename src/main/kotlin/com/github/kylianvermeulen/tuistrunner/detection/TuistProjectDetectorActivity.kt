package com.github.kylianvermeulen.tuistrunner.detection

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.github.kylianvermeulen.tuistrunner.TuistBundle

class TuistProjectDetectorActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        val service = project.service<TuistProjectService>()
        service.detectProject()

        if (service.isTuistProject && service.tuistExecutablePath == null) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Tuist Runner")
                .createNotification(
                    TuistBundle.message("detection.notification.title"),
                    TuistBundle.message("detection.notification.cliNotFound"),
                    NotificationType.WARNING,
                )
                .notify(project)
        }
    }
}
