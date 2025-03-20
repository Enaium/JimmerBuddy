/*
 * Copyright 2025 Enaium
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.enaium.jimmer.buddy.extensions

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.utility.hasImmutableAnnotation
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtClass

/**
 * @author Enaium
 */
class ImmutableLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (element is PsiClass && element.hasImmutableAnnotation() || element is KtClass && element.hasImmutableAnnotation()) {
            element.identifyingElement?.also {
                result.add(
                    NavigationGutterIconBuilder.create(JimmerBuddy.Icons.IMMUTABLE).setTargets()
                        .createLineMarkerInfo(it)
                )
            }
            if (element is PsiClass) {
                element.methods.forEach { method ->
                    method.identifyingElement?.also {
                        result.add(
                            NavigationGutterIconBuilder.create(JimmerBuddy.Icons.PROP).setTargets()
                                .createLineMarkerInfo(it)
                        )
                    }
                }
            } else if (element is KtClass) {
                element.getProperties().forEach { property ->
                    property.identifyingElement?.also {
                        result.add(
                            NavigationGutterIconBuilder.create(JimmerBuddy.Icons.PROP).setTargets()
                                .createLineMarkerInfo(it)
                        )
                    }
                }
            }
        }
        super.collectNavigationMarkers(element, result)
    }
}