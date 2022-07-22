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

package eu.cloudnetservice.node.setup;

import eu.cloudnetservice.common.JavaVersion;
import eu.cloudnetservice.common.collection.Pair;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.driver.service.GroupConfiguration;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.console.animation.setup.ConsoleSetupAnimation;
import eu.cloudnetservice.node.console.animation.setup.answer.Parsers;
import eu.cloudnetservice.node.console.animation.setup.answer.QuestionAnswerType;
import eu.cloudnetservice.node.console.animation.setup.answer.QuestionListEntry;
import eu.cloudnetservice.node.util.NetworkUtil;
import eu.cloudnetservice.node.version.ServiceVersion;
import eu.cloudnetservice.node.version.ServiceVersionType;
import eu.cloudnetservice.node.version.information.TemplateVersionInstaller;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;

public class SpecificTaskSetup extends DefaultTaskSetup implements DefaultSetup {

  public SpecificTaskSetup(@NonNull Node node) {
    super(node);
  }

  @Override
  public void applyQuestions(@NonNull ConsoleSetupAnimation animation) {
    animation.addEntries(
      QuestionListEntry.<String>builder()
        .key("taskName")
        .translatedQuestion("command-tasks-setup-question-name")
        .answerType(QuestionAnswerType.<String>builder()
          .parser(Parsers.allOf(
            Parsers.nonExistingTask(),
            Parsers.regex(ServiceTask.NAMING_PATTERN))))
        .build(),
      QuestionListEntry.<Integer>builder()
        .key("taskMemory")
        .translatedQuestion("command-tasks-setup-question-memory")
        .answerType(QuestionAnswerType.<Integer>builder()
          .parser(Parsers.ranged(128, Integer.MAX_VALUE))
          .recommendation(512))
        .build(),
      QuestionListEntry.<Boolean>builder()
        .key("taskMaintenance")
        .translatedQuestion("command-tasks-setup-question-maintenance")
        .answerType(QuestionAnswerType.<Boolean>builder()
          .recommendation("no")
          .possibleResults("yes", "no")
          .parser(Parsers.bool()))
        .build(),
      QuestionListEntry.<Boolean>builder()
        .key("taskStaticServices")
        .translatedQuestion("command-tasks-setup-question-static")
        .answerType(QuestionAnswerType.<Boolean>builder()
          .possibleResults("yes", "no")
          .parser(Parsers.bool()))
        .build(),
      QuestionListEntry.<Integer>builder()
        .key("taskMinServices")
        .translatedQuestion("command-tasks-setup-question-minservices")
        .answerType(QuestionAnswerType.<Integer>builder()
          .parser(Parsers.ranged(0, Integer.MAX_VALUE)))
        .build(),
      QuestionListEntry.<ServiceEnvironmentType>builder()
        .key("taskEnvironment")
        .translatedQuestion("command-tasks-setup-question-environment")
        .answerType(QuestionAnswerType.<ServiceEnvironmentType>builder()
          .parser(Parsers.serviceEnvironmentType())
          .possibleResults(this.node.serviceVersionProvider().knownEnvironments().keySet()))
        .build(),
      QuestionListEntry.<Integer>builder()
        .key("taskStartPort")
        .translatedQuestion("command-tasks-setup-question-startport")
        .answerType(QuestionAnswerType.<Integer>builder()
          .parser(Parsers.ranged(0, 0xFFFF))
          .possibleResults("[0, 65535]")
          .recommendation(44955))
        .build(),
      QuestionListEntry.<String>builder()
        .key("taskHostAddress")
        .translatedQuestion("command-tasks-setup-question-host-address")
        .answerType(QuestionAnswerType.<String>builder()
          .recommendation(this.node.config().hostAddress())
          .parser(Parsers.assignableHostAndPortOrAlias())
          .possibleResults(() -> NetworkUtil.availableIPAddresses()
            .stream()
            .collect(Collectors.collectingAndThen(Collectors.toSet(), ips -> {
              ips.addAll(this.node.config().ipAliases().keySet());
              return ips;
            }))))
        .build(),
      QuestionListEntry.<Pair<String, JavaVersion>>builder()
        .key("taskJavaCommand")
        .translatedQuestion("command-tasks-setup-question-javacommand")
        .answerType(QuestionAnswerType.<Pair<String, JavaVersion>>builder()
          .recommendation("java")
          .possibleResults("java")
          .parser(Parsers.javaVersion()))
        .build(),
      QuestionListEntry.<Pair<ServiceVersionType, ServiceVersion>>builder()
        .key("taskServiceVersion")
        .translatedQuestion("command-tasks-setup-question-application")
        .answerType(QuestionAnswerType.<Pair<ServiceVersionType, ServiceVersion>>builder()
          .possibleResults(() -> this.completableServiceVersions(
            animation.result("taskEnvironment"),
            animation.result("taskJavaCommand")))
          .parser(Parsers.serviceVersion()))
        .build(),
      QuestionListEntry.<String>builder()
        .key("taskNameSplitter")
        .translatedQuestion("command-tasks-setup-question-name-splitter")
        .answerType(QuestionAnswerType.<String>builder()
          .recommendation("-")
          .parser(Parsers.regex(ServiceTask.NAMING_PATTERN)))
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
    String hostAddress = animation.result("taskHostAddress");

    var task = ServiceTask.builder()
      .name(name)
      .groups(Set.of(name))
      .maxHeapMemory(animation.result("taskMemory"))
      .maintenance(animation.result("taskMaintenance"))
      .staticServices(animation.result("taskStaticServices"))
      .minServiceCount(animation.result("taskMinServices"))
      .serviceEnvironmentType(environment)
      .startPort(animation.result("taskStartPort"))
      .hostAddress(hostAddress)
      .javaCommand(javaVersion.first())
      .templates(Set.of(defaultTemplate))
      .nameSplitter(animation.result("taskNameSplitter"))
      .build();
    // create the default template for the task
    this.initializeTemplate(defaultTemplate, environment, true);
    // check if the user chose to install a version
    if (version != null) {
      // install the chosen version
      this.node.serviceVersionProvider().installServiceVersion(TemplateVersionInstaller.builder()
        .serviceVersionType(version.first())
        .serviceVersion(version.second())
        .toTemplate(defaultTemplate)
        .executable(javaVersion.first())
        .build(), false);
    }
    // add the task after the template is created
    this.node.serviceTaskProvider().addServiceTask(task);
    // create a group with the same name
    var groupConfiguration = GroupConfiguration.builder().name(name).build();
    this.node.groupConfigurationProvider().addGroupConfiguration(groupConfiguration);
    LOGGER.info(I18n.trans("command-tasks-setup-create-success", task.name()));
  }
}
