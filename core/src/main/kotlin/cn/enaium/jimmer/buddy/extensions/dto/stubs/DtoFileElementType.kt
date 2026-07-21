package cn.enaium.jimmer.buddy.extensions.dto.stubs

import cn.enaium.jimmer.buddy.extensions.dto.stub.DtoFileStub
import cn.enaium.jimmer.buddy.extensions.dto.DtoLanguage
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiFile
import com.intellij.psi.PsiFile
import com.intellij.psi.StubBuilder
import com.intellij.psi.stubs.DefaultStubBuilder
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.IStubFileElementType
import org.jetbrains.annotations.NonNls

/**
 * @author Enaium
 */
class DtoFileElementType : IStubFileElementType<DtoFileStub>("DTO_FILE", DtoLanguage) {
    companion object {
        const val STUB_VERSION = 1
    }

    override fun getStubVersion(): Int {
        return STUB_VERSION
    }

    override fun getBuilder(): StubBuilder {
        return object : DefaultStubBuilder() {
            override fun createStubForFile(file: PsiFile): StubElement<*> {
                return DtoFileStub(file as DtoPsiFile)
            }
        }
    }

    override fun getExternalId(): String {
        return "JimmerBuddy.DTO.FILE"
    }
}