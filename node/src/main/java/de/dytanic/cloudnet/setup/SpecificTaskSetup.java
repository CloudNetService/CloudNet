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

package de.dytanic.cloudnet.setup;

import static de.dytanic.cloudnet.console.animation.setup.answer.Parsers.bool;
import static de.dytanic.cloudnet.console.animation.setup.answer.Parsers.enumConstant;
import static de.dytanic.cloudnet.console.animation.setup.answer.Parsers.javaVersion;
import static de.dytanic.cloudnet.console.animation.setup.answer.Parsers.nonExistingTask;
import static de.dytanic.cloudnet.console.animation.setup.answer.Parsers.ranged;
import static de.dytanic.cloudnet.console.animation.setup.answer.Parsers.serviceVersion;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.JavaVersion;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.console.animation.setup.ConsoleSetupAnimation;
import de.dytanic.cloudnet.console.animation.setup.answer.QuestionAnswerType;
import de.dytanic.cloudnet.console.animation.setup.answer.QuestionListEntry;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.template.install.InstallInformation;
import de.dytanic.cloudnet.template.install.ServiceVersion;
import de.dytanic.cloudnet.template.install.ServiceVersionType;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class SpecificTaskSetup extends DefaultTaskSetup implements DefaultSetup {

  @Override
  public void applyQuestions(@NotNull ConsoleSetupAnimation animation) {
    animation.addEntries(
      QuestionListEntry.<String>builder()
        .key("taskName")
        .translatedQuestion("command-tasks-setup-question-name")
        .answerType(QuestionAnswerType.<String>builder()
          .parser(nonExistingTask()))
        .build(),
      QuestionListEntry.<Integer>builder()
        .key("taskMemory")
        .translatedQuestion("command-tasks-setup-question-memory")
        .answerType(QuestionAnswerType.<Integer>builder()
          .parser(ranged(128, Integer.MAX_VALUE))
          .recommendation(512))
        .build(),
      QuestionListEntry.<Boolean>builder()
        .key("taskMaintenance")
        .translatedQuestion("command-tasks-setup-question-maintenance")
        .answerType(QuestionAnswerType.<Boolean>builder()
          .recommendation("no")
          .possibleResults("yes", "no")
          .parser(bool()))
        .build(),
      QuestionListEntry.<Boolean>builder()
        .key("taskAutoDelete")
        .translatedQuestion("command-tasks-setup-question-auto-delete")
        .answerType(QuestionAnswerType.<Boolean>builder()
          .recommendation("yes")
          .possibleResults("yes", "no")
          .parser(bool()))
        .build(),
      QuestionListEntry.<Boolean>builder()
        .key("taskStaticServices")
        .translatedQuestion("command-tasks-setup-question-static")
        .answerType(QuestionAnswerType.<Boolean>builder()
          .possibleResults("yes", "no")
          .parser(bool()))
        .build(),
      QuestionListEntry.<Integer>builder()
        .key("taskMinServices")
        .translatedQuestion("command-tasks-setup-question-minservices")
        .answerType(QuestionAnswerType.<Integer>builder()
          .parser(ranged(0, Integer.MAX_VALUE)))
        .build(),
      QuestionListEntry.<ServiceEnvironmentType>builder()
        .key("taskEnvironment")
        .translatedQuestion("command-tasks-setup-question-environment")
        .answerType(QuestionAnswerType.<ServiceEnvironmentType>builder()
          .parser(enumConstant(ServiceEnvironmentType.class))
          .possibleResults(Arrays.stream(ServiceEnvironmentType.VALUES)
            .map(Enum::name)
            .collect(Collectors.toList())))
        .build(),
      QuestionListEntry.<Integer>builder()
        .key("taskStartPort")
        .translatedQuestion("command-tasks-setup-question-startport")
        .answerType(QuestionAnswerType.<Integer>builder()
          .parser(ranged(0, 65535))
          .possibleResults("[0, 65535]")
          .recommendation(44955))
        .build(),
      QuestionListEntry.<Pair<String, JavaVersion>>builder()
        .key("taskJavaCommand")
        .translatedQuestion("command-tasks-setup-question-javacommand")
        .answerType(QuestionAnswerType.<Pair<String, JavaVersion>>builder()
          .recommendation("java")
          .possibleResults("java")
          .parser(javaVersion()))
        .build(),
      QuestionListEntry.<Pair<ServiceVersionType, ServiceVersion>>builder()
        .key("taskServiceVersion")
        .translatedQuestion("command-tasks-setup-question-application")
        .answerType(QuestionAnswerType.<Pair<ServiceVersionType, ServiceVersion>>builder()
          .possibleResults(() -> this.completableServiceVersions(
            animation.getResult("taskEnvironment"),
            animation.getResult("taskJavaCommand")))
          .parser(serviceVersion()))
        .build()
    );
  }

  @Override
  public void handleResults(@NotNull ConsoleSetupAnimation animation) {
    String name = animation.getResult("taskName");
    ServiceEnvironmentType environment = animation.getResult("taskEnvironment");
    Pair<ServiceVersionType, ServiceVersion> version = animation.getResult("taskServiceVersion");
    Pair<String, ?> javaVersion = animation.getResult("taskJavaCommand");
    ServiceTemplate defaultTemplate = new ServiceTemplate(name, "default", "local");

    ServiceTask task = ServiceTask.builder()
      .name(name)
      .maxHeapMemory(animation.getResult("taskMemory"))
      .maintenance(animation.getResult("taskMaintenance"))
      .autoDeleteOnStop(animation.getResult("taskAutoDelete"))
      .staticServices(animation.getResult("taskStaticServices"))
      .minServiceCount(animation.getResult("taskMinServices"))
      .serviceEnvironmentType(environment)
      .startPort(animation.getResult("taskStartPort"))
      .javaCommand(javaVersion.getFirst())
      .templates(Collections.singleton(defaultTemplate))
      .build();
    CloudNet.getInstance().getServiceTaskProvider().addPermanentServiceTask(task);
    // create a group with the same name
    GroupConfiguration groupConfiguration = GroupConfiguration.empty(name, null);
    CloudNet.getInstance().getGroupConfigurationProvider().addGroupConfiguration(groupConfiguration);

    // create the default template for the task
    this.initializeTemplate(defaultTemplate, environment);
    // install the chosen version
    CloudNet.getInstance().getServiceVersionProvider().installServiceVersion(InstallInformation
      .builder(version.getFirst(), version.getSecond())
      .toTemplate(defaultTemplate)
      .executable(javaVersion.getFirst())
      .build(), false);
  }
}
