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

package de.dytanic.cloudnet.ext.signs.node.listener;

import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionListEntry;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeBoolean;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.event.setup.SetupCompleteEvent;
import de.dytanic.cloudnet.event.setup.SetupResponseEvent;
import de.dytanic.cloudnet.ext.signs.configuration.SignConfiguration;
import de.dytanic.cloudnet.ext.signs.configuration.SignConfigurationReaderAndWriter;
import de.dytanic.cloudnet.ext.signs.configuration.entry.SignConfigurationEntryType;
import de.dytanic.cloudnet.ext.signs.node.CloudNetSignsModule;

public class SignsTaskSetupListener {

  @EventListener
  public void handleSetupComplete(SetupCompleteEvent event) {
    if (!event.getSetup().getName().equals("TaskSetup")) {
      return;
    }

    if (event.getSetup().hasResult("GenerateDefaultSignsConfig")) {
      String taskName = (String) event.getSetup().getResult("name");
      ServiceEnvironmentType environment = (ServiceEnvironmentType) event.getSetup().getResult("environment");
      boolean generateDefaultSignsConfig = (Boolean) event.getSetup().getResult("GenerateDefaultSignsConfig");

      if (!generateDefaultSignsConfig) {
        return;
      }

      SignConfiguration configuration = CloudNetSignsModule.getInstance().getSignConfiguration();
      if (configuration.getConfigurations().stream().noneMatch(entry -> entry.getTargetGroup().equals(taskName))) {
        SignConfigurationEntryType entryType =
          environment == ServiceEnvironmentType.MINECRAFT_SERVER ? SignConfigurationEntryType.BUKKIT
            : SignConfigurationEntryType.NUKKIT;
        configuration.getConfigurations().add(entryType.createEntry(taskName));
        SignConfigurationReaderAndWriter
          .write(configuration, CloudNetSignsModule.getInstance().getConfigurationFilePath());
      }
    }
  }

  @EventListener
  public void handleSetupResponse(SetupResponseEvent event) {
    if (!event.getSetup().getName().equals("TaskSetup") || !(event.getResponse() instanceof ServiceEnvironmentType)
      || event.getSetup().hasResult("GenerateDefaultSignsConfig")) {
      return;
    }

    ServiceEnvironmentType environment = (ServiceEnvironmentType) event.getResponse();
    if (environment != ServiceEnvironmentType.MINECRAFT_SERVER && environment != ServiceEnvironmentType.NUKKIT) {
      return;
    }

    event.getSetup().addEntry(new QuestionListEntry<>(
      "GenerateDefaultSignsConfig",
      LanguageManager.getMessage("module-signs-tasks-setup-generate-default-config"),
      new QuestionAnswerTypeBoolean() {
        @Override
        public String getRecommendation() {
          return super.getFalseString();
        }
      }
    ));
  }

}
