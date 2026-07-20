package cn.enaium.jimmer.buddy.dto.psi

import cn.enaium.jimmer.buddy.extensions.dto.DtoLanguage
import com.intellij.psi.tree.IElementType

/**
 * @author Enaium
 */
class DtoElementType(debugName: String) : IElementType(debugName, DtoLanguage) {
    override fun toString(): String {
        return "DtoElementType.${super.toString()}"
    }
}