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

package de.dytanic.cloudnet.command.sub;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.specifier.Range;
import cloud.commandframework.context.CommandContext;
import com.google.common.net.InetAddresses;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.annotation.Description;
import de.dytanic.cloudnet.command.exception.ArgumentNotAvailableException;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.common.JavaVersion;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.config.IConfiguration;
import de.dytanic.cloudnet.config.IConfiguration.DefaultJVMFlags;
import de.dytanic.cloudnet.config.JsonConfiguration;
import java.util.Collection;
import java.util.Queue;

@Description("")
@CommandPermission("cloudnet.command.config")
public final class CommandConfig {

  @Parser(name = "ip")
  public String ipParser(CommandContext<CommandSource> $, Queue<String> input) {
    var address = input.remove();

    if (!InetAddresses.isInetAddress(address)) {
      throw new ArgumentNotAvailableException(I18n.trans("command-node-ip-invalid"));
    }

    return address;
  }

  @CommandMethod("config reload")
  public void reloadConfigs(CommandSource source) {
    this.updateNodeConfig(JsonConfiguration.loadFromFile(CloudNet.getInstance()));
    CloudNet.getInstance().getServiceTaskProvider().reload();
    CloudNet.getInstance().getGroupConfigurationProvider().reload();
    CloudNet.getInstance().getPermissionManagement().reload();
    source.sendMessage(I18n.trans("command-reload-reload-config-success"));
  }

  @CommandMethod("config node reload")
  public void reloadNodeConfig(CommandSource source) {
    this.updateNodeConfig(JsonConfiguration.loadFromFile(CloudNet.getInstance()));
    source.sendMessage(I18n.trans("command-reload-node-config"));
  }

  @CommandMethod("config node add ip <ip>")
  public void addIpWhitelist(CommandSource source, @Argument(value = "ip", parserName = "ip") String ip) {
    var ipWhitelist = this.nodeConfig().getIpWhitelist();
    // check if the collection changes after we add the ip
    if (ipWhitelist.add(ip)) {
      // update the config as we have a change
      this.updateNodeConfig();
      source.sendMessage(I18n.trans("command-node-add-ip-whitelist").replace("%ip%", ip));
    } else {
      source.sendMessage(I18n.trans("command-node-ip-already-whitelisted"));
    }
  }

  @CommandMethod("config node remove ip <ip>")
  public void removeIpWhitelist(CommandSource source, @Argument(value = "ip") String ip) {
    var ipWhitelist = this.nodeConfig().getIpWhitelist();
    // check if the collection changes after we remove the given ip
    if (ipWhitelist.remove(ip)) {
      // update the config as we have a change
      this.updateNodeConfig();
      source.sendMessage(I18n.trans("command-node-remove-ip-whitelist").replace("%ip%", ip));
    } else {
      source.sendMessage(I18n.trans("command-node-ip-not-whitelisted"));
    }
  }

  @CommandMethod("config node set maxMemory <maxMemory>")
  public void setMaxMemory(CommandSource source, @Argument("maxMemory") @Range(min = "0") int maxMemory) {
    this.nodeConfig().setMaxMemory(maxMemory);
    this.updateNodeConfig();
    source.sendMessage(I18n.trans("command-node-max-memory-set")
      .replace("%memory%", Integer.toString(maxMemory)));
  }

  @CommandMethod("config node set javaCommand <executable>")
  public void setJavaCommand(
    CommandSource source,
    @Argument(value = "executable", parserName = "javaCommand") Pair<String, JavaVersion> executable
  ) {
    this.nodeConfig().setJVMCommand(executable.getFirst());
    this.updateNodeConfig();
    source.sendMessage(I18n.trans("command-node-set-java-command")
      .replace("%executable%", executable.getFirst())
      .replace("%ver%", executable.getSecond().getName()));
  }

  @CommandMethod("config node set defaultJVMFlags <flag>")
  public void setDefaultJVMFlags(CommandSource source, @Argument("flag") DefaultJVMFlags flags) {
    this.nodeConfig().setDefaultJVMFlags(flags);
    this.updateNodeConfig();
    source.sendMessage(I18n.trans("command-node-set-default-flags").replace("%flags%", flags.name()));
  }

  private IConfiguration nodeConfig() {
    return CloudNet.getInstance().getConfig();
  }

  private void updateNodeConfig() {
    this.updateNodeConfig(CloudNet.getInstance().getConfig());
  }

  private void updateNodeConfig(IConfiguration configuration) {
    CloudNet.getInstance().setConfig(configuration);
    configuration.save();
  }
}
