/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.node.command.sub;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.specifier.Range;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import eu.cloudnetservice.common.JavaVersion;
import eu.cloudnetservice.common.collection.Pair;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.config.JsonConfiguration;
import java.util.List;
import java.util.Queue;
import lombok.NonNull;

@CommandAlias("cfg")
@CommandPermission("cloudnet.command.config")
@Description("command-config-description")
public final class ConfigCommand {

  @Parser(name = "ipAlias")
  public @NonNull String ipAliasParser(@NonNull CommandContext<?> $, @NonNull Queue<String> input) {
    var alias = input.remove();
    if (this.nodeConfig().ipAliases().containsKey(alias)) {
      throw new ArgumentNotAvailableException(I18n.trans("command-config-node-ip-alias-already-existing", alias));
    }

    return alias;
  }

  @Suggestions("ipAlias")
  public @NonNull List<String> ipAliasSuggestions(@NonNull CommandContext<?> $, @NonNull String input) {
    return List.copyOf(this.nodeConfig().ipAliases().keySet());
  }

  @Suggestions("whitelistedIps")
  public @NonNull List<String> suggestWhitelistIps(@NonNull CommandContext<?> $, @NonNull String input) {
    return List.copyOf(this.nodeConfig().ipWhitelist());
  }

  @CommandMethod("config|cfg reload")
  public void reloadConfigs(@NonNull CommandSource source) {
    Node.instance().reloadConfigFrom(JsonConfiguration.loadFromFile(Node.instance()));
    Node.instance().serviceTaskProvider().reload();
    Node.instance().groupConfigurationProvider().reload();
    Node.instance().permissionManagement().reload();
    source.sendMessage(I18n.trans("command-config-reload-config"));
  }

  @CommandMethod("config|cfg node reload")
  public void reloadNodeConfig(@NonNull CommandSource source) {
    Node.instance().reloadConfigFrom(JsonConfiguration.loadFromFile(Node.instance()));
    source.sendMessage(I18n.trans("command-config-node-reload-config"));
  }

  @CommandMethod("config|cfg node add ip <ip>")
  public void addIpWhitelist(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "ip", parserName = "anyHost") String ip
  ) {
    var ipWhitelist = this.nodeConfig().ipWhitelist();
    // check if the collection changes after we add the ip
    if (ipWhitelist.add(ip)) {
      // update the config as we have a change
      this.nodeConfig().save();
    }
    source.sendMessage(I18n.trans("command-config-node-add-ip-whitelist", ip));
  }

  @CommandMethod("config|cfg node remove ip <ip>")
  public void removeIpWhitelist(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "ip", suggestions = "whitelistedIps") String ip
  ) {
    var ipWhitelist = this.nodeConfig().ipWhitelist();
    // check if the collection changes after we remove the given ip
    if (ipWhitelist.remove(ip)) {
      // update the config as we have a change
      this.nodeConfig().save();
    }
    source.sendMessage(I18n.trans("command-config-node-remove-ip-whitelist", ip));
  }

  @CommandMethod("config|cfg node set maxMemory <maxMemory>")
  public void setMaxMemory(@NonNull CommandSource source, @Argument("maxMemory") @Range(min = "0") int maxMemory) {
    this.nodeConfig().maxMemory(maxMemory);
    this.nodeConfig().save();
    source.sendMessage(I18n.trans("command-config-node-set-max-memory", maxMemory));
  }

  @CommandMethod("config|cfg node set javaCommand <executable>")
  public void setJavaCommand(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "executable", parserName = "javaCommand") Pair<String, JavaVersion> executable
  ) {
    this.nodeConfig().javaCommand(executable.first());
    this.nodeConfig().save();
    source.sendMessage(I18n.trans("command-config-node-set-java-command",
      executable.first(),
      executable.second().name()));
  }

  @CommandMethod("config|cfg node add ipalias|ipa <name> <hostAddress>")
  public void addIpAlias(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "name", parserName = "ipAlias") String alias,
    @NonNull @Argument(value = "hostAddress", parserName = "assignableHostAndPort") HostAndPort hostAddress
  ) {
    this.nodeConfig().ipAliases().put(alias, hostAddress.host());
    this.nodeConfig().save();
    source.sendMessage(I18n.trans("command-config-node-ip-alias-added", alias, hostAddress.host()));
  }

  @CommandMethod("config|cfg node remove ipalias|ipa <name>")
  public void removeIpAlias(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "name", suggestions = "ipAlias") String alias
  ) {
    if (this.nodeConfig().ipAliases().remove(alias) != null) {
      this.nodeConfig().save();
    }
    source.sendMessage(I18n.trans("command-config-node-ip-alias-remove", alias));
  }

  private @NonNull Configuration nodeConfig() {
    return Node.instance().config();
  }
}

