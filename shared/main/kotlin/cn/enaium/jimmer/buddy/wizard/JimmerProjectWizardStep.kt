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

package cn.enaium.jimmer.buddy.wizard

import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.ui.dsl.builder.Panel

/**
 * @author Enaium
 */
class JimmerProjectWizardStep(
    override val context: WizardContext,
    override val propertyGraph: PropertyGraph
) : NewProjectWizardStep {

    override val data: UserDataHolder
        get() = UserDataHolderBase()

    override val keywords: NewProjectWizardStep.Keywords
        get() = NewProjectWizardStep.Keywords()

    override fun setupUI(builder: Panel) {
        JimmerProjectPanel(propertyGraph, context, builder)
    }
}