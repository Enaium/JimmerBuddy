package cn.enaium.jimmer.buddy.extensions.dto.reference

import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiDtoType
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiPropName
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiQualifiedNamePart
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar


/**
 * @author Enaium
 */
class DtoReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(DtoPsiDtoType::class.java),
            DtoTypeNameReferenceProvider
        )
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(DtoPsiPropName::class.java),
            PropNameReferenceProvider
        )
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(DtoPsiQualifiedNamePart::class.java),
            QualifiedNamePartReferenceProvider
        )
    }
}