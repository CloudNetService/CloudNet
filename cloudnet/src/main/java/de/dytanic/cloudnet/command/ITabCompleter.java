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
import java.util.Collection;

/**
 * A interface, for all commands, an additional completer for that.
 *
 * @see Command
 */
public interface ITabCompleter {

  /**
   * This method allows on a Command implementation to complete the tab requests from the console or a supported command
   * sender
   *
   * @param commandLine the commandLine, that is currently written
   * @param args        the command line split into arguments
   * @param properties  the parsed properties from the command line
   * @return all available results. It does not necessarily depend on the actual input, which is already given
   */
  Collection<String> complete(String commandLine, String[] args, Properties properties);
}
