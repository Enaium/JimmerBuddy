package cn.enaium.jimmer.buddy.dto.stubs

import cn.enaium.jimmer.buddy.dto.index.DtoTypeIndex
import cn.enaium.jimmer.buddy.dto.psi.DtoPsiDtoType
import cn.enaium.jimmer.buddy.dto.psi.impl.DtoPsiDtoTypeImpl
import cn.enaium.jimmer.buddy.dto.stub.DtoTypeStub
import cn.enaium.jimmer.buddy.dto.stub.impl.DtoTypeStubImpl
import cn.enaium.jimmer.buddy.extensions.dto.DtoLanguage
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import com.intellij.util.io.StringRef

/**
 * @author Enaium
 */
class DtoTypeElementType(debugName: String) : IStubElementType<DtoTypeStub, DtoPsiDtoType>(debugName, DtoLanguage) {
    override fun createPsi(stub: DtoTypeStub): DtoPsiDtoType {
        return DtoPsiDtoTypeImpl(stub, this)
    }

    override fun createStub(psi: DtoPsiDtoType, parentStub: StubElement<out PsiElement>): DtoTypeStub {
        return DtoTypeStubImpl(parentStub, psi.identifier.text)
    }

    override fun serialize(stub: DtoTypeStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): DtoTypeStub {
        return DtoTypeStubImpl(parentStub, StringRef.toString(dataStream.readName()))
    }

    override fun indexStub(stub: DtoTypeStub, sink: IndexSink) {
        sink.occurrence(DtoTypeIndex.KEY, stub.name)
    }

    override fun getExternalId(): String {
        return "JimmerBuddy.DTO.DTO_TYPE"
    }

    override fun toString(): String {
        return "DtoTypeElementType.${super.toString()}"
    }
}