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

package de.dytanic.cloudnet.command.sub;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.specifier.Greedy;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.arguments.StaticArgument;
import cloud.commandframework.context.CommandContext;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.exception.ArgumentNotAvailableException;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceConfigurationBase;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

public class CommandServiceConfiguration {

  private static final StaticArgument<CommandSource> TASK_ARGUMENT = StaticArgument.of("tasks");

  public static void applyServiceConfigurationDisplay(Collection<String> messages,
    ServiceConfigurationBase configurationBase) {
    messages.add("Includes:");

    for (ServiceRemoteInclusion inclusion : configurationBase.getIncludes()) {
      messages.add("- " + inclusion.getUrl() + " => " + inclusion.getDestination());
    }

    messages.add(" ");
    messages.add("Templates:");

    for (ServiceTemplate template : configurationBase.getTemplates()) {
      messages.add("- " + template);
    }

    messages.add(" ");
    messages.add("Deployments:");

    for (ServiceDeployment deployment : configurationBase.getDeployments()) {
      messages.add("- ");
      messages.add(
        "Template:  " + deployment.getTemplate());
      messages.add("Excludes: " + deployment.getExcludes());
    }

    messages.add(" ");
    messages.add("JVM Options:");

    for (String jvmOption : configurationBase.getJvmOptions()) {
      messages.add("- " + jvmOption);
    }

    messages.add(" ");
    messages.add("Process Parameters:");

    for (String processParameters : configurationBase.getProcessParameters()) {
      messages.add("- " + processParameters);
    }

    messages.add("Properties: ");

    messages.addAll(Arrays.asList(configurationBase.getProperties().toPrettyJson().split("\n")));
    messages.add(" ");
  }

  @Parser(suggestions = "dualServiceConfiguration")
  public ServiceConfigurationBase dualServiceConfigurationParser(CommandContext<CommandSource> context,
    Queue<String> input) {
    String name = input.remove();

    ServiceConfigurationBase configurationBase;
    boolean checkForTask = context.getArgumentTimings().containsKey(TASK_ARGUMENT);
    if (checkForTask) {
      configurationBase = CloudNet.getInstance().getServiceTaskProvider().getServiceTask(name);
    } else {
      configurationBase = CloudNet.getInstance().getGroupConfigurationProvider().getGroupConfiguration(name);
    }

    if (configurationBase == null) {
      if (checkForTask) {
        throw new ArgumentNotAvailableException(LanguageManager.getMessage("command-tasks-task-not-found"));
      } else {
        throw new ArgumentNotAvailableException(LanguageManager.getMessage("command-service-base-group-not-found"));
      }
    }

    return configurationBase;
  }

  @Suggestions("dualServiceConfiguration")
  public List<String> suggestServiceConfiguration(CommandContext<CommandSource> context, String input) {
    if (context.getRawInputJoined().toLowerCase().startsWith("tasks")) {
      return CloudNet.getInstance().getServiceTaskProvider().getPermanentServiceTasks()
        .stream()
        .map(INameable::getName)
        .collect(Collectors.toList());
    }
    return CloudNet.getInstance().getGroupConfigurationProvider().getGroupConfigurations()
      .stream()
      .map(INameable::getName)
      .collect(Collectors.toList());
  }

  @Parser
  public Queue<String> greedyParameterParser(CommandContext<CommandSource> context, Queue<String> input) {
    return input;
  }

  @CommandMethod("tasks1|groups1 task|group <name> add deployment <deployment>")
  public void addDeployment(
    CommandSource source,
    @Argument("name") ServiceConfigurationBase configurationBase,
    @Argument("deployment") ServiceTemplate template
  ) {
    ServiceDeployment deployment = new ServiceDeployment(template, new ArrayList<>());

    configurationBase.getDeployments().add(deployment);
    this.updateConfigurationBase(configurationBase);
  }

  @CommandMethod("tasks1|groups1 task|group <name> add template <template>")
  public void addTemplate(
    CommandSource source,
    @Argument("name") ServiceConfigurationBase configurationBase,
    @Argument("template") ServiceTemplate template
  ) {
    configurationBase.getTemplates().add(template);
    this.updateConfigurationBase(configurationBase);
  }

