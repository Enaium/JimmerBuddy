package cn.enaium.jimmer.buddy.extensions.dto

import cn.enaium.jimmer.buddy.JimmerBuddy
import com.intellij.lang.Language

/**
 * @author Enaium
 */
object DtoLanguage : Language(JimmerBuddy.DTO_LANGUAGE_ID) {
    private fun readResolve(): Any = DtoLanguage
}