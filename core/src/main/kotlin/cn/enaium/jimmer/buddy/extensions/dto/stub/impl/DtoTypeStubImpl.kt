package cn.enaium.jimmer.buddy.extensions.dto.stub.impl

import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiDtoType
import cn.enaium.jimmer.buddy.extensions.dto.stub.DtoTypeStub
import cn.enaium.jimmer.buddy.extensions.dto.stubs.DtoStubElementTypes
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement

/**
 * @author Enaium
 */
class DtoTypeStubImpl(parent: StubElement<*>, override val name: String) :
    StubBase<DtoPsiDtoType>(parent, DtoStubElementTypes.DTO_TYPE), DtoTypeStub