package cn.enaium.jimmer.buddy.extensions.insight

import cn.enaium.jimmer.buddy.utility.hasErrorFamilyAnnotation
import cn.enaium.jimmer.buddy.utility.snakeToCamelCase
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.PlatformIcons
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.idea.base.util.projectScope
import org.jetbrains.kotlin.idea.codeinsight.utils.findExistingEditor
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.psiUtil.containingClass

/**
 * @author Enaium
 */
class ErrorFamilyLinkMarkerProvider : RelatedItemLineMarkerProvider() {

    val errorCode = "ErrorCode"
    val error = "Error"


    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val (className, enumName) = when (element) {
            is PsiEnumConstant -> {
                element.containingClass?.takeIf { element.containingClass?.hasErrorFamilyAnnotation() == true }?.let {
                    (it.qualifiedName ?: return@let null) to element.nameIdentifier.text
                } ?: return
            }

            is KtEnumEntry -> {
                element.containingClass()?.takeIf { it.hasErrorFamilyAnnotation() }?.let {
                    (it.fqName?.asString() ?: return@let null) to (element.nameIdentifier?.text ?: return@let null)
                } ?: return
            }

            else -> {
                return
            }
        }

        val innerClassName = className.let {
            if (it.endsWith(errorCode)) {
                it.subSequence(0, it.length - errorCode.length)
            } else if (it.endsWith(error)) {
                it.subSequence(0, it.length - error.length)
            } else {
                it
            }
        }.let { "${it}Exception" } + "." + enumName.lowercase().snakeToCamelCase(true)

        result.add(
            NavigationGutterIconBuilder.create(PlatformIcons.EXCEPTION_CLASS_ICON).setTargets()
                .createLineMarkerInfo(element) { _, _ ->
                    val target =
                        JavaPsiFacade.getInstance(element.project).findClass(innerClassName, element.project.allScope())
                            ?.also {
                                ReferencesSearch.search(it, element.project.projectScope())
                                    .toCollection(mutableListOf())
                            } ?: return@createLineMarkerInfo
                    GotoDeclarationAction.startFindUsages(
                        element.findExistingEditor() ?: return@createLineMarkerInfo,
                        target.project,
                        target,
                        null
                    )
                }
        )
        super.collectNavigationMarkers(element, result)
    }
}