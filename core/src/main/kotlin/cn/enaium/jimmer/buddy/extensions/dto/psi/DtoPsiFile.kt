package cn.enaium.jimmer.buddy.extensions.dto.psi

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.extensions.dto.DtoFileType
import cn.enaium.jimmer.buddy.extensions.dto.DtoLanguage
import cn.enaium.jimmer.buddy.utility.DTO_FILE
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.util.PsiTreeUtil
import javax.swing.Icon

/**
 * @author Enaium
 */
class DtoPsiFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, DtoLanguage) {

    val exportStatement: DtoPsiExportStatement?
        get() = PsiTreeUtil.getChildOfType(this, DtoPsiExportStatement::class.java)


    override fun getFileType(): FileType {
        return DtoFileType
    }

    override fun toString(): String {
        return JimmerBuddy.DTO_NAME
    }

    override fun getIcon(flags: Int): Icon {
        return JimmerBuddy.Icons.DTO_FILE
    }
}