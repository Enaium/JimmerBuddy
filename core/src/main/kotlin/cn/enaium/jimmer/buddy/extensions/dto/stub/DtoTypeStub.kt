package cn.enaium.jimmer.buddy.extensions.dto.stub

import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiDtoType
import com.intellij.psi.stubs.StubElement

/**
 * @author Enaium
 */
interface DtoTypeStub : StubElement<DtoPsiDtoType> {
    val name: String
}