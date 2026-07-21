package cn.enaium.jimmer.buddy.extensions.dto.reference

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*


/**
 * @author Enaium
 */
class DtoReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(),
            DtoTypeNameReferenceProvider
        )
    }
}