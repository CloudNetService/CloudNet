/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.bridge.platform.bungeecord;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.NonNull;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ServerConnectRequest;
import net.md_5.bungee.api.SkinConfiguration;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PermissionCheckEvent;
import net.md_5.bungee.api.event.ServerConnectEvent.Reason;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.api.score.Scoreboard;

public final class PendingConnectionProxiedPlayer implements ProxiedPlayer {

  private final PluginManager pluginManager;
  private final PendingConnection connection;

  public PendingConnectionProxiedPlayer(@NonNull PluginManager pluginManager, @NonNull PendingConnection connection) {
    this.pluginManager = pluginManager;
    this.connection = connection;
  }

  @Override
  public String getDisplayName() {
    return null;
  }

  @Override
  public void setDisplayName(String name) {
  }

  @Override
  public void sendMessage(ChatMessageType position, BaseComponent... message) {
  }

  @Override
  public void sendMessage(ChatMessageType position, BaseComponent message) {
  }

  @Override
  public void sendMessage(UUID sender, BaseComponent... message) {
  }

  @Override
  public void sendMessage(UUID sender, BaseComponent message) {
  }

  @Override
  public void connect(ServerInfo target) {
  }

  @Override
  public void connect(ServerInfo target, Reason reason) {
  }

  @Override
  public void connect(ServerInfo target, Callback<Boolean> callback) {
  }

  @Override
  public void connect(ServerInfo target, Callback<Boolean> callback, Reason reason) {
  }

  @Override
  public void connect(ServerConnectRequest request) {
  }

  @Override
  public Server getServer() {
    return null;
  }

  @Override
  public int getPing() {
    return 0;
  }

  @Override
  public void sendData(String channel, byte[] data) {
  }

  @Override
  public PendingConnection getPendingConnection() {
    return this.connection;
  }

  @Override
  public void chat(String message) {
  }

  @Override
  public ServerInfo getReconnectServer() {
    return null;
  }

  @Override
  public void setReconnectServer(ServerInfo server) {
  }

  @Override
  @Deprecated
  public String getUUID() {
    return this.connection.getUUID();
  }

  @Override
  public UUID getUniqueId() {
    return this.connection.getUniqueId();
  }

  @Override
  public Locale getLocale() {
    return null;
  }

  @Override
  public byte getViewDistance() {
    return 0;
  }

  @Override
  public ChatMode getChatMode() {
    return null;
  }

  @Override
  public boolean hasChatColors() {
    return false;
  }

  @Override
  public SkinConfiguration getSkinParts() {
    return null;
  }

  @Override
  public MainHand getMainHand() {
    return null;
  }

  @Override
  public void setTabHeader(BaseComponent header, BaseComponent footer) {
  }

  @Override
  public void setTabHeader(BaseComponent[] header, BaseComponent[] footer) {
  }

  @Override
  public void resetTabHeader() {
  }

  @Override
  public void sendTitle(Title title) {
  }

  @Override
  public boolean isForgeUser() {
    return false;
  }

  @Override
  public Map<String, String> getModList() {
    return Collections.emptyMap();
  }

  @Override
  public Scoreboard getScoreboard() {
    return null;
  }

  @Override
  public String getName() {
    return this.connection.getName();
  }

  @Override
  public void sendMessage(String message) {
  }

  @Override
  public void sendMessages(String... messages) {
  }

  @Override
  public void sendMessage(BaseComponent... message) {
  }

  @Override
  public void sendMessage(BaseComponent message) {
  }

  @Override
  public Collection<String> getGroups() {
    return Collections.emptyList();
  }

  @Override
  public void addGroups(String... groups) {
  }

  @Override
  public void removeGroups(String... groups) {
  }

  @Override
  public boolean hasPermission(String permission) {
    return this.pluginManager
      .callEvent(new PermissionCheckEvent(this, permission, false))
      .hasPermission();
  }

  @Override
  public void setPermission(String permission, boolean value) {
  }

  @Override
  public Collection<String> getPermissions() {
    return Collections.emptyList();
  }

  @Override
  @Deprecated
  public InetSocketAddress getAddress() {
    return this.connection.getAddress();
  }

  @Override
  public SocketAddress getSocketAddress() {
    return this.connection.getSocketAddress();
  }

  @Override
  @Deprecated
  public void disconnect(String reason) {
    this.connection.disconnect(reason);
  }

  @Override
  public void disconnect(BaseComponent... reason) {
    this.connection.disconnect(reason);
  }

  @Override
  public void disconnect(BaseComponent reason) {
    this.connection.disconnect(reason);
  }

  @Override
  public boolean isConnected() {
    return this.connection.isConnected();
  }

  @Override
  public Unsafe unsafe() {
    return this.connection.unsafe();
  }
}
