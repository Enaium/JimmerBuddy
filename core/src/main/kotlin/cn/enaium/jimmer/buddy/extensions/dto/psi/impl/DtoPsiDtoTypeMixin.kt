package cn.enaium.jimmer.buddy.extensions.dto.psi.impl

import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiDtoType
import cn.enaium.jimmer.buddy.extensions.dto.stub.DtoTypeStub
import cn.enaium.jimmer.buddy.utility.createDtoTypeName
import cn.enaium.jimmer.buddy.utility.generatedName
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.psi.stubs.IStubElementType
import org.jetbrains.kotlin.idea.base.util.allScope

/**
 * @author Enaium
 */
abstract class DtoPsiDtoTypeMixin : StubBasedPsiElementBase<DtoTypeStub>, DtoPsiDtoType, PsiNameIdentifierOwner {
    constructor(node: ASTNode) : super(node)
    constructor(stub: DtoTypeStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun getNameIdentifier(): PsiElement {
        return identifier
    }

    override fun getName(): String? {
        return stub?.name ?: identifier.text
    }

    override fun setName(name: String): PsiElement {
        val newIdentifier = project.createDtoTypeName(name)
        identifier.replace(newIdentifier)
        return this
    }

    override fun getReferences(): Array<PsiReference> {
        return ReferenceProvidersRegistry.getReferencesFromProviders(this)
    }

//    override fun getReference(): PsiReference {
//        return object : PsiReferenceBase<DtoPsiDtoType>(this, firstChild.textRangeInParent) {
//            override fun resolve(): PsiElement? {
//                return JavaPsiFacade.getInstance(project)
//                    .findClass(generatedName() ?: return null, project.allScope())
//            }
//        }
//    }
}