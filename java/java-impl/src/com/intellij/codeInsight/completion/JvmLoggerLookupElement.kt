// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.codeInsight.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.lang.logging.JvmLogger
import com.intellij.lang.logging.JvmLoggerFieldDelegate
import com.intellij.psi.*
import com.intellij.psi.statistics.StatisticsInfo
import com.intellij.psi.util.parentOfType

class JvmLoggerLookupElement(private val logger: JvmLogger, private val place: PsiClass) : LookupElement(), JavaCompletionStatistician.CustomStatisticsInfoProvider {
  val typeName: String = logger.loggerTypeName

  override fun getLookupString(): String = JvmLoggerFieldDelegate.LOGGER_IDENTIFIER

  override fun handleInsert(context: InsertionContext) {
    val loggerText = logger.createLogger(context.project, place) ?: return
    logger.insertLoggerAtClass(context.project, place, loggerText)
    replaceWithStaticReferenceIfCollisions(context, place)
  }

  override fun renderElement(presentation: LookupElementPresentation) {
    super.renderElement(presentation)
    presentation.tailText = " ${logger.loggerTypeName}"
    presentation.typeText = "$logger"
  }

  private fun replaceWithStaticReferenceIfCollisions(context: InsertionContext,
                                                     place: PsiClass) {
    val file = context.file
    val element = file.findElementAt(context.startOffset)?.parentOfType<PsiReferenceExpression>(false) ?: return
    val resolved = element.resolve() as? PsiField
    val containingClass = resolved?.containingClass
    if (resolved == null || !PsiManager.getInstance(context.project).areElementsEquivalent(place, containingClass)) {
      val factory = JavaPsiFacade.getElementFactory(context.project)
      val className = place.qualifiedName ?: return
      val staticRefExpression = factory.createExpressionFromText("$className.${JvmLoggerFieldDelegate.LOGGER_IDENTIFIER}", place)
      element.replace(staticRefExpression)
    }
  }

  override fun getStatisticsInfo(): StatisticsInfo = StatisticsInfo("Jvm logger", logger.loggerTypeName)
}