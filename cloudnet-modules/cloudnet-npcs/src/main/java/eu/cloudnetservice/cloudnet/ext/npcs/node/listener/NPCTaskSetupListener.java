/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.ext.npcs.node.listener;

import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionListEntry;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeBoolean;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.event.setup.SetupCompleteEvent;
import de.dytanic.cloudnet.event.setup.SetupResponseEvent;
import eu.cloudnetservice.cloudnet.ext.npcs.configuration.NPCConfiguration;
import eu.cloudnetservice.cloudnet.ext.npcs.configuration.NPCConfigurationEntry;
import eu.cloudnetservice.cloudnet.ext.npcs.node.CloudNetNPCModule;

public class NPCTaskSetupListener {

  private final CloudNetNPCModule npcModule;

  public NPCTaskSetupListener(CloudNetNPCModule npcModule) {
    this.npcModule = npcModule;
  }

  @EventListener
  public void handleSetupComplete(SetupCompleteEvent event) {
    if (!event.getSetup().getName().equals("TaskSetup")) {
      return;
    }

    if (event.getSetup().hasResult("GenerateDefaultNPCConfig")) {
      String taskName = (String) event.getSetup().getResult("name");
      boolean generateDefaultConfig = (Boolean) event.getSetup().getResult("GenerateDefaultNPCConfig");

      if (!generateDefaultConfig) {
        return;
      }

      NPCConfiguration npcConfiguration = this.npcModule.getNPCConfiguration();
      if (npcConfiguration.getConfigurations().stream().noneMatch(entry -> entry.getTargetGroup().equals(taskName))) {
        npcConfiguration.getConfigurations().add(new NPCConfigurationEntry(taskName));
        this.npcModule.saveNPCConfiguration();
      }
    }
  }

  @EventListener
  public void handleSetupResponse(SetupResponseEvent event) {
    if (!event.getSetup().getName().equals("TaskSetup") || !(event.getResponse() instanceof ServiceEnvironmentType)
      || event.getSetup().hasResult("GenerateDefaultNPCConfig")) {
      return;
    }

    ServiceEnvironmentType environment = (ServiceEnvironmentType) event.getResponse();
    if (environment != ServiceEnvironmentType.MINECRAFT_SERVER) {
      return;
    }

    event.getSetup().addEntry(new QuestionListEntry<>(
      "GenerateDefaultNPCConfig",
      LanguageManager.getMessage("module-npcs-tasks-setup-generate-default-config"),
      new QuestionAnswerTypeBoolean() {
        @Override
        public String getRecommendation() {
          return super.getFalseString();
        }
      }
    ));
  }

}
