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

import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.impl.console.Console;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;

@Singleton
@Permission("cloudnet.command.clear")
@Description("command-clear-description")
public final class ClearCommand {

  @Command("clear")
  public void clearConsole(@NonNull Console console) {
    console.clearScreen();
  }
}
