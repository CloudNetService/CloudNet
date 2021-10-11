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
import cloud.commandframework.arguments.StaticArgument;
import cloud.commandframework.context.CommandContext;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.exception.ArgumentNotAvailableException;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceConfigurationBase;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Queue;

public class CommandServiceConfiguration {

  private static final StaticArgument<CommandSource> TASK_ARGUMENT = StaticArgument.of("tasks");

  @Parser
  public ServiceConfigurationBase dualServiceConfigurationParser(CommandContext<CommandSource> context,
    Queue<String> input) {
    String name = input.remove();

    ServiceConfigurationBase configurationBase;
    if (context.getArgumentTimings().containsKey(TASK_ARGUMENT)) {
      configurationBase = CloudNet.getInstance().getServiceTaskProvider().getServiceTask(name);
    } else {
      configurationBase = CloudNet.getInstance().getGroupConfigurationProvider().getGroupConfiguration(name);
    }

    if (configurationBase == null) {
      throw new ArgumentNotAvailableException("Group / task not found");
    }

    return configurationBase;
  }

  @Parser
  public Queue<String> greedyParameterParser(CommandContext<CommandSource> context, Queue<String> input) {
    return input;
  }

  @CommandMethod("tasks|configurationBases task|configurationBase <name> add deployment <storage:prefix/name>")
  public void addDeployment(
    CommandSource source,
    @Argument("name") ServiceConfigurationBase configurationBase,
    @Argument("storage:prefix/name") ServiceTemplate template
  ) {
    ServiceDeployment deployment = new ServiceDeployment(template, new ArrayList<>());

    configurationBase.getDeployments().add(deployment);
    this.updateConfigurationBase(configurationBase);
  }

  @CommandMethod("tasks|configurationBases task|configurationBase <name> add template <storage:prefix/name>")
  public void addTemplate(
    CommandSource source,
    @Argument("name") ServiceConfigurationBase configurationBase,
    @Argument("storage:prefix/name") ServiceTemplate template
  ) {
    configurationBase.getTemplates().add(template);
    this.updateConfigurationBase(configurationBase);
  }

  @CommandMethod("tasks|configurationBases task|configurationBase <name> add inclusion <url> <path>")
  public void addInclusion(
    CommandSource source,
    @Argument("name") ServiceConfigurationBase configurationBase,
    @Argument("url") String url,
    @Argument("targetPath") String path
  ) {
    ServiceRemoteInclusion inclusion = new ServiceRemoteInclusion(url, path);

    configurationBase.getIncludes().add(inclusion);
    this.updateConfigurationBase(configurationBase);
  }

  @CommandMethod("tasks|configurationBases task|configurationBase <name> add jvmOption <options>")
  public void addJvmOption(
    CommandSource source,
    @Argument("name") ServiceConfigurationBase configurationBase,
    @Greedy @Argument("options") Queue<String> jvmOptions
  ) {
    for (String jvmOption : jvmOptions) {
      configurationBase.getJvmOptions().add(jvmOption);
    }
    this.updateConfigurationBase(configurationBase);
  }

  @CommandMethod("tasks|configurationBases task|configurationBase <name> add processParameter <options>")
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

  @CommandMethod("tasks|configurationBases task|configurationBase <name> remove deployment <storage:prefix/name>")
  public void removeDeployment(
    CommandSource source,
    @Argument("name") ServiceConfigurationBase configurationBase,
    @Argument("storage:prefix/name") ServiceTemplate template
  ) {
    ServiceDeployment deployment = new ServiceDeployment(template, new ArrayList<>());

    configurationBase.getDeployments().remove(deployment);
    this.updateConfigurationBase(configurationBase);
  }

  @CommandMethod("tasks|configurationBases task|configurationBase <name> remove template <storage:prefix/name>")
  public void removeTemplate(
    CommandSource source,
    @Argument("name") ServiceConfigurationBase configurationBase,
    @Argument("storage:prefix/name") ServiceTemplate template
  ) {
    configurationBase.getTemplates().remove(template);
    this.updateConfigurationBase(configurationBase);
  }

  @CommandMethod("tasks|configurationBases task|configurationBase <name> remove inclusion <url> <path>")
  public void removeInclusion(
    CommandSource source,
    @Argument("name") ServiceConfigurationBase configurationBase,
    @Argument("url") String url,
    @Argument("targetPath") String path
  ) {
    ServiceRemoteInclusion inclusion = new ServiceRemoteInclusion(url, path);

    configurationBase.getIncludes().remove(inclusion);
    this.updateConfigurationBase(configurationBase);
  }

  @CommandMethod("tasks|configurationBases task|configurationBase <name> remove jvmOption <options>")
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

  @CommandMethod("tasks|configurationBases task|configurationBase <name> remove processParameter <options>")
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

  @CommandMethod("tasks|configurationBases task|configurationBase <name> clear jvmOptions")
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

}
