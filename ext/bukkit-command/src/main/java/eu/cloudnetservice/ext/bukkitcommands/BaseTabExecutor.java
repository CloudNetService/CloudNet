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

package eu.cloudnetservice.ext.bukkitcommands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.NonNull;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.util.StringUtil;

public abstract class BaseTabExecutor implements TabExecutor {

  @Override
  public final List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    List<String> completions = new ArrayList<>();
    // copy everything that could match from the last started argument to the target completion list
    StringUtil.copyPartialMatches(args[args.length - 1], this.tabComplete(sender, args), completions);
    return completions;
  }

  protected abstract @NonNull Collection<String> tabComplete(@NonNull CommandSender sender, String @NonNull [] args);
}
