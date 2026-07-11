package cn.enaium.jimmer.buddy.extensions.dto.psi.impl

import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiTypesBlock
import com.intellij.lang.ASTNode
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DtoPsiTypesBlockImpl(node: ASTNode) : ANTLRPsiNode(node), DtoPsiTypesBlock