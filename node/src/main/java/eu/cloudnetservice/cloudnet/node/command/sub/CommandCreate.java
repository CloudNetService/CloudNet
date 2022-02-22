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

package eu.cloudnetservice.cloudnet.node.command.sub;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.specifier.Range;
import eu.cloudnetservice.cloudnet.common.JavaVersion;
import eu.cloudnetservice.cloudnet.common.collection.Pair;
import eu.cloudnetservice.cloudnet.common.language.I18n;
import eu.cloudnetservice.cloudnet.driver.service.ServiceConfiguration;
import eu.cloudnetservice.cloudnet.driver.service.ServiceTask;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.command.annotation.Description;
import eu.cloudnetservice.cloudnet.node.command.source.CommandSource;
import eu.cloudnetservice.cloudnet.node.console.animation.progressbar.ConsoleProgressAnimation;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@CommandPermission("cloudnet.command.create")
@Description("Creates one or more new services based on a task or completely independent")
public final class CommandCreate {

  @CommandMethod("create by <task> <amount>")
  public void createByTask(
    @NonNull CommandSource source,
    @NonNull @Argument("task") ServiceTask task,
    @Argument("amount") @Range(min = "1") int amount,
    @Flag("start") boolean startService,
    @Flag("id") Integer id,
    @Flag(value = "javaCommand", parserName = "javaCommand") Pair<String, JavaVersion> javaCommand,
    @Flag("node") String nodeId,
    @Flag("memory") Integer memory
  ) {
    var configurationBuilder = ServiceConfiguration.builder(task);
    if (id != null) {
      configurationBuilder.taskId(id);
    }

    if (javaCommand != null) {
      configurationBuilder.javaCommand(javaCommand.first());
    }

    if (nodeId != null) {
      configurationBuilder.node(nodeId);
    }

    if (memory != null) {
      configurationBuilder.maxHeapMemory(memory);
    }

    if (amount >= 10) {
      // display with progress animation
      this.startServices(
        source,
        configurationBuilder.build(),
        ConsoleProgressAnimation.createDefault("Creating", " Services", 1, amount),
        amount,
        startService);
    } else {
      // start without progress animation
      this.startServices(source, configurationBuilder.build(), null, amount, startService);
    }
  }

  private void startServices(
    @NonNull CommandSource source,
    @NonNull ServiceConfiguration configuration,
    @Nullable ConsoleProgressAnimation animation,
    int amount,
    boolean start
  ) {
    source.sendMessage(I18n.trans("command-create-by-task-starting", configuration.serviceId().taskName(), amount));
    // start the progress animation if needed
    if (animation != null && !CloudNet.instance().console().animationRunning()) {
      CloudNet.instance().console().startAnimation(animation);
    }
    // try to start the provided amount of services based on the configuration
    for (var i = 0; i < amount; i++) {
      var service = configuration.createNewService();
      // stop creating new services if the creation failed once
      if (service == null) {
        source.sendMessage(I18n.trans("command-create-by-task-failed"));
        // stop the animation
        if (animation != null) {
          animation.stepToEnd();
        }
        return;
      }
      // start the service if requested
      if (start) {
        service.provider().start();
      }
      // step the progress bar by one if given
      if (animation != null) {
        animation.step();
      }
    }
    // print the finish message if no other progress indication is there
    if (animation == null) {
      source.sendMessage(I18n.trans("command-create-by-task-success"));
    }
  }
}
