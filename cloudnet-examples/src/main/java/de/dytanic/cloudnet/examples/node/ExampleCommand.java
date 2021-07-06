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

package de.dytanic.cloudnet.examples.node;

import de.dytanic.cloudnet.command.Command;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;

public final class ExampleCommand extends Command {

  public ExampleCommand() {
    super("example", "exm");

    this.permission = "cloudnet.command.example";
    this.usage = "example <test>";
    this.prefix = "cloudnet-example-module";
    this.description = "This is an example command";
  }

  @Override
  public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
    if (args.length == 0) {
      sender.sendMessage("example <test>");
      return;
    }

    //exm test-1 get=my_argument
    if (args[0].equalsIgnoreCase("test-1")) {
      sender.sendMessage(
        "Starting Test-1 with the following" + (properties.containsKey("get") ? " default argument: " + properties
          .get("get") : " no arguments"));
      sender.sendMessage(
        "Argument is " + properties.get("get")
      );
    }
  }
}
