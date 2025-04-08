/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.signs.impl.node.util;

import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.modules.signs.SignManagement;
import eu.cloudnetservice.modules.signs.configuration.SignsConfiguration;
import eu.cloudnetservice.modules.signs.impl.node.configuration.SignConfigurationType;
import eu.cloudnetservice.node.impl.console.animation.setup.ConsoleSetupAnimation;
import eu.cloudnetservice.node.impl.console.animation.setup.answer.Parsers;
import eu.cloudnetservice.node.impl.console.animation.setup.answer.QuestionAnswerType;
import eu.cloudnetservice.node.impl.console.animation.setup.answer.QuestionListEntry;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public final class SignEntryTaskSetup {

  private final QuestionListEntry<Boolean> createEntryQuestionList;

  @Inject
  public SignEntryTaskSetup(@NonNull Parsers parsers) {
    this.createEntryQuestionList = QuestionListEntry.<Boolean>builder()
      .key("generateDefaultSignConfigurationEntry")
      .translatedQuestion("module-sign-tasks-setup-generate-default-config")
      .answerType(QuestionAnswerType.<Boolean>builder()
        .parser(parsers.bool())
        .recommendation("no")
        .possibleResults("yes", "no")
        .build())
      .build();
  }

  public void addSetupQuestionIfNecessary(
    @NonNull ConsoleSetupAnimation animation,
    @NonNull ServiceEnvironmentType type
  ) {
    if (!animation.hasResult("generateDefaultSignConfigurationEntry")
      && ServiceEnvironmentType.minecraftServer(type)) {
      animation.addEntries(this.createEntryQuestionList);
    }
  }

  public void handleSetupComplete(
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
        && !configuration.hasEntry(taskName)
      ) {
        var entry = environment.readProperty(ServiceEnvironmentType.JAVA_SERVER)
          ? SignConfigurationType.JAVA.createEntry(taskName)
          : SignConfigurationType.BEDROCK.createEntry(taskName);

        var builder = SignsConfiguration.builder(configuration).modifyEntries(entries -> entries.add(entry));
        signManagement.signsConfiguration(builder.build());
      }
    }
  }
}
