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

package de.dytanic.cloudnet.command;

import de.dytanic.cloudnet.common.Properties;

/**
 * This interface represents the handling a input commandLine as args
 */
public interface ICommandExecutor {

  /**
   * Handles an incoming commandLine which are already split and fetched for all extra properties
   *
   * @param sender      the sender, that execute the command
   * @param command     the command name, that is used for
   * @param args        all important arguments
   * @param commandLine the full commandline from the sender
   * @param properties  all properties, that are parsed from the command line
   */
  void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties);
}
