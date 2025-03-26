package cn.enaium.jimmer.buddy.extensions.inspection

import cn.enaium.jimmer.buddy.JimmerBuddy.PSI_SHARED
import cn.enaium.jimmer.buddy.utility.findPropertyByName
import cn.enaium.jimmer.buddy.utility.getTarget
import cn.enaium.jimmer.buddy.utility.hasImmutableAnnotation
import cn.enaium.jimmer.buddy.utility.toAny
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethod
import org.babyfish.jimmer.Formula
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.containingClass

/**
 * @author Enaium
 */
class FormulaAnnotationInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: com.intellij.psi.PsiElement) {
                if (element is PsiMethod && element.containingClass?.hasImmutableAnnotation() == true) {
                    element.annotations.find { it.qualifiedName == Formula::class.qualifiedName }?.also {
                        val dependencies = (it.findAttributeValue("dependencies")
                            ?.toAny(Array<String>::class.java) as? Array<*>)?.map { it.toString() }
                            ?.takeIf { it.isNotEmpty() } ?: run {
                            element.body?.also {
                                holder.registerProblem(
                                    element,
                                    "The dependencies is empty"
                                )
                            }
                            return@also
                        }
                        dependencies.forEach { dependency ->
                            val trace = dependency.split(".")
                            var containingClass = element.containingClass

                            trace.forEach {
                                containingClass?.findMethodsByName(it, true)?.takeIf { it.isNotEmpty() }?.also {
                                    containingClass = it.first().getTarget()
                                } ?: run {
                                    holder.registerProblem(element, "The dependency '$it' does not exist")
                                    return@also
                                }
                            }
                        }
                    }
                } else if (element is KtProperty && element.containingClass()?.hasImmutableAnnotation() == true) {
                    PSI_SHARED.annotations(element).find { it.fqName == Formula::class.qualifiedName }
                        ?.also {
                            val dependencies =
                                (it.arguments.find { argument -> argument.name == "dependencies" }?.value as? List<*>)?.map { it.toString() }
                                    ?: run {
                                        element.getter?.also {
                                            holder.registerProblem(
                                                element,
                                                "The dependencies is empty"
                                            )
                                        }
                                        return@also
                                    }
                            dependencies.forEach { dependency ->
                                val trace = dependency.split(".")
                                var containingClass = element.containingClass()
                                trace.forEach {
                                    containingClass?.findPropertyByName(it, true)?.also {
                                        containingClass = (it as KtProperty).getTarget()
                                    } ?: run {
                                        holder.registerProblem(element, "The dependency '$it' does not exist")
                                        return@also
                                    }
                                }
                            }
                        }
                }
            }
        }
    }
}