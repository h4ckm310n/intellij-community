// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.ide.startup.importSettings.controllers

import com.intellij.ide.startup.importSettings.fus.TransferSettingsCollector
import com.intellij.ide.startup.importSettings.models.BaseIdeVersion
import com.intellij.ide.startup.importSettings.models.FailedIdeVersion
import com.intellij.ide.startup.importSettings.models.IdeVersion
import com.intellij.ide.startup.importSettings.providers.TransferSettingsPerformContext
import com.intellij.ide.startup.importSettings.providers.TransferSettingsPerformImportTask
import com.intellij.ide.startup.importSettings.transfer.backend.providers.DefaultImportPerformer
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.util.EventDispatcher

class TransferSettingsControllerImpl : TransferSettingsController {
  private val eventDispatcher = EventDispatcher.create(TransferSettingsListener::class.java)
  private var previouslySelected: BaseIdeVersion? = null
  private var timesSwitchedBetweenInstances = -1 // The field will be changed to 0 on the first IDE showing

  override fun updateCheckboxes(ideVersion: IdeVersion) {
    eventDispatcher.multicaster.checkboxesUpdated(ideVersion)
  }

  override fun performImport(project: Project?, ideVersion: IdeVersion, pi: ProgressIndicator) {
    TransferSettingsCollector.logImportStarted(ideVersion.settingsCache, timesSwitchedBetweenInstances)
    eventDispatcher.multicaster.importStarted(ideVersion, ideVersion.settingsCache)
    val performer = getImportPerformer()

    val context = TransferSettingsPerformContext()
    val task = object : TransferSettingsPerformImportTask(project, performer, ideVersion.settingsCache, true, context) {
      override fun onSuccess() {
        TransferSettingsCollector.logImportSucceeded(ideVersion, ideVersion.settingsCache)
        eventDispatcher.multicaster.importPerformed(ideVersion, ideVersion.settingsCache, context)
      }

      override fun onThrowable(error: Throwable) {
        TransferSettingsCollector.logImportFailed(ideVersion)
        eventDispatcher.multicaster.importFailed(ideVersion, ideVersion.settingsCache, error)
      }
    }

    ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, pi)
  }

  override fun performReload(ideVersion: FailedIdeVersion, pi: ProgressIndicator) {
    eventDispatcher.multicaster.reloadPerformed(ideVersion)
  }

  // TODO stateflow instead of ugly listeners
  override fun addListener(listener: TransferSettingsListener) {
    val ps = previouslySelected
    if (ps != null) {
      listener.itemSelected(ps)
    }
    eventDispatcher.addListener(listener)
  }

  override fun itemSelected(ideVersion: BaseIdeVersion) {
    eventDispatcher.multicaster.itemSelected(ideVersion)
    previouslySelected = ideVersion
    timesSwitchedBetweenInstances++
  }

  override fun getImportPerformer() = DefaultImportPerformer()
}