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
import static de.dytanic.cloudnet.console.animation.setup.answer.Parsers.javaVersion;
import static de.dytanic.cloudnet.console.animation.setup.answer.Parsers.nonExistingTask;
import static de.dytanic.cloudnet.console.animation.setup.answer.Parsers.ranged;
import static de.dytanic.cloudnet.console.animation.setup.answer.Parsers.regex;
import static de.dytanic.cloudnet.console.animation.setup.answer.Parsers.serviceEnvironmentType;
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
import lombok.NonNull;

public class SpecificTaskSetup extends DefaultTaskSetup implements DefaultSetup {

  @Override
  public void applyQuestions(@NonNull ConsoleSetupAnimation animation) {
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
          .parser(serviceEnvironmentType())
          .possibleResults(CloudNet.instance().serviceVersionProvider().knownEnvironments().keySet()))
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
            animation.result("taskEnvironment"),
            animation.result("taskJavaCommand")))
          .parser(serviceVersion()))
        .build(),
      QuestionListEntry.<String>builder()
        .key("taskNameSplitter")
        .translatedQuestion("command-tasks-setup-question-name-splitter")
        .answerType(QuestionAnswerType.<String>builder()
          .recommendation("-")
          .parser(regex(ServiceTask.NAMING_PATTERN)))
        .build()
    );
  }

  @Override
  public void handleResults(@NonNull ConsoleSetupAnimation animation) {
    String name = animation.result("taskName");
    ServiceEnvironmentType environment = animation.result("taskEnvironment");
    Pair<ServiceVersionType, ServiceVersion> version = animation.result("taskServiceVersion");
    Pair<String, ?> javaVersion = animation.result("taskJavaCommand");
    var defaultTemplate = ServiceTemplate.builder().prefix(name).name("default").build();

    var task = ServiceTask.builder()
      .name(name)
      .maxHeapMemory(animation.result("taskMemory"))
      .maintenance(animation.result("taskMaintenance"))
      .autoDeleteOnStop(animation.result("taskAutoDelete"))
      .staticServices(animation.result("taskStaticServices"))
      .minServiceCount(animation.result("taskMinServices"))
      .serviceEnvironmentType(environment)
      .startPort(animation.result("taskStartPort"))
      .javaCommand(javaVersion.first())
      .addTemplate(defaultTemplate)
      .nameSplitter(animation.result("taskNameSplitter"))
      .build();
    CloudNet.instance().serviceTaskProvider().addPermanentServiceTask(task);
    // create a group with the same name
    var groupConfiguration = GroupConfiguration.builder().name(name).build();
    CloudNet.instance().groupConfigurationProvider().addGroupConfiguration(groupConfiguration);

    // create the default template for the task
    this.initializeTemplate(defaultTemplate, environment, true);
    // check if the user chose to install a version
    if (version != null) {
      // install the chosen version
      CloudNet.instance().serviceVersionProvider().installServiceVersion(InstallInformation.builder()
        .serviceVersionType(version.first())
        .serviceVersion(version.second())
        .toTemplate(defaultTemplate)
        .executable(javaVersion.first())
        .build(), false);
    }
  }
}
