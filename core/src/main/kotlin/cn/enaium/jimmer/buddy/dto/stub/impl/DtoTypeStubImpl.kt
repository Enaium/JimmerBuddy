package cn.enaium.jimmer.buddy.dto.stub.impl

import cn.enaium.jimmer.buddy.dto.psi.DtoPsiDtoType
import cn.enaium.jimmer.buddy.dto.stub.DtoTypeStub
import cn.enaium.jimmer.buddy.dto.stubs.DtoStubElementTypes
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement

/**
 * @author Enaium
 */
class DtoTypeStubImpl(parent: StubElement<*>, override val name: String) :
    StubBase<DtoPsiDtoType>(parent, DtoStubElementTypes.DTO_TYPE), DtoTypeStub