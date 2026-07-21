package cn.enaium.jimmer.buddy.extensions.dto.stub

import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiFile
import com.intellij.psi.stubs.PsiFileStubImpl

/**
 * @author Enaium
 */
class DtoFileStub(file: DtoPsiFile) : PsiFileStubImpl<DtoPsiFile>(file)