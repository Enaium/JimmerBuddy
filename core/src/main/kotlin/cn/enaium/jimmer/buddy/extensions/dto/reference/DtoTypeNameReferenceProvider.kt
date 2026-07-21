package cn.enaium.jimmer.buddy.extensions.dto.reference

import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiDtoType
import cn.enaium.jimmer.buddy.utility.generatedName
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.base.util.allScope

/**
 * @author Enaium
 */
object DtoTypeNameReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<out PsiReference> {
        return arrayOf(DtoTypeReference(element as? DtoPsiDtoType ?: return emptyArray()))
    }

    class DtoTypeReference(
        val dtoType: DtoPsiDtoType
    ) : PsiReferenceBase<DtoPsiDtoType>(
        dtoType,
        dtoType.identifier.textRangeInParent
    ) {

        override fun resolve(): PsiElement? {
            return JavaPsiFacade.getInstance(dtoType.project)
                .findClass(dtoType.generatedName() ?: return null, dtoType.project.allScope())
        }

        override fun handleElementRename(newElementName: String): PsiElement {
            element.setName(newElementName)
            return element
        }
    }
}