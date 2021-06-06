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

import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;

public class SubCommandBuilder {

  private final Deque<QuestionAnswerType<?>> prefixes = new LinkedBlockingDeque<>();
  private final Deque<SubCommandExecutor> preCommandExecutors = new LinkedBlockingDeque<>();
  private final Deque<SubCommandExecutor> postCommandExecutors = new LinkedBlockingDeque<>();
  private final Collection<SubCommand> subCommands = new ArrayList<>();

  public static SubCommandBuilder create() {
    return new SubCommandBuilder();
  }

  public SubCommandBuilder applyHandler(Consumer<SubCommandBuilder> consumer) {
    consumer.accept(this);
    return this;
  }

  public SubCommandBuilder generateCommand(SubCommandExecutor executor, QuestionAnswerType<?>... types) {
    return this.generateCommand(executor, null, types);
  }

  public SubCommandBuilder generateCommand(SubCommandExecutor executor, Consumer<SubCommand> commandModifier,
    QuestionAnswerType<?>... types) {
    Collection<QuestionAnswerType<?>> allTypes = new ArrayList<>(this.prefixes);
    allTypes.addAll(Arrays.asList(types));
    Collection<SubCommandExecutor> preCommandExecutors = new ArrayList<>(this.preCommandExecutors);
    Collection<SubCommandExecutor> postCommandExecutors = new ArrayList<>(this.postCommandExecutors);
    SubCommand subCommand = new SubCommand(allTypes.toArray(new QuestionAnswerType<?>[0])) {
      @Override
      public void execute(SubCommand subCommand, ICommandSender sender, String command, SubCommandArgumentWrapper args,
        String commandLine, Properties properties, Map<String, Object> internalProperties) {
        try {
          for (SubCommandExecutor preCommandExecutor : preCommandExecutors) {
            preCommandExecutor.execute(subCommand, sender, command, args, commandLine, properties, internalProperties);
          }
          executor.execute(subCommand, sender, command, args, commandLine, properties, internalProperties);
          for (SubCommandExecutor postCommandExecutor : postCommandExecutors) {
            postCommandExecutor.execute(subCommand, sender, command, args, commandLine, properties, internalProperties);
          }
        } catch (CommandInterrupt ignored) {
        }
      }
    };
    if (commandModifier != null) {
      commandModifier.accept(subCommand);
    }
    this.subCommands.add(subCommand);
    return this;
  }

  public SubCommandBuilder prefix(QuestionAnswerType<?> type) {
    this.prefixes.add(type);
    return this;
  }

  public SubCommandBuilder clearAll() {
    this.clearPreHandlers();
    this.clearPostHandlers();
    this.clearPrefixes();
    return this;
  }

  public SubCommandBuilder clearPrefixes() {
    this.prefixes.clear();
    return this;
  }

  public SubCommandBuilder removeLastPrefix() {
    this.prefixes.pollLast();
    return this;
  }

  public SubCommandBuilder preExecute(SubCommandExecutor executor) {
    this.preCommandExecutors.add(executor);
    return this;
  }

  public SubCommandBuilder clearPreHandlers() {
    this.preCommandExecutors.clear();
    return this;
  }

  public SubCommandBuilder removeLastPreHandler() {
    this.preCommandExecutors.pollLast();
    return this;
  }

  public SubCommandBuilder postExecute(SubCommandExecutor executor) {
    this.postCommandExecutors.add(executor);
    return this;
  }

  public SubCommandBuilder clearPostHandlers() {
    this.postCommandExecutors.clear();
    return this;
  }

  public SubCommandBuilder removeLastPostHandler() {
    this.postCommandExecutors.pollLast();
    return this;
  }

  public SubCommandBuilder executeMultipleTimes(int count, Consumer<SubCommandBuilder> consumer) {
    for (int i = 0; i < count; i++) {
      consumer.accept(this);
    }
    return this;
  }

  public Collection<SubCommand> getSubCommands() {
    return this.subCommands;
  }


}
