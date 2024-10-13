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

package eu.cloudnetservice.node.command.sub;

import eu.cloudnetservice.common.jvm.JavaVersion;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.common.tuple.Tuple2;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.provider.GroupConfigurationProvider;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.config.JsonConfiguration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import lombok.NonNull;
import org.incendo.cloud.annotation.specifier.Range;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.parser.Parser;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandInput;

@Singleton
@CommandAlias("cfg")
@Permission("cloudnet.command.config")
@Description("command-config-description")
public final class ConfigCommand {

  private final Configuration configuration;
  private final ServiceTaskProvider taskProvider;
  private final GroupConfigurationProvider groupProvider;

  @Inject
  public ConfigCommand(
    @NonNull Configuration configuration,
    @NonNull ServiceTaskProvider taskProvider,
    @NonNull GroupConfigurationProvider groupProvider
  ) {
    this.configuration = configuration;
    this.taskProvider = taskProvider;
    this.groupProvider = groupProvider;
  }

  @Parser(name = "ipAlias")
  public @NonNull String ipAliasParser(@NonNull CommandInput input) {
    var alias = input.readString();
    if (this.configuration.ipAliases().containsKey(alias)) {
      throw new ArgumentNotAvailableException(I18n.trans("command-config-node-ip-alias-already-existing", alias));
    }

    return alias;
  }

  @Suggestions("ipAlias")
  public @NonNull List<String> ipAliasSuggestions() {
    return List.copyOf(this.configuration.ipAliases().keySet());
  }

  @Suggestions("whitelistedIps")
  public @NonNull List<String> suggestWhitelistIps() {
    return List.copyOf(this.configuration.ipWhitelist());
  }

  @Command("config|cfg reload")
  public void reloadConfigs(@NonNull CommandSource source) {
    this.configuration.reloadFrom(JsonConfiguration.loadFromFile());
    this.taskProvider.reload();
    this.groupProvider.reload();
    source.sendMessage(I18n.trans("command-config-reload-config"));
  }

  @Command("config|cfg node reload")
  public void reloadNodeConfig(@NonNull CommandSource source) {
    this.configuration.reloadFrom(JsonConfiguration.loadFromFile());
    source.sendMessage(I18n.trans("command-config-node-reload-config"));
  }

  @Command("config|cfg node add ip <ip>")
  public void addIpWhitelist(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "ip", parserName = "anyHost") String ip
  ) {
    var ipWhitelist = this.configuration.ipWhitelist();
    // check if the collection changes after we add the ip
    if (ipWhitelist.add(ip)) {
      // update the config as we have a change
      this.configuration.save();
    }
    source.sendMessage(I18n.trans("command-config-node-add-ip-whitelist", ip));
  }

  @Command("config|cfg node remove ip <ip>")
  public void removeIpWhitelist(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "ip", suggestions = "whitelistedIps") String ip
  ) {
    var ipWhitelist = this.configuration.ipWhitelist();
    // check if the collection changes after we remove the given ip
    if (ipWhitelist.remove(ip)) {
      // update the config as we have a change
      this.configuration.save();
    }
    source.sendMessage(I18n.trans("command-config-node-remove-ip-whitelist", ip));
  }

  @Command("config|cfg node set maxMemory <maxMemory>")
  public void setMaxMemory(@NonNull CommandSource source, @Argument("maxMemory") @Range(min = "0") int maxMemory) {
    this.configuration.maxMemory(maxMemory);
    this.configuration.save();
    source.sendMessage(I18n.trans("command-config-node-set-max-memory", maxMemory));
  }

  @Command("config|cfg node set javaCommand <executable>")
  public void setJavaCommand(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "executable", parserName = "javaCommand") Tuple2<String, JavaVersion> executable
  ) {
    this.configuration.javaCommand(executable.first());
    this.configuration.save();
    source.sendMessage(I18n.trans("command-config-node-set-java-command",
      executable.first(),
      executable.second().name()));
  }

  @Command("config|cfg node add ipalias|ipa <name> <hostAddress>")
  public void addIpAlias(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "name", parserName = "ipAlias") String alias,
    @NonNull @Argument(value = "hostAddress", parserName = "assignableHostAndPort") HostAndPort hostAddress
  ) {
    this.configuration.ipAliases().put(alias, hostAddress.host());
    this.configuration.save();
    source.sendMessage(I18n.trans("command-config-node-ip-alias-added", alias, hostAddress.host()));
  }

  @Command("config|cfg node remove ipalias|ipa <name>")
  public void removeIpAlias(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "name", suggestions = "ipAlias") String alias
  ) {
    if (this.configuration.ipAliases().remove(alias) != null) {
      this.configuration.save();
    }
    source.sendMessage(I18n.trans("command-config-node-ip-alias-remove", alias));
  }
}

