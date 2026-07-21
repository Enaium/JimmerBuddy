package cn.enaium.jimmer.buddy.dto.psi.impl

import cn.enaium.jimmer.buddy.dto.psi.DtoPsiDtoType
import cn.enaium.jimmer.buddy.dto.stub.DtoTypeStub
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType

/**
 * @author Enaium
 */
abstract class DtoPsiDtoTypeMixin : StubBasedPsiElementBase<DtoTypeStub>, DtoPsiDtoType {
    constructor(node: ASTNode) : super(node)
    constructor(stub: DtoTypeStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)
}