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

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyConstants;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode
public class SyncProxyConfiguration {

  public static final Type TYPE = new TypeToken<SyncProxyConfiguration>() {
  }.getType();
  protected Collection<SyncProxyProxyLoginConfiguration> loginConfigurations;
  protected Collection<SyncProxyTabListConfiguration> tabListConfigurations;
  protected Map<String, String> messages;
  private boolean ingameServiceStartStopMessages = true;

  public SyncProxyConfiguration(Collection<SyncProxyProxyLoginConfiguration> loginConfigurations,
    Collection<SyncProxyTabListConfiguration> tabListConfigurations, Map<String, String> messages,
    boolean ingameServiceStartStopMessages) {
    this.loginConfigurations = loginConfigurations;
    this.tabListConfigurations = tabListConfigurations;
    this.messages = messages;
    this.ingameServiceStartStopMessages = ingameServiceStartStopMessages;
  }

  public SyncProxyConfiguration() {
  }

  public static SyncProxyConfiguration getConfigurationFromNode() {
    ChannelMessage response = ChannelMessage.builder()
      .channel(SyncProxyConstants.SYNC_PROXY_CHANNEL_NAME)
      .message(SyncProxyConstants.SYNC_PROXY_CHANNEL_GET_CONFIGURATION)
      .targetNode(Wrapper.getInstance().getServiceId().getNodeUniqueId())
      .build()
      .sendSingleQuery();

    if (response != null) {
      return response.getJson().get("syncProxyConfiguration", SyncProxyConfiguration.TYPE);
    }

    return null;
  }

  public static void updateSyncProxyConfigurationInNetwork(@NotNull SyncProxyConfiguration syncProxyConfiguration) {
    CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
      SyncProxyConstants.SYNC_PROXY_CHANNEL_NAME,
      SyncProxyConstants.SYNC_PROXY_UPDATE_CONFIGURATION,
      new JsonDocument(
        "syncProxyConfiguration",
        syncProxyConfiguration
      )
    );
  }

  public Collection<SyncProxyProxyLoginConfiguration> getLoginConfigurations() {
    return this.loginConfigurations;
  }

  public void setLoginConfigurations(Collection<SyncProxyProxyLoginConfiguration> loginConfigurations) {
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
