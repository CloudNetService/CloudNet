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

package eu.cloudnetservice.node.impl.command.sub;

import eu.cloudnetservice.driver.base.JavaVersion;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.driver.service.ServiceCreateResult;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.node.impl.console.Console;
import eu.cloudnetservice.node.impl.console.animation.progressbar.ConsoleProgressAnimation;
import io.vavr.Tuple2;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.incendo.cloud.annotation.specifier.Range;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
import org.jetbrains.annotations.Nullable;

@Singleton
@Permission("cloudnet.command.create")
@Description("command-create-description")
public final class CreateCommand {

  @Command("create by <task> <amount>")
  public void createByTask(
    @NonNull @Service I18n i18n,
    @NonNull Console console,
    @NonNull CommandSource source,
    @NonNull @Argument("task") ServiceTask task,
    @Argument("amount") @Range(min = "1") int amount,
    @Flag("start") boolean startService,
    @Nullable @Flag("id") Integer id,
    @Nullable @Flag(value = "javaCommand", parserName = "javaCommand") Tuple2<String, JavaVersion> javaCommand,
    @Nullable @Flag("node") String nodeId,
    @Nullable @Flag("memory") Integer memory
  ) {
    var configurationBuilder = ServiceConfiguration.builder(task);
    if (id != null) {
      configurationBuilder.taskId(id);
    }

    if (javaCommand != null) {
      configurationBuilder.javaCommand(javaCommand._1());
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
        i18n,
        console,
        source,
        configurationBuilder.build(),
        ConsoleProgressAnimation.createDefault("Creating", " Services", 1, amount),
        amount,
        startService);
    } else {
      // start without progress animation
      this.startServices(i18n, console, source, configurationBuilder.build(), null, amount, startService);
    }
  }

  private void startServices(
    @NonNull @Service I18n i18n,
    @NonNull Console console,
    @NonNull CommandSource source,
    @NonNull ServiceConfiguration configuration,
    @Nullable ConsoleProgressAnimation animation,
    int amount,
    boolean start
  ) {
    source.sendMessage(i18n.translate("command-create-by-task-starting", configuration.serviceId().taskName(), amount));
    // start the progress animation if needed
    if (animation != null && !console.animationRunning()) {
      console.startAnimation(animation);
    }

    // try to start the provided amount of services based on the configuration
    for (var i = 0; i < amount; i++) {
      var createResult = configuration.createNewService();
      // stop creating new services if the creation failed once
      if (createResult.state() == ServiceCreateResult.State.FAILED) {
        source.sendMessage(i18n.translate("command-create-by-task-failed"));
        // stop the animation
        if (animation != null) {
          animation.stepToEnd();
        }
        return;
      }
      // start the service if requested
      if (createResult.state() == ServiceCreateResult.State.CREATED && start) {
        createResult.serviceInfo().provider().start();
      }
      // step the progress bar by one if given
      if (animation != null) {
        animation.step();
      }
    }
    // print the finish message if no other progress indication is there
    if (animation == null) {
      source.sendMessage(i18n.translate("command-create-by-task-success"));
    }
  }
}
