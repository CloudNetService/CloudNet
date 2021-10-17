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
import static de.dytanic.cloudnet.console.animation.setup.answer.Parsers.serviceVersion;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.JavaVersion;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.console.animation.setup.ConsoleSetupAnimation;
import de.dytanic.cloudnet.console.animation.setup.answer.QuestionAnswerType;
import de.dytanic.cloudnet.console.animation.setup.answer.QuestionListEntry;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.template.TemplateStorageUtil;
import de.dytanic.cloudnet.template.install.InstallInformation;
import de.dytanic.cloudnet.template.install.ServiceVersion;
import de.dytanic.cloudnet.template.install.ServiceVersionType;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class DefaultTaskSetup implements DefaultSetup {

  protected static final Logger LOGGER = LogManager.getLogger(DefaultTaskSetup.class);

  protected static final String PROXY_TASK_NAME = "Proxy";
  protected static final String LOBBY_TASK_NAME = "Lobby";

  protected static final String GLOBAL_TEMPLATE_PREFIX = "Global";
  protected static final String GLOBAL_PROXY_GROUP_NAME = "Global-Proxy";
  protected static final String GLOBAL_SERVER_GROUP_NAME = "Global-Server";

  @Override
  public void applyQuestions(@NotNull ConsoleSetupAnimation animation) {
    animation.addEntries(
      // proxy installation request
      QuestionListEntry.<Boolean>builder()
        .key("installProxy")
        .translatedQuestion("cloudnet-init-setup-tasks-should-install-proxy")
        .answerType(QuestionAnswerType.<Boolean>builder()
          .recommendation("yes")
          .possibleResults("yes", "no")
          .parser(bool())
          .addResultListener(($, result) -> {
            if (result) {
              animation.addEntriesFirst(
                // environment
                QuestionListEntry.<ServiceEnvironmentType>builder()
                  .key("proxyEnvironment")
                  .translatedQuestion("cloudnet-init-setup-tasks-proxy-environment")
                  .answerType(QuestionAnswerType.<ServiceEnvironmentType>builder()
                    .parser(enumConstant(ServiceEnvironmentType.class))
                    .possibleResults(Arrays.stream(ServiceEnvironmentType.values())
                      .filter(ServiceEnvironmentType::isMinecraftProxy)
                      .map(ServiceEnvironmentType::name)
                      .collect(Collectors.toList())))
                  .build(),
                // Java command
                QuestionListEntry.<Pair<String, JavaVersion>>builder()
                  .key("proxyJavaCommand")
                  .translatedQuestion("cloudnet-init-setup-tasks-javacommand")
                  .answerType(QuestionAnswerType.<Pair<String, JavaVersion>>builder()
                    .recommendation("java")
                    .possibleResults("java")
                    .parser(javaVersion()))
                  .build(),
                // proxy service version
                QuestionListEntry.<Pair<ServiceVersionType, ServiceVersion>>builder()
                  .key("proxyVersion")
                  .translatedQuestion("cloudnet-init-setup-tasks-proxy-version")
                  .answerType(QuestionAnswerType.<Pair<ServiceVersionType, ServiceVersion>>builder()
                    .possibleResults(() -> this.completableServiceVersions(
                      animation.getResult("proxyEnvironment"),
                      animation.getResult("proxyJavaCommand")))
                    .parser(serviceVersion()))
                  .build());
            }
          }))
        .build(),
      // server installation request
      QuestionListEntry.<Boolean>builder()
        .key("installServer")
        .translatedQuestion("cloudnet-init-setup-tasks-should-install-server")
        .answerType(QuestionAnswerType.<Boolean>builder()
          .recommendation("yes")
          .possibleResults("yes", "no")
          .parser(bool())
          .addResultListener(($, result) -> {
            if (result) {
              animation.addEntriesFirst(
                // environment
                QuestionListEntry.<ServiceEnvironmentType>builder()
                  .key("serverEnvironment")
                  .translatedQuestion("cloudnet-init-setup-tasks-server-environment")
                  .answerType(QuestionAnswerType.<ServiceEnvironmentType>builder()
                    .parser(enumConstant(ServiceEnvironmentType.class))
                    .possibleResults(Arrays.stream(ServiceEnvironmentType.values())
                      .filter(ServiceEnvironmentType::isMinecraftServer)
                      .map(ServiceEnvironmentType::name)
                      .collect(Collectors.toList())))
                  .build(),
                // Java command
                QuestionListEntry.<Pair<String, JavaVersion>>builder()
                  .key("serverJavaCommand")
                  .translatedQuestion("cloudnet-init-setup-tasks-javacommand")
                  .answerType(QuestionAnswerType.<Pair<String, JavaVersion>>builder()
                    .recommendation("java")
                    .possibleResults("java")
                    .parser(javaVersion()))
                  .build(),
                // server service version
                QuestionListEntry.<Pair<ServiceVersionType, ServiceVersion>>builder()
                  .key("serverVersion")
                  .translatedQuestion("cloudnet-init-setup-tasks-server-version")
                  .answerType(QuestionAnswerType.<Pair<ServiceVersionType, ServiceVersion>>builder()
                    .possibleResults(() -> this.completableServiceVersions(
                      animation.getResult("serverEnvironment"),
                      animation.getResult("serverJavaCommand")))
                    .parser(serviceVersion()))
                  .build());
            }
          }))
        .build());
  }

  @Override
  public void handleResults(@NotNull ConsoleSetupAnimation animation) {
    // proxy installation
    if (animation.getResult("installProxy")) {
      this.executeSetup(animation, "proxy", PROXY_TASK_NAME, GLOBAL_PROXY_GROUP_NAME, 256);
    }
    // server installation
    if (animation.getResult("installServer")) {
      this.executeSetup(animation, "server", LOBBY_TASK_NAME, GLOBAL_SERVER_GROUP_NAME, 512);
    }
  }

  protected void executeSetup(
    @NotNull ConsoleSetupAnimation animation,
    @NotNull String resultPrefix,
    @NotNull String taskName,
    @NotNull String groupName,
    int maxHeapMemory
  ) {
    // read the responses
    ServiceEnvironmentType environment = animation.getResult(resultPrefix + "Environment");
    Pair<String, ?> javaCommand = animation.getResult(resultPrefix + "JavaCommand");
    Pair<ServiceVersionType, ServiceVersion> version = animation.getResult(resultPrefix + "Version");
    // create the task
    ServiceTask serviceTask = ServiceTask.builder()
      .name(taskName)
      .minServiceCount(1)
      .autoDeleteOnStop(true)
      .maxHeapMemory(maxHeapMemory)
      .javaCommand(javaCommand.getFirst())
      .serviceEnvironmentType(environment)
      .groups(Collections.singletonList(groupName))
      .startPort(environment.getDefaultStartPort())
      .templates(Collections.singletonList(new ServiceTemplate(taskName, "default", "local")))
      .build();
    CloudNet.getInstance().getServiceTaskProvider().addPermanentServiceTask(serviceTask);

    // initialize all templates of the new task
    serviceTask.getTemplates().forEach(template -> this.initializeTemplate(template, environment));

    // create the group template
    ServiceTemplate template = ServiceTemplate.local(GLOBAL_TEMPLATE_PREFIX, groupName);
    this.initializeTemplate(template, environment);

    // install the template
    CloudNet.getInstance().getServiceVersionProvider().installServiceVersion(InstallInformation
      .builder(version.getFirst(), version.getSecond())
      .toTemplate(template)
      .executable(javaCommand.getFirst())
      .build(), false);

    // add the group configuration
    GroupConfiguration configuration = GroupConfiguration.empty(groupName, environment);
    configuration.getTemplates().add(template);
    // register the group
    CloudNet.getInstance().getGroupConfigurationProvider().addGroupConfiguration(configuration);
  }

  protected void initializeTemplate(@NotNull ServiceTemplate template, @NotNull ServiceEnvironmentType environment) {
    // install the template
    try {
      TemplateStorageUtil.createAndPrepareTemplate(template.storage(), environment);
    } catch (IOException exception) {
      LOGGER.severe("Exception while initializing local template %s with environment %s",
        exception,
        template,
        environment);
    }
  }

  protected @NotNull Collection<String> completableServiceVersions(
    @NotNull ServiceEnvironmentType type,
    @NotNull Pair<String, JavaVersion> javaVersion
  ) {
    return CloudNet.getInstance().getServiceVersionProvider().getServiceVersionTypes().values().stream()
      .filter(serviceVersionType -> serviceVersionType.getTargetEnvironment().getEnvironmentType() == type)
      .flatMap(serviceVersionType -> serviceVersionType.getVersions()
        .stream()
        .filter(version -> version.canRun(javaVersion.getSecond()))
        .map(version -> String.format("%s-%s", serviceVersionType.getName(), version.getName())))
      .collect(Collectors.collectingAndThen(Collectors.toList(), result -> {
        result.add("none");
        return result;
      }));
  }
}