  @CommandMethod("tasks1|groups1 task|group <name> add inclusion <url> <path>")
  public void addInclusion(
    CommandSource source,
    @Argument("name") ServiceConfigurationBase configurationBase,
    @Argument("url") String url,
    @Argument("path") String path
  ) {
    ServiceRemoteInclusion inclusion = new ServiceRemoteInclusion(url, path);

    configurationBase.getIncludes().add(inclusion);
    this.updateConfigurationBase(configurationBase);
  }

  @CommandMethod("tasks1|groups1 task|group <name> add jvmOption <options>")
  public void addJvmOption(
    CommandSource source,
    @Argument("name") ServiceConfigurationBase configurationBase,
    @Argument("options") Queue<String> jvmOptions
  ) {
    for (String jvmOption : jvmOptions) {
      configurationBase.getJvmOptions().add(jvmOption);
    }
    this.updateConfigurationBase(configurationBase);
  }

  @CommandMethod("tasks1|groups1 task|group <name> add processParameter <options>")
  public void addProcessParameter(
    CommandSource source,
    @Argument("name") ServiceConfigurationBase configurationBase,
    @Greedy @Argument("options") Queue<String> processParameters
  ) {
    for (String processParameter : processParameters) {
      configurationBase.getProcessParameters().add(processParameter);
    }
    this.updateConfigurationBase(configurationBase);
  }

  @CommandMethod("tasks1|groups1 task|group <name> remove deployment <deployment>")
  public void removeDeployment(
    CommandSource source,
    @Argument("name") ServiceConfigurationBase configurationBase,
    @Argument("deployment") ServiceTemplate template
  ) {
    ServiceDeployment deployment = new ServiceDeployment(template, new ArrayList<>());

    configurationBase.getDeployments().remove(deployment);
    this.updateConfigurationBase(configurationBase);
  }

  @CommandMethod("tasks1|groups1 task|group <name> remove template <template>")
  public void removeTemplate(
    CommandSource source,
    @Argument("name") ServiceConfigurationBase configurationBase,
    @Argument("template") ServiceTemplate template
  ) {
    configurationBase.getTemplates().remove(template);
    this.updateConfigurationBase(configurationBase);
  }

  @CommandMethod("tasks1|groups1 task|group <name> remove inclusion <url> <path>")
  public void removeInclusion(
    CommandSource source,
    @Argument("name") ServiceConfigurationBase configurationBase,
    @Argument("url") String url,
    @Argument("path") String path
  ) {
    ServiceRemoteInclusion inclusion = new ServiceRemoteInclusion(url, path);

    configurationBase.getIncludes().remove(inclusion);
    this.updateConfigurationBase(configurationBase);
  }

  @CommandMethod("tasks1|groups1 task|group <name> remove jvmOption <options>")
  public void removeJvmOption(
    CommandSource source,
    @Argument("name") ServiceConfigurationBase configurationBase,
    @Greedy @Argument("options") Queue<String> jvmOptions
  ) {
    for (String jvmOption : jvmOptions) {
      configurationBase.getJvmOptions().remove(jvmOption);
    }
    this.updateConfigurationBase(configurationBase);
  }

  @CommandMethod("tasks1|groups1 task|group <name> remove processParameter <options>")
  public void removeProcessParameter(
    CommandSource source,
    @Argument("name") ServiceConfigurationBase configurationBase,
    @Greedy @Argument("options") Queue<String> processParameters
  ) {
    for (String processParameter : processParameters) {
      configurationBase.getProcessParameters().remove(processParameter);
    }
    this.updateConfigurationBase(configurationBase);
  }

  @CommandMethod("tasks1|groups1 task|group <name> clear jvmOptions")
  public void clearJvmOptions(CommandSource source, @Argument("name") ServiceConfigurationBase configurationBase) {
    configurationBase.getJvmOptions().clear();
    this.updateConfigurationBase(configurationBase);
  }

  protected void updateConfigurationBase(ServiceConfigurationBase configurationBase) {
    if (configurationBase instanceof ServiceTask) {
      CloudNet.getInstance().getServiceTaskProvider().addPermanentServiceTask((ServiceTask) configurationBase);
    } else {
      CloudNet.getInstance().getGroupConfigurationProvider()
        .addGroupConfiguration((GroupConfiguration) configurationBase);
    }
  }

}
