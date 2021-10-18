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

package eu.cloudnetservice.cloudnet.ext.signs.node.util;

import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.questionlist.ConsoleQuestionListAnimation;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionListEntry;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeBoolean;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.cloudnet.ext.signs.SignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignConfigurationEntry;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignsConfiguration;
import eu.cloudnetservice.cloudnet.ext.signs.node.configuration.SignConfigurationType;
import java.util.Collections;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Internal
public final class SignEntryTaskSetup {

  private static final QuestionListEntry<Boolean> CREATE_ENTRY_QUESTION_LIST = new QuestionListEntry<>(
    "GenerateDefaultSignsConfig",
    LanguageManager.getMessage("module-signs-tasks-setup-generate-default-config"),
    new QuestionAnswerTypeBoolean() {
      @Override
      public String getRecommendation() {
        return super.getFalseString();
      }
    }
  );

  private SignEntryTaskSetup() {
    throw new UnsupportedOperationException();
  }

  public static void addSetupQuestionIfNecessary(@NotNull ConsoleQuestionListAnimation animation,
    @NotNull ServiceEnvironmentType type) {
    if (!animation.hasResult("GenerateDefaultSignsConfig")
      && (type.isMinecraftJavaServer() || type.isMinecraftBedrockServer())) {
      animation.addEntry(CREATE_ENTRY_QUESTION_LIST);
    }
  }

  public static void handleSetupComplete(@NotNull ConsoleQuestionListAnimation animation,
    @NotNull SignsConfiguration configuration,
    @NotNull SignManagement signManagement) {
    if (animation.getName().equals("TaskSetup") && animation.hasResult("GenerateDefaultSignsConfig")) {
      String taskName = (String) animation.getResult("name");
      ServiceEnvironmentType environment = (ServiceEnvironmentType) animation.getResult("environment");
      Boolean generateSignsConfig = (Boolean) animation.getResult("GenerateDefaultSignsConfig");

      if (taskName != null && environment != null && generateSignsConfig != null && generateSignsConfig
        && !SignPluginInclusion.hasConfigurationEntry(Collections.singleton(taskName), configuration)) {
        SignConfigurationEntry entry = environment.isMinecraftJavaServer()
          ? SignConfigurationType.JAVA.createEntry(taskName)
          : SignConfigurationType.BEDROCK.createEntry(taskName);
        configuration.getConfigurationEntries().add(entry);
        signManagement.setSignsConfiguration(configuration);
      }
    }
  }
}
