package cn.enaium.jimmer.buddy.extensions.dto

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.utility.DTO_FILE
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

/**
 * @author Enaium
 */
object DtoFileType : LanguageFileType(DtoLanguage) {
    override fun getName(): String = JimmerBuddy.DTO_NAME

    override fun getDescription(): String = JimmerBuddy.DTO_NAME

    override fun getDefaultExtension(): String = "dto"

    override fun getIcon(): Icon = JimmerBuddy.Icons.DTO_FILE
}