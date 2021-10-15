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

import java.util.Collection;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

/**
 * The commandInfo class allows to easily serialize the command information
 */
@ToString
@EqualsAndHashCode
public class CommandInfo {

  protected String name;

  /**
   * The configured names by the command
   */
  protected Collection<String> aliases;

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
  protected List<String> usage;

  public CommandInfo(
    String name,
    Collection<String> aliases,
    String permission,
    String description,
    List<String> usage
  ) {
    this.name = name;
    this.aliases = aliases;
    this.permission = permission;
    this.description = description;
    this.usage = usage;
  }

  public String getName() {
    return this.name;
  }

  public Collection<String> getAliases() {
    return this.aliases;
  }

  public String getPermission() {
    return this.permission;
  }

  public String getDescription() {
    return this.description;
  }

  public List<String> getUsage() {
    return this.usage;
  }

  public String joinNameToAliases(@NotNull String separator) {
    String result = this.name;
    if (!this.aliases.isEmpty()) {
      result += separator + String.join(separator, this.aliases);
    }

    return result;
  }
}
