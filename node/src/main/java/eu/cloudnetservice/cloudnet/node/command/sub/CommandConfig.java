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

package eu.cloudnetservice.cloudnet.node.command.sub;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.specifier.Range;
import cloud.commandframework.context.CommandContext;
import com.google.common.net.InetAddresses;
import eu.cloudnetservice.cloudnet.common.JavaVersion;
import eu.cloudnetservice.cloudnet.common.collection.Pair;
import eu.cloudnetservice.cloudnet.common.language.I18n;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.command.annotation.Description;
import eu.cloudnetservice.cloudnet.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.cloudnet.node.command.source.CommandSource;
import eu.cloudnetservice.cloudnet.node.config.Configuration;
import java.util.Queue;
import lombok.NonNull;

@Description("")
@CommandPermission("cloudnet.command.config")
public final class CommandConfig {

  @Parser(name = "ip")
  public String ipParser(CommandContext<CommandSource> $, Queue<String> input) {
    var address = input.remove();

    if (!InetAddresses.isInetAddress(address)) {
      throw new ArgumentNotAvailableException(I18n.trans("command-config-node-ip-invalid"));
    }

    return address;
  }

  @CommandMethod("config reload")
  public void reloadConfigs(CommandSource source) {
    this.updateNodeConfig(CloudNet.instance().config().load());
    CloudNet.instance().serviceTaskProvider().reload();
    CloudNet.instance().groupConfigurationProvider().reload();
    CloudNet.instance().permissionManagement().reload();
    source.sendMessage(I18n.trans("command-node-reload-config-success"));
  }

  @CommandMethod("config node reload")
  public void reloadNodeConfig(CommandSource source) {
    this.updateNodeConfig(CloudNet.instance().config().load());
    source.sendMessage(I18n.trans("command-config-node-reload-config"));
  }

  @CommandMethod("config node add ip <ip>")
  public void addIpWhitelist(CommandSource source, @Argument(value = "ip", parserName = "ip") String ip) {
    var ipWhitelist = this.nodeConfig().ipWhitelist();
    // check if the collection changes after we add the ip
    if (ipWhitelist.add(ip)) {
      // update the config as we have a change
      this.updateNodeConfig();
    }
    source.sendMessage(I18n.trans("command-config-node-add-ip-whitelist", ip));
  }

  @CommandMethod("config node remove ip <ip>")
  public void removeIpWhitelist(CommandSource source, @Argument(value = "ip") String ip) {
    var ipWhitelist = this.nodeConfig().ipWhitelist();
    // check if the collection changes after we remove the given ip
    if (ipWhitelist.remove(ip)) {
      // update the config as we have a change
      this.updateNodeConfig();
    }
    source.sendMessage(I18n.trans("command-config-node-remove-ip-whitelist", ip));
  }

  @CommandMethod("config node set maxMemory <maxMemory>")
  public void setMaxMemory(CommandSource source, @Argument("maxMemory") @Range(min = "0") int maxMemory) {
    this.nodeConfig().maxMemory(maxMemory);
    this.updateNodeConfig();
    source.sendMessage(I18n.trans("command-config-node-max-memory-set", maxMemory));
  }

  @CommandMethod("config node set javaCommand <executable>")
  public void setJavaCommand(
    CommandSource source,
    @Argument(value = "executable", parserName = "javaCommand") Pair<String, JavaVersion> executable
  ) {
    this.nodeConfig().javaCommand(executable.first());
    this.updateNodeConfig();
    source.sendMessage(I18n.trans("command-config-node-set-java-command",
      executable.first(),
      executable.second().name()));
  }

  private @NonNull Configuration nodeConfig() {
    return CloudNet.instance().config();
  }

  private void updateNodeConfig() {
    this.updateNodeConfig(CloudNet.instance().config());
  }

  private void updateNodeConfig(@NonNull Configuration configuration) {
    CloudNet.instance().config(configuration.save());
  }
}
