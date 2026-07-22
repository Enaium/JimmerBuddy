package cn.enaium.jimmer.buddy.extensions.dto.psi.impl

import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiPropName
import cn.enaium.jimmer.buddy.utility.createDtoTypeName
import cn.enaium.jimmer.buddy.utility.createPropName
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry

/**
 * @author Enaium
 */
abstract class DtoPsiPropNameMixin(node: ASTNode) : ASTWrapperPsiElement(node), DtoPsiPropName,
    PsiNameIdentifierOwner {
    override fun getNameIdentifier(): PsiElement? {
        return identifier
    }

    override fun getName(): String? {
        return identifier.text
    }

    override fun setName(name: String): PsiElement {
        val newIdentifier = project.createPropName(name)
        identifier.replace(newIdentifier)
        return this
    }

    override fun getReferences(): Array<PsiReference> {
        return ReferenceProvidersRegistry.getReferencesFromProviders(this)
    }
}