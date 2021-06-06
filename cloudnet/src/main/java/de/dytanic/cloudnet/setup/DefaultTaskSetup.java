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
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.questionlist.ConsoleQuestionListAnimation;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionListEntry;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeBoolean;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeEnum;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeServiceVersion;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.driver.service.ProcessConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.provider.NodeGroupConfigurationProvider;
import de.dytanic.cloudnet.provider.NodeServiceTaskProvider;
import de.dytanic.cloudnet.service.EmptyGroupConfiguration;
import de.dytanic.cloudnet.template.TemplateStorageUtil;
import de.dytanic.cloudnet.template.install.ServiceVersion;
import de.dytanic.cloudnet.template.install.ServiceVersionType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class DefaultTaskSetup implements DefaultSetup {

  private static final String GLOBAL_TEMPLATE_PREFIX = "Global";
  private static final String GLOBAL_PROXY_GROUP_NAME = "Global-Proxy";
  private static final String GLOBAL_SERVER_GROUP_NAME = "Global-Server";
  private static final String PROXY_TASK_NAME = "Proxy";
  private static final String LOBBY_TASK_NAME = "Lobby";

  private boolean shouldExecute = false;

  @Override
  public void postExecute(ConsoleQuestionListAnimation animation) {
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

    GroupConfiguration globalServerGroup = new EmptyGroupConfiguration(GLOBAL_SERVER_GROUP_NAME);
    GroupConfiguration globalProxyGroup = new EmptyGroupConfiguration(GLOBAL_PROXY_GROUP_NAME);
    globalProxyGroup.getTargetEnvironments().add(proxyEnvironment);
    globalServerGroup.getTargetEnvironments().add(serverEnvironment);

    if (installProxy) {
      this.createDefaultTask(proxyEnvironment, PROXY_TASK_NAME, 256);
    }

    if (installServer) {
      this.createDefaultTask(serverEnvironment, LOBBY_TASK_NAME, 512);
    }

    if (proxyVersion != null) {
      this.installGlobalTemplate(globalProxyGroup, "proxy", proxyVersion.getFirst(), proxyVersion.getSecond());
    }
    if (serverVersion != null) {
      this.installGlobalTemplate(globalServerGroup, "server", serverVersion.getFirst(), serverVersion.getSecond());
    }

    CloudNet.getInstance().getGroupConfigurationProvider().addGroupConfiguration(globalServerGroup);
    CloudNet.getInstance().getGroupConfigurationProvider().addGroupConfiguration(globalProxyGroup);
  }

  @Override
  public boolean shouldAsk(boolean configFileAvailable) {
    NodeServiceTaskProvider taskProvider = (NodeServiceTaskProvider) CloudNet.getInstance().getServiceTaskProvider();
    NodeGroupConfigurationProvider groupProvider = (NodeGroupConfigurationProvider) CloudNet.getInstance()
      .getGroupConfigurationProvider();
    return !taskProvider.isFileCreated() && !groupProvider.isFileCreated();
  }

  private void installGlobalTemplate(GroupConfiguration globalGroup, String name, ServiceVersionType versionType,
    ServiceVersion version) {
    ServiceTemplate globalTemplate = ServiceTemplate.local(GLOBAL_TEMPLATE_PREFIX, name);
    globalGroup.getTemplates().add(globalTemplate);

    try {
      TemplateStorageUtil
        .createAndPrepareTemplate(globalTemplate, versionType.getTargetEnvironment().getEnvironmentType());
    } catch (IOException exception) {
      exception.printStackTrace();
    }

    CloudNet.getInstance().getServiceVersionProvider().installServiceVersion(versionType, version, globalTemplate);
  }

  private void createDefaultTask(ServiceEnvironmentType environment, String taskName, int maxHeapMemorySize) {
    ServiceTask serviceTask = new ServiceTask(
      new ArrayList<>(),
      new ArrayList<>(Collections.singletonList(new ServiceTemplate(taskName, "default", "local"))),
      new ArrayList<>(),
      taskName,
      "jvm",
      false,
      true,
      false,
      new ArrayList<>(),
      new ArrayList<>(Collections.singletonList(taskName)),
      new ArrayList<>(),
      new ProcessConfiguration(
        environment,
        maxHeapMemorySize,
        new ArrayList<>(),
        new ArrayList<>()
      ),
      environment.getDefaultStartPort(),
      1
    );

    for (ServiceTemplate template : serviceTask.getTemplates()) {
      try {
        TemplateStorageUtil.createAndPrepareTemplate(template, environment);
      } catch (IOException exception) {
        exception.printStackTrace();
      }
    }
    CloudNet.getInstance().getServiceTaskProvider().addPermanentServiceTask(serviceTask);
    CloudNet.getInstance().getGroupConfigurationProvider().addGroupConfiguration(new EmptyGroupConfiguration(taskName));
  }

  @Override
  public void applyQuestions(ConsoleQuestionListAnimation animation) {
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
      "serverVersion",
      LanguageManager.getMessage("cloudnet-init-setup-tasks-server-version"),
      new QuestionAnswerTypeServiceVersion(() -> (ServiceEnvironmentType) animation.getResult("serverEnvironment"),
        CloudNet.getInstance().getServiceVersionProvider())
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
  }

}
