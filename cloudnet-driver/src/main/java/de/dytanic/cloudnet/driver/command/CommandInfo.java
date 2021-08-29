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

package de.dytanic.cloudnet.driver.command;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * The commandInfo class allows to easy serialize the command information
 */
@ToString
@EqualsAndHashCode
public class CommandInfo {

  /**
   * The configured names by the command
   */
  protected String[] names;

  /**
   * The permission, that is configured by this command, that the command sender should has
   */
  protected String permission;

  /**
   * The command description with a basic description
   */
  protected String description;

  /**
   * The easiest and important usage for the command
   */
  protected String usage;

  public CommandInfo(String[] names, String permission, String description, String usage) {
    this.names = names;
    this.permission = permission;
    this.description = description;
    this.usage = usage;
  }

  public CommandInfo() {
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
}
