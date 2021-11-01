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

package de.dytanic.cloudnet.ext.syncproxy.configuration;

import com.google.common.collect.ImmutableMap;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyConstants;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode
public class SyncProxyConfiguration {

  public static final Map<String, String> DEFAULT_MESSAGES = ImmutableMap.of(
    "player-login-not-whitelisted", "&cThe network is currently in maintenance!",
    "player-login-full-server", "&cThe network is currently full. You need extra permissions to enter the network",
    "service-start", "&7The service &e%service% &7is &astarting &7on node &e%node%&7...",
    "service-stop", "&7The service &e%service% &7is &cstopping &7on node &e%node%&7...");

  protected Collection<SyncProxyLoginConfiguration> loginConfigurations;
  protected Collection<SyncProxyTabListConfiguration> tabListConfigurations;
  protected Map<String, String> messages;
  protected boolean ingameServiceStartStopMessages;

  public SyncProxyConfiguration(Collection<SyncProxyLoginConfiguration> loginConfigurations,
    Collection<SyncProxyTabListConfiguration> tabListConfigurations, Map<String, String> messages,
    boolean ingameServiceStartStopMessages) {
    this.loginConfigurations = loginConfigurations;
    this.tabListConfigurations = tabListConfigurations;
    this.messages = messages;
    this.ingameServiceStartStopMessages = ingameServiceStartStopMessages;
  }

  public static SyncProxyConfiguration createDefault(@NotNull String targetGroup) {
    return new SyncProxyConfiguration(
      Collections.singletonList(SyncProxyLoginConfiguration.createDefaultLoginConfiguration(targetGroup)),
      Collections.singletonList(SyncProxyTabListConfiguration.createDefaultTabListConfiguration(targetGroup)),
      DEFAULT_MESSAGES,
      true);
  }

  public static SyncProxyConfiguration getConfigurationFromNode() {
    ChannelMessage response = ChannelMessage.builder()
      .channel(SyncProxyConstants.SYNC_PROXY_CHANNEL_NAME)
      .message(SyncProxyConstants.SYNC_PROXY_GET_CONFIGURATION)
      .targetNode(Wrapper.getInstance().getServiceId().getNodeUniqueId())
      .build()
      .sendSingleQuery();

    if (response != null) {
      return response.getContent().readObject(SyncProxyConfiguration.class);
    }

    return null;
  }

  public Collection<SyncProxyLoginConfiguration> getLoginConfigurations() {
    return this.loginConfigurations;
  }

  public void setLoginConfigurations(Collection<SyncProxyLoginConfiguration> loginConfigurations) {
    this.loginConfigurations = loginConfigurations;
  }

  public Collection<SyncProxyTabListConfiguration> getTabListConfigurations() {
    return this.tabListConfigurations;
  }

  public void setTabListConfigurations(Collection<SyncProxyTabListConfiguration> tabListConfigurations) {
    this.tabListConfigurations = tabListConfigurations;
  }

  public Map<String, String> getMessages() {
    return this.messages;
  }

  public void setMessages(Map<String, String> messages) {
    this.messages = messages;
  }

  public boolean showIngameServicesStartStopMessages() {
    return this.ingameServiceStartStopMessages;
  }

}
