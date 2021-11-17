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
import java.util.Collection;
import java.util.Collections;
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

  protected Set<SyncProxyLoginConfiguration> loginConfigurations;
  protected Set<SyncProxyTabListConfiguration> tabListConfigurations;
  protected Map<String, String> messages;
  protected boolean ingameServiceStartStopMessages;

  public SyncProxyConfiguration(Set<SyncProxyLoginConfiguration> loginConfigurations,
    Set<SyncProxyTabListConfiguration> tabListConfigurations, Map<String, String> messages,
    boolean ingameServiceStartStopMessages) {
    this.loginConfigurations = loginConfigurations;
    this.tabListConfigurations = tabListConfigurations;
    this.messages = messages;
    this.ingameServiceStartStopMessages = ingameServiceStartStopMessages;
  }

  public static SyncProxyConfiguration createDefault(@NotNull String targetGroup) {
    return new SyncProxyConfiguration(
      Collections.singleton(SyncProxyLoginConfiguration.createDefault(targetGroup)),
      Collections.singleton(SyncProxyTabListConfiguration.createDefault(targetGroup)),
      DEFAULT_MESSAGES,
      true);
  }

  public static SyncProxyConfiguration getConfigurationFromNode() {
    ChannelMessage response = ChannelMessage.builder()
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

  public Set<SyncProxyLoginConfiguration> getLoginConfigurations() {
    return this.loginConfigurations;
  }

  public void setLoginConfigurations(Set<SyncProxyLoginConfiguration> loginConfigurations) {
    this.loginConfigurations = loginConfigurations;
  }

  public Collection<SyncProxyTabListConfiguration> getTabListConfigurations() {
    return this.tabListConfigurations;
  }

  public void setTabListConfigurations(Set<SyncProxyTabListConfiguration> tabListConfigurations) {
    this.tabListConfigurations = tabListConfigurations;
  }

  public Map<String, String> getMessages() {
    return this.messages;
  }

  public void setMessages(Map<String, String> messages) {
    this.messages = messages;
  }

  public @UnknownNullability String getMessage(@NotNull String key, @Nullable Function<String, String> modifier) {
    String message = this.messages.getOrDefault(key, DEFAULT_MESSAGES.get(key));
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
}
