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

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.console.animation.setup.ConsoleSetupAnimation;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.provider.NodeGroupConfigurationProvider;
import de.dytanic.cloudnet.provider.NodeServiceTaskProvider;
import de.dytanic.cloudnet.template.TemplateStorageUtil;
import de.dytanic.cloudnet.template.install.ServiceVersion;
import de.dytanic.cloudnet.template.install.ServiceVersionType;
import java.io.IOException;
import java.util.Collections;

// TODO
public class DefaultTaskSetup implements DefaultSetup {

  private static final Logger LOGGER = LogManager.getLogger(DefaultTaskSetup.class);
  private static final String GLOBAL_TEMPLATE_PREFIX = "Global";
  private static final String GLOBAL_PROXY_GROUP_NAME = "Global-Proxy";
  private static final String GLOBAL_SERVER_GROUP_NAME = "Global-Server";
  private static final String PROXY_TASK_NAME = "Proxy";
  private static final String LOBBY_TASK_NAME = "Lobby";

  private boolean shouldExecute = false;

  @Override
  @SuppressWarnings("unchecked")
  public void postExecute(ConsoleSetupAnimation animation) {
    if (!this.shouldExecute) {
      return;
    }

    boolean installProxy = (boolean) animation.getResult("installProxy");
    boolean installServer = (boolean) animation.getResult("installServer");

    ServiceEnvironmentType proxyEnvironment = (ServiceEnvironmentType) animation.getResult("proxyEnvironment");
    Pair<ServiceVersionType, ServiceVersion> proxyVersion = (Pair<ServiceVersionType, ServiceVersion>) animation
      .getResult("proxyVersion");

    ServiceEnvironmentType serverEnvironment = (ServiceEnvironmentType) animation.getResult("serverEnvironment");
    Pair<ServiceVersionType, ServiceVersion> serverVersion = (Pair<ServiceVersionType, ServiceVersion>) animation
      .getResult("serverVersion");

    String lobbyJavaCommand = ((Pair<String, ?>) animation.getResult("javaCommand")).getFirst();

    GroupConfiguration globalServerGroup = GroupConfiguration.empty(GLOBAL_SERVER_GROUP_NAME);
    GroupConfiguration globalProxyGroup = GroupConfiguration.empty(GLOBAL_PROXY_GROUP_NAME);
    globalProxyGroup.getTargetEnvironments().add(proxyEnvironment);
    globalServerGroup.getTargetEnvironments().add(serverEnvironment);

    if (installProxy) {
      this.createDefaultTask(proxyEnvironment, PROXY_TASK_NAME, null, 256);
    }

    if (installServer) {
      this.createDefaultTask(serverEnvironment, LOBBY_TASK_NAME, lobbyJavaCommand, 512);
    }

    if (proxyVersion != null) {
      this.installGlobalTemplate(null, globalProxyGroup, "proxy", proxyVersion.getFirst(), proxyVersion.getSecond());
    }

    if (serverVersion != null) {
      this.installGlobalTemplate(lobbyJavaCommand, globalServerGroup, "server", serverVersion.getFirst(),
        serverVersion.getSecond());
    }

    CloudNet.getInstance().getGroupConfigurationProvider().addGroupConfiguration(globalServerGroup);
    CloudNet.getInstance().getGroupConfigurationProvider().addGroupConfiguration(globalProxyGroup);
  }

  @Override
  public boolean shouldAsk(boolean configFileAvailable) {
    NodeServiceTaskProvider taskProvider = (NodeServiceTaskProvider) CloudNet.getInstance().getServiceTaskProvider();
    NodeGroupConfigurationProvider groupProvider = (NodeGroupConfigurationProvider) CloudNet.getInstance()
      .getGroupConfigurationProvider();
    return false; //return !taskProvider.isFileCreated() && !groupProvider.isFileCreated();
  }

  private void installGlobalTemplate(
    String javaCommand, GroupConfiguration group, String name,
    ServiceVersionType type, ServiceVersion version
  ) {
    ServiceTemplate globalTemplate = ServiceTemplate.local(GLOBAL_TEMPLATE_PREFIX, name);
    group.getTemplates().add(globalTemplate);

    try {
      TemplateStorageUtil
        .createAndPrepareTemplate(globalTemplate.storage(), type.getTargetEnvironment().getEnvironmentType());
    } catch (IOException exception) {
      LOGGER.severe("Exception while creating templates", exception);
    }

    //CloudNet.getInstance().getServiceVersionProvider()
    //  .installServiceVersion(javaCommand, type, version, globalTemplate);
  }

  private void createDefaultTask(ServiceEnvironmentType environment, String taskName, String javaCommand,
    int maxHeapMemorySize) {
    ServiceTask serviceTask = ServiceTask.builder()
      .templates(Collections.singletonList(new ServiceTemplate(taskName, "default", "local")))
      .name(taskName)
      .autoDeleteOnStop(true)
      .javaCommand(javaCommand)
      .groups(Collections.singletonList(taskName))
      .serviceEnvironmentType(environment)
      .maxHeapMemory(maxHeapMemorySize)
      .startPort(environment.getDefaultStartPort())
      .minServiceCount(1)
      .build();

    for (ServiceTemplate template : serviceTask.getTemplates()) {
      try {
        TemplateStorageUtil.createAndPrepareTemplate(template.storage(), environment);
      } catch (IOException exception) {
        LOGGER.severe("Exception while creating templates", exception);
      }
    }
    CloudNet.getInstance().getServiceTaskProvider().addPermanentServiceTask(serviceTask);
    CloudNet.getInstance().getGroupConfigurationProvider().addGroupConfiguration(GroupConfiguration.empty(taskName));
  }

  @Override
  public void applyQuestions(ConsoleSetupAnimation animation) {
    /*
    animation.addEntry(new QuestionListEntry<>(
      "proxyEnvironment",
      LanguageManager.getMessage("cloudnet-init-setup-tasks-proxy-environment"),
      new QuestionAnswerTypeEnum<ServiceEnvironmentType>(ServiceEnvironmentType.class) {
        @Override
        protected ServiceEnvironmentType[] values() {
          return Arrays.stream(super.values()).filter(ServiceEnvironmentType::isMinecraftProxy)
            .toArray(ServiceEnvironmentType[]::new);
        }
      }
    ));
    animation.addEntry(new QuestionListEntry<>(
      "proxyVersion",
      LanguageManager.getMessage("cloudnet-init-setup-tasks-proxy-version"),
      new QuestionAnswerTypeServiceVersion(() -> (ServiceEnvironmentType) animation.getResult("proxyEnvironment"),
        CloudNet.getInstance().getServiceVersionProvider())
    ));

    animation.addEntry(new QuestionListEntry<>(
      "serverEnvironment",
      LanguageManager.getMessage("cloudnet-init-setup-tasks-server-environment"),
      new QuestionAnswerTypeEnum<ServiceEnvironmentType>(ServiceEnvironmentType.class) {
        @Override
        protected ServiceEnvironmentType[] values() {
          return Arrays.stream(super.values()).filter(ServiceEnvironmentType::isMinecraftServer)
            .toArray(ServiceEnvironmentType[]::new);
        }
      }
    ));

    animation.addEntry(new QuestionListEntry<>(
      "javaCommand",
      LanguageManager.getMessage("cloudnet-init-setup-tasks-server-javacommand"),
      SubCommandArgumentTypes.functional(
        "java",
        LanguageManager.getMessage("cloudnet-init-setup-tasks-server-javacommand-invalid"),
        input -> {
          JavaVersion version = JavaVersionResolver.resolveFromJavaExecutable(input);
          return version == null ? null : new Pair<>(input.equals("java") ? null : input, version);
        },
        Collections.emptyList()
      )
    ));

    animation.addEntry(new QuestionListEntry<>(
      "serverVersion",
      LanguageManager.getMessage("cloudnet-init-setup-tasks-server-version"),
      new QuestionAnswerTypeServiceVersion(
        () -> (ServiceEnvironmentType) animation.getResult("serverEnvironment"),
        CloudNet.getInstance().getServiceVersionProvider(),
        () -> {
          //noinspection unchecked
          Pair<String, JavaVersion> pair = (Pair<String, JavaVersion>) animation.getResult("javaCommand");
          return pair.getSecond();
        }
      )
    ));

    animation.addEntry(new QuestionListEntry<>(
      "installProxy",
      LanguageManager.getMessage("cloudnet-init-setup-tasks-should-install-proxy"),
      new QuestionAnswerTypeBoolean() {
        @Override
        public String getRecommendation() {
          return super.getTrueString();
        }
      }
    ));
    animation.addEntry(new QuestionListEntry<>(
      "installServer",
      LanguageManager.getMessage("cloudnet-init-setup-tasks-should-install-server"),
      new QuestionAnswerTypeBoolean() {
        @Override
        public String getRecommendation() {
          return super.getTrueString();
        }
      }
    ));

    this.shouldExecute = true;

     */
  }

}
