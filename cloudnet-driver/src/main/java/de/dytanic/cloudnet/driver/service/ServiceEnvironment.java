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

package de.dytanic.cloudnet.driver.service;

import java.util.Arrays;

public enum ServiceEnvironment {

  //Minecraft Server
  MINECRAFT_SERVER_DEFAULT("minecraft"),
  MINECRAFT_SERVER_SPIGOT("spigot"),
  MINECRAFT_SERVER_PAPER_SPIGOT("paper"),
  @Deprecated MINECRAFT_SERVER_TUINITY_SPIGOT("tuinity"),
  @Deprecated MINECRAFT_SERVER_FORGE("forge"),
  MINECRAFT_SERVER_SPONGE_VANILLA("spongevanilla"),
  @Deprecated MINECRAFT_SERVER_AKARIN("akarin"),
  @Deprecated MINECRAFT_SERVER_TACO("taco"),
  //GlowStone
  GLOWSTONE_DEFAULT("glowstone"),
  //BungeeCord
  BUNGEECORD_DEFAULT("bungee"),
  BUNGEECORD_WATERFALL("waterfall"),
  @Deprecated BUNGEECORD_TRAVERTINE("travertine"),
  BUNGEECORD_HEXACORD("hexacord"),
  //Waterdog
  WATERDOG_PE("waterdog-pe"),
  @Deprecated WATERDOG_DEFAULT("waterdog"),
  //Nukkit
  NUKKIT_DEFAULT("nukkit"),
  //GoMint
  @Deprecated GO_MINT_DEFAULT("gomint"),
  //Velocity
  VELOCITY_DEFAULT("velocity");

  private final String name;

  ServiceEnvironment(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

  public ServiceEnvironmentType getEnvironmentType() {
    return Arrays.stream(ServiceEnvironmentType.values())
      .filter(serviceEnvironmentType -> Arrays.asList(serviceEnvironmentType.getEnvironments()).contains(this))
      .findFirst()
      .orElse(null);
  }
}
