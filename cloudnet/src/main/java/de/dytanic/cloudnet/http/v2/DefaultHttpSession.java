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

package de.dytanic.cloudnet.http.v2;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class DefaultHttpSession implements HttpSession {

  protected final String uniqueId;
  protected final UUID userId;
  protected final AtomicLong expireTime;
  protected final Map<String, Object> properties;

  public DefaultHttpSession(long expireTime, UUID userId) {
    this(expireTime, UUID.randomUUID().toString(), userId);
  }

  public DefaultHttpSession(long expireTime, String uniqueId, UUID userId) {
    this(expireTime, uniqueId, userId, new ConcurrentHashMap<>());
  }

  public DefaultHttpSession(long expireTime, String uniqueId, UUID userId, Map<String, Object> properties) {
    this.expireTime = new AtomicLong(expireTime);
    this.uniqueId = uniqueId;
    this.userId = userId;
    this.properties = properties;
  }

  @Override
  public long getExpireTime() {
    return this.expireTime.get();
  }

  @Override
  public long refreshFor(long liveMillis) {
    return this.expireTime.addAndGet(liveMillis);
  }

  @Override
  public @NotNull String getUniqueId() {
    return this.uniqueId;
  }

  @Override
  public @NotNull UUID getUserId() {
    return this.userId;
  }

  @Override
  public IPermissionUser getUser() {
    return CloudNetDriver.getInstance().getPermissionManagement().getUser(this.userId);
  }

  @Override
  public <T> T getProperty(@NotNull String key) {
    return this.getProperty(key, null);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getProperty(@NotNull String key, @Nullable T def) {
    return (T) this.properties.getOrDefault(key, def);
  }

  @Override
  public @NotNull HttpSession setProperty(@NotNull String key, @NotNull Object value) {
    this.properties.put(key, value);
    return this;
  }

  @Override
  public @NotNull HttpSession removeProperty(@NotNull String key) {
    this.properties.remove(key);
    return this;
  }

  @Override
  public boolean hasProperty(@NotNull String key) {
    return this.properties.containsKey(key);
  }

  @Override
  public @NotNull Map<String, Object> getProperties() {
    return this.properties;
  }
}
