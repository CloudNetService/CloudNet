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

import de.dytanic.cloudnet.common.INameable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

/**
 * The commandInfo class allows to easily serialize the command information
 */
@ToString
public class CommandInfo implements INameable {

  protected final String name;

  /**
   * The configured names by the command
   */
  protected final Collection<String> aliases;

  /**
   * The permission, that is configured by this command, that the command sender should has
   */
  protected final String permission;

  /**
   * The command description with a basic description
   */
  protected final String description;

  /**
   * The easiest and important usage for the command
   */
  protected final List<String> usage;

  public CommandInfo(
    @NotNull String name,
    @NotNull Collection<String> aliases,
    @NotNull String permission,
    @NotNull String description,
    @NotNull List<String> usage
  ) {
    this.name = name.toLowerCase();
    this.aliases = aliases;
    this.permission = permission;
    this.description = description;
    this.usage = usage;
  }

  public static CommandInfo empty(@NotNull String name) {
    return new CommandInfo(name, Collections.emptyList(), "", "", Collections.emptyList());
  }

  @Override
  public @NotNull String getName() {
    return this.name;
  }

  public @NotNull Collection<String> getAliases() {
    return this.aliases;
  }

  public @NotNull String getPermission() {
    return this.permission;
  }

  public @NotNull String getDescription() {
    return this.description;
  }

  public @NotNull List<String> getUsage() {
    return this.usage;
  }

  public @NotNull String joinNameToAliases(@NotNull String separator) {
    var result = this.name;
    if (!this.aliases.isEmpty()) {
      result += separator + String.join(separator, this.aliases);
    }

    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CommandInfo that)) {
      return false;
    }
    return this.name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.name);
  }
}
