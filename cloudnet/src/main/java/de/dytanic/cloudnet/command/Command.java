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

import de.dytanic.cloudnet.driver.command.CommandInfo;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Represents a command that should be execute
 */
@ToString
@EqualsAndHashCode
public abstract class Command implements ICommandExecutor {

  protected String[] names;

  protected String permission;

  protected String description;

  protected String usage;

  protected String prefix;

  public Command(String... names) {
    this.names = names;
  }

  public Command(String[] names, String permission) {
    this.names = names;
    this.permission = permission;
  }

  public Command(String[] names, String permission, String description) {
    this.names = names;
    this.permission = permission;
    this.description = description;
  }

  public Command(String[] names, String permission, String description, String usage, String prefix) {
    this.names = names;
    this.permission = permission;
    this.description = description;
    this.usage = usage;
    this.prefix = prefix;
  }

  public Command() {
  }

  public CommandInfo getInfo() {
    return new CommandInfo(this.names, this.permission, this.description, this.usage);
  }

  public final boolean isValid() {
    return this.names != null && this.names.length > 0 && this.names[0] != null && !this.names[0].isEmpty();
  }

  public String[] getNames() {
    return this.names;
  }

  public String getPermission() {
    return this.permission;
  }

  public String getDescription() {
    return this.description;
  }

  public String getUsage() {
    return this.usage;
  }

  public String getPrefix() {
    return this.prefix;
  }

}
