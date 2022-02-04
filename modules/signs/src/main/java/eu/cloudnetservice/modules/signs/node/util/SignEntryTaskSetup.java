/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.signs.node.util;

import eu.cloudnetservice.cloudnet.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.cloudnet.node.console.animation.setup.ConsoleSetupAnimation;
import eu.cloudnetservice.cloudnet.node.console.animation.setup.answer.Parsers;
import eu.cloudnetservice.cloudnet.node.console.animation.setup.answer.QuestionAnswerType;
import eu.cloudnetservice.cloudnet.node.console.animation.setup.answer.QuestionListEntry;
import eu.cloudnetservice.modules.signs.SignManagement;
import eu.cloudnetservice.modules.signs.configuration.SignsConfiguration;
import eu.cloudnetservice.modules.signs.node.configuration.SignConfigurationType;
import java.util.Collections;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class SignEntryTaskSetup {

  private static final QuestionListEntry<Boolean> CREATE_ENTRY_QUESTION_LIST = QuestionListEntry.<Boolean>builder()
    .key("generateDefaultSignConfigurationEntry")
    .translatedQuestion("module-signs-tasks-setup-generate-default-config")
    .answerType(QuestionAnswerType.<Boolean>builder()
      .parser(Parsers.bool())
      .recommendation("no")
      .possibleResults("yes", "no")
      .build())
    .build();

  private SignEntryTaskSetup() {
    throw new UnsupportedOperationException();
  }

  public static void addSetupQuestionIfNecessary(
    @NonNull ConsoleSetupAnimation animation,
    @NonNull ServiceEnvironmentType type
  ) {
    if (!animation.hasResult("generateDefaultSignConfigurationEntry")
      && ServiceEnvironmentType.minecraftServer(type)) {
      animation.addEntries(CREATE_ENTRY_QUESTION_LIST);
    }
  }

  public static void handleSetupComplete(
    @NonNull ConsoleSetupAnimation animation,
    @NonNull SignsConfiguration configuration,
    @NonNull SignManagement signManagement
  ) {
    if (animation.hasResult("generateDefaultSignConfigurationEntry")) {
      String taskName = animation.result("taskName");
      ServiceEnvironmentType environment = animation.result("taskEnvironment");
      Boolean generateSignsConfig = animation.result("generateDefaultSignConfigurationEntry");

      if (taskName != null
        && environment != null
        && generateSignsConfig != null
        && generateSignsConfig
        && !SignPluginInclusion.hasConfigurationEntry(Collections.singleton(taskName), configuration)
      ) {
        var entry = ServiceEnvironmentType.JAVA_SERVER.get(environment.properties())
          ? SignConfigurationType.JAVA.createEntry(taskName)
          : SignConfigurationType.BEDROCK.createEntry(taskName);
        configuration.configurationEntries().add(entry);
        signManagement.signsConfiguration(configuration);
      }
    }
  }
}
