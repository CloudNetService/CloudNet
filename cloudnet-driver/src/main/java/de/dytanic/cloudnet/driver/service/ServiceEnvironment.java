package de.dytanic.cloudnet.driver.service;

public enum ServiceEnvironment {

    //Minecraft Server
    MINECRAFT_SERVER_DEFAULT("minecraft"),
    MINECRAFT_SERVER_SPIGOT("spigot"),
    MINECRAFT_SERVER_PAPER_SPIGOT("paper"),
    MINECRAFT_SERVER_FORGE("forge"),
    MINECRAFT_SERVER_MODPACK("modpack"),
    MINECRAFT_SERVER_SPONGE("sponge"),
    MINECRAFT_SERVER_SPONGE_VANILLA("spongevanilla"),
    MINECRAFT_SERVER_SPONGE_FORGE("spongeforge"),
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
    VELOCITY_DEFAULT("velocity"),
    //ProxProx
    PROX_PROX_DEFAULT("proxprox");

    private final String name;

    ServiceEnvironment(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}