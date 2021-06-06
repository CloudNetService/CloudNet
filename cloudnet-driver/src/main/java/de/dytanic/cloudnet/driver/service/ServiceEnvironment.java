package de.dytanic.cloudnet.driver.service;

import java.util.Arrays;

public enum ServiceEnvironment {

  //Minecraft Server
  MINECRAFT_SERVER_DEFAULT("minecraft"),
  MINECRAFT_SERVER_SPIGOT("spigot"),
  MINECRAFT_SERVER_PAPER_SPIGOT("paper"),
  MINECRAFT_SERVER_TUINITY_SPIGOT("tuinity"),
  MINECRAFT_SERVER_FORGE("forge"),
  MINECRAFT_SERVER_SPONGE_VANILLA("spongevanilla"),
  MINECRAFT_SERVER_AKARIN("akarin"),
  MINECRAFT_SERVER_TACO("taco"),
  //GlowStone
  GLOWSTONE_DEFAULT("glowstone"),
  //BungeeCord
  BUNGEECORD_DEFAULT("bungee"),
  BUNGEECORD_WATERFALL("waterfall"),
  BUNGEECORD_TRAVERTINE("travertine"),
  BUNGEECORD_HEXACORD("hexacord"),
  //Waterdog
  WATERDOG_DEFAULT("waterdog"),
  //Nukkit
  NUKKIT_DEFAULT("nukkit"),
  //GoMint
  GO_MINT_DEFAULT("gomint"),
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
