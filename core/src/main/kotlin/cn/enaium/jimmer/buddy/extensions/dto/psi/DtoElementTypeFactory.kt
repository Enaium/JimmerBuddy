package cn.enaium.jimmer.buddy.extensions.dto.psi

/**
 * @author Enaium
 */
import cn.enaium.jimmer.buddy.extensions.dto.stubs.DtoStubElementTypes
import com.intellij.psi.tree.IElementType

fun factory(name: String): IElementType {
    return when (name) {
        "DTO_TYPE" -> DtoStubElementTypes.DTO_TYPE

        else -> DtoElementType(name)
    }
}