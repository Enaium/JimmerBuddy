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

package cn.enaium.jimmer.buddy.extensions.wizard

import cn.enaium.jimmer.buddy.JimmerBuddy
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.GeneratorNewProjectWizard
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.observable.properties.PropertyGraph
import javax.swing.Icon

/**
 * @author Enaium
 */
class JimmerProjectWizard : GeneratorNewProjectWizard {
    override val icon: Icon
        get() = JimmerBuddy.Icons.LOGO_NORMAL
    override val id: String
        get() = JimmerBuddy.NAME
    override val name: String
        get() = JimmerBuddy.JIMMER_NAME

    private val propertyGraph: PropertyGraph
        get() = PropertyGraph("Jimmer project")

    private val projectModelProperty = propertyGraph.property<JimmerProjectModel>(JimmerProjectModel())
    val projectModel: JimmerProjectModel by projectModelProperty

    override fun createStep(context: WizardContext): NewProjectWizardStep {
        context.putUserData(JimmerBuddy.PROJECT_MODEL_PROP_KEY, projectModelProperty)
        return JimmerProjectWizardStep(context, propertyGraph)
    }
}