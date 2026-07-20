package cn.enaium.jimmer.buddy.extensions.dto.psi.impl

import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiAnnotationArguments
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiAnnotationNamedArgument
import com.intellij.lang.ASTNode
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

/**
 * @author Enaium
 */
class DtoPsiAnnotationNamedArgumentImpl(node: ASTNode) : ANTLRPsiNode(node), DtoPsiAnnotationArguments,
    DtoPsiAnnotationNamedArgument {

}