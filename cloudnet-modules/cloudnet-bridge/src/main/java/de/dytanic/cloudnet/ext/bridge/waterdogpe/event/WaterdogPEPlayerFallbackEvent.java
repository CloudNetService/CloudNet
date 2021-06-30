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

package de.dytanic.cloudnet.ext.bridge.waterdogpe.event;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WaterdogPEPlayerFallbackEvent extends WaterdogPECloudNetEvent {

  private final ProxiedPlayer player;
  private ServiceInfoSnapshot fallback;
  private String fallbackName;

  public WaterdogPEPlayerFallbackEvent(ProxiedPlayer player, ServiceInfoSnapshot fallback, String fallbackName) {
    this.player = player;
    this.fallback = fallback;
    this.fallbackName = fallbackName;
  }

  @NotNull
  public ProxiedPlayer getPlayer() {
    return this.player;
  }

  @Nullable
  public String getFallbackName() {
    return this.fallbackName != null ? this.fallbackName
      : this.fallback != null ? this.fallback.getServiceId().getName() : null;
  }

  @Nullable
  public ServiceInfoSnapshot getFallback() {
    return this.fallback;
  }

  public void setFallback(@Nullable ServiceInfoSnapshot fallback) {
    this.fallback = fallback;
    this.fallbackName = null;
  }

  public void setFallback(@Nullable String name) {
    this.fallbackName = name;
    this.fallback = null;
  }

}
