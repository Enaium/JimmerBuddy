package cn.enaium.jimmer.buddy.extensions.dto.psi.impl

import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiTypesElement
import com.intellij.lang.ASTNode
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DtoPsiTypesElementImpl(node: ASTNode) : ANTLRPsiNode(node), DtoPsiTypesElement