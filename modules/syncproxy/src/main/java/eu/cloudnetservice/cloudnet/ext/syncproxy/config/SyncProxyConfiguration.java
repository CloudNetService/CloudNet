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

package eu.cloudnetservice.cloudnet.ext.syncproxy.config;

import com.google.common.collect.ImmutableMap;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.wrapper.Wrapper;
import eu.cloudnetservice.cloudnet.ext.syncproxy.SyncProxyConstants;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

@ToString
@EqualsAndHashCode
public class SyncProxyConfiguration {

  public static final Map<String, String> DEFAULT_MESSAGES = ImmutableMap.of(
    "player-login-not-whitelisted", "&cThe network is currently in maintenance!",
    "player-login-full-server", "&cThe network is currently full. You need extra permissions to enter the network",
    "service-start", "&7The service &e%service% &7is &astarting &7on node &e%node%&7...",
    "service-stop", "&7The service &e%service% &7is &cstopping &7on node &e%node%&7...");

  protected final Set<SyncProxyLoginConfiguration> loginConfigurations;
  protected final Set<SyncProxyTabListConfiguration> tabListConfigurations;
  protected final Map<String, String> messages;
  protected final boolean ingameServiceStartStopMessages;

  protected SyncProxyConfiguration(
    @NotNull Set<SyncProxyLoginConfiguration> loginConfigurations,
    @NotNull Set<SyncProxyTabListConfiguration> tabListConfigurations,
    @NotNull Map<String, String> messages,
    boolean ingameServiceStartStopMessages
  ) {
    this.loginConfigurations = loginConfigurations;
    this.tabListConfigurations = tabListConfigurations;
    this.messages = messages;
    this.ingameServiceStartStopMessages = ingameServiceStartStopMessages;
  }

  public static @NotNull Builder builder() {
    return new Builder();
  }

  public static @NotNull Builder builder(@NotNull SyncProxyConfiguration configuration) {
    return builder()
      .loginConfigurations(configuration.getLoginConfigurations())
      .tabListConfigurations(configuration.getTabListConfigurations())
      .messages(configuration.getMessages())
      .ingameStartStopMessages(configuration.showIngameServicesStartStopMessages());
  }

  public static @NotNull SyncProxyConfiguration createDefault(@NotNull String targetGroup) {
    return builder()
      .addLoginConfiguration(SyncProxyLoginConfiguration.createDefault(targetGroup))
      .addTabListConfiguration(SyncProxyTabListConfiguration.createDefault(targetGroup))
      .messages(DEFAULT_MESSAGES)
      .ingameStartStopMessages(true)
      .build();
  }

  public static @Nullable SyncProxyConfiguration getConfigurationFromNode() {
    var response = ChannelMessage.builder()
      .channel(SyncProxyConstants.SYNC_PROXY_CHANNEL)
      .message(SyncProxyConstants.SYNC_PROXY_CONFIG_REQUEST)
      .targetNode(Wrapper.getInstance().getServiceId().getNodeUniqueId())
      .build()
      .sendSingleQuery();

    if (response != null) {
      return response.getContent().readObject(SyncProxyConfiguration.class);
    }

    return null;
  }

  public @NotNull Set<SyncProxyLoginConfiguration> getLoginConfigurations() {
    return this.loginConfigurations;
  }

  public @NotNull Set<SyncProxyTabListConfiguration> getTabListConfigurations() {
    return this.tabListConfigurations;
  }

  public @NotNull Map<String, String> getMessages() {
    return this.messages;
  }

  public @UnknownNullability String getMessage(@NotNull String key, @Nullable Function<String, String> modifier) {
    var message = this.messages.getOrDefault(key, DEFAULT_MESSAGES.get(key));
    if (message != null && modifier != null) {
      message = modifier.apply(message);
    }

    return message;
  }

  public boolean showIngameServicesStartStopMessages() {
    return this.ingameServiceStartStopMessages;
  }

  public void sendUpdate() {
    ChannelMessage.builder()
      .channel(SyncProxyConstants.SYNC_PROXY_CHANNEL)
      .message(SyncProxyConstants.SYNC_PROXY_UPDATE_CONFIG)
      .targetAll()
      .buffer(DataBuf.empty().writeObject(this))
      .build()
      .send();
  }

  public static class Builder {

    private Set<SyncProxyLoginConfiguration> loginConfigurations = new HashSet<>();
    private Set<SyncProxyTabListConfiguration> tabListConfigurations = new HashSet<>();
    private Map<String, String> messages = new HashMap<>();
    private boolean ingameServiceStartStopMessages;

    public @NotNull Builder loginConfigurations(@NotNull Set<SyncProxyLoginConfiguration> configurations) {
      this.loginConfigurations = new HashSet<>(configurations);
      return this;
    }

    public @NotNull Builder addLoginConfiguration(@NotNull SyncProxyLoginConfiguration configuration) {
      this.loginConfigurations.add(configuration);
      return this;
    }

    public @NotNull Builder tabListConfigurations(@NotNull Set<SyncProxyTabListConfiguration> configurations) {
      this.tabListConfigurations = new HashSet<>(configurations);
      return this;
    }

    public @NotNull Builder addTabListConfiguration(@NotNull SyncProxyTabListConfiguration configuration) {
      this.tabListConfigurations.add(configuration);
      return this;
    }

    public @NotNull Builder messages(@NotNull Map<String, String> messages) {
      this.messages = new HashMap<>(messages);
      return this;
    }

    public @NotNull Builder addMessage(@NotNull String key, @NotNull String message) {
      this.messages.put(key, message);
      return this;
    }

    public @NotNull Builder ingameStartStopMessages(boolean ingameMessages) {
      this.ingameServiceStartStopMessages = ingameMessages;
      return this;
    }

    public @NotNull SyncProxyConfiguration build() {
      return new SyncProxyConfiguration(this.loginConfigurations,
        this.tabListConfigurations,
        this.messages,
        this.ingameServiceStartStopMessages);
    }
  }
}
