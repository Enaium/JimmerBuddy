package cn.enaium.jimmer.buddy.extensions.dto.index

import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiDtoType
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey


/**
 * @author Enaium
 */
class DtoTypeIndex : StringStubIndexExtension<DtoPsiDtoType>() {
    companion object {
        val KEY = StubIndexKey.createIndexKey<String, DtoPsiDtoType>("JimmerBuddy.DTO.DTO_TYPE")
    }

    override fun getKey(): StubIndexKey<String, DtoPsiDtoType> = KEY
}