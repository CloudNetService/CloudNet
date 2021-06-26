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

package de.dytanic.cloudnet.ext.syncproxy.bungee.util;

import com.google.common.base.Preconditions;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerConnectRequest;
import net.md_5.bungee.api.SkinConfiguration;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PermissionCheckEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.score.Scoreboard;

public class LoginProxiedPlayer implements ProxiedPlayer {

  private final PendingConnection connection;

  private final Collection<String> permissions = new ArrayList<>();

  private final Collection<String> groups = new ArrayList<>();

  public LoginProxiedPlayer(PendingConnection connection) {
    this.connection = connection;

    this.groups.addAll(ProxyServer.getInstance().getConfigurationAdapter().getGroups(connection.getName()));

    for (String group : this.groups) {
      for (String permission : ProxyServer.getInstance().getConfigurationAdapter().getPermissions(group)) {
        this.setPermission(permission, true);
      }
    }
  }

  @Override
  public String getDisplayName() {
    return this.connection.getName();
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
  public void connect(ServerInfo target, ServerConnectEvent.Reason reason) {

  }

  @Override
  public void connect(ServerInfo target, Callback<Boolean> callback) {

  }

  @Override
  public void connect(ServerInfo target, Callback<Boolean> callback, ServerConnectEvent.Reason reason) {

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
    return 1337;
  }

  @Override
  public void sendData(String channel, byte[] data) {

  }

  @Override
  public PendingConnection getPendingConnection() {
    return null;
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
  public String getUUID() {
    return this.connection.getUniqueId().toString();
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
    return null;
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
    return this.groups;
  }

  @Override
  public void addGroups(String... groups) {

  }

  @Override
  public void removeGroups(String... groups) {

  }

  @Override
  public boolean hasPermission(String permission) {
    Preconditions.checkNotNull(permission);

    return ProxyServer.getInstance().getPluginManager().callEvent(new PermissionCheckEvent(
      this,
      permission,
      this.permissions.contains(permission.toLowerCase())
    )).hasPermission();
  }

  @Override
  public void setPermission(String permission, boolean value) {
    Preconditions.checkNotNull(permission);

    if (value) {
      this.permissions.add(permission.toLowerCase());
    } else {
      this.permissions.remove(permission.toLowerCase());
    }
  }

  @Override
  public Collection<String> getPermissions() {
    return this.permissions;
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
  public void disconnect(String reason) {

  }

  @Override
  public void disconnect(BaseComponent... reason) {

  }

  @Override
  public void disconnect(BaseComponent reason) {

  }

  @Override
  public boolean isConnected() {
    return true;
  }

  @Override
  public Unsafe unsafe() {
    return null;
  }

}
