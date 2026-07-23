package cn.enaium.jimmer.buddy.extensions.dto.pattern

import com.intellij.psi.PsiElement

/**
 * @author Enaium
 */
object DtoPsiPatterns {
    fun psiElement() = psiElement(PsiElement::class.java)
    fun psiElement(klass: Class<out PsiElement>) = DtoPsiPattern.Capture(klass)
}