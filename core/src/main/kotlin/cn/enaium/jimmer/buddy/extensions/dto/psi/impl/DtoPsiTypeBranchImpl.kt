package cn.enaium.jimmer.buddy.extensions.dto.psi.impl

import cn.enaium.jimmer.buddy.extensions.dto.DtoLanguage.findChild
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiQualifiedName
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiTypeBranch
import com.intellij.lang.ASTNode
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

/**
 * @author Enaium
 */
class DtoPsiTypeBranchImpl(node: ASTNode) : ANTLRPsiNode(node), DtoPsiTypeBranch {
    override val qualifiedName: DtoPsiQualifiedName?
        get() = findChild<DtoPsiQualifiedName>("/typeBranch/qualifiedName")
}