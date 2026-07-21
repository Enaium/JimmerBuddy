package cn.enaium.jimmer.buddy.extensions.dto.pattern

import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext

/**
 * @author Enaium
 */
open class DtoPsiPattern<T : PsiElement, Self : PsiElementPattern<T, Self>>(klass: Class<T>) :
    PsiElementPattern<T, Self>(klass) {
    override fun getParent(element: PsiElement): PsiElement? {
        return element.parent
    }

    class Capture<T : PsiElement>(klass: Class<T>) : DtoPsiPattern<T, Capture<T>>(klass)

    override fun accepts(o: Any?, context: ProcessingContext?): Boolean {
        return super.accepts(o, context)
    }
}