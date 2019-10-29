package de.dytanic.cloudnet.driver.service;

public enum ServiceEnvironmentType {

    MINECRAFT_SERVER(new ServiceEnvironment[]{
            ServiceEnvironment.MINECRAFT_SERVER_MODPACK,
            ServiceEnvironment.MINECRAFT_SERVER_FORGE,
            ServiceEnvironment.MINECRAFT_SERVER_SPONGE_FORGE,
            ServiceEnvironment.MINECRAFT_SERVER_SPONGE_VANILLA,
            ServiceEnvironment.MINECRAFT_SERVER_SPONGE,
            ServiceEnvironment.MINECRAFT_SERVER_TACO,
            ServiceEnvironment.MINECRAFT_SERVER_PAPER_SPIGOT,
            ServiceEnvironment.MINECRAFT_SERVER_SPIGOT,
            ServiceEnvironment.MINECRAFT_SERVER_AKARIN,
            ServiceEnvironment.MINECRAFT_SERVER_DEFAULT,
    }, false, false, true, false),
    GLOWSTONE(new ServiceEnvironment[]{
            ServiceEnvironment.GLOWSTONE_DEFAULT
    }, false, false, true, false),
    NUKKIT(new ServiceEnvironment[]{
            ServiceEnvironment.NUKKIT_DEFAULT
    }, false, false, false, true),
    GO_MINT(new ServiceEnvironment[]{
            ServiceEnvironment.GO_MINT_DEFAULT
    }, false, false, false, true),
    BUNGEECORD(new ServiceEnvironment[]{
            ServiceEnvironment.BUNGEECORD_HEXACORD,
            ServiceEnvironment.BUNGEECORD_TRAVERTINE,
            ServiceEnvironment.BUNGEECORD_WATERFALL,
            ServiceEnvironment.BUNGEECORD_DEFAULT
    }, true, false, false, false),
    VELOCITY(new ServiceEnvironment[]{
            ServiceEnvironment.VELOCITY_DEFAULT
    }, true, false, false, false),
    WATERDOG(new ServiceEnvironment[]{
            ServiceEnvironment.WATERDOG_DEFAULT
    }, false, true, false, false),
    PROX_PROX(new ServiceEnvironment[]{
            ServiceEnvironment.PROX_PROX_DEFAULT
    }, false, true, false, false);

    private final ServiceEnvironment[] environments;

    private final boolean minecraftJavaProxy, minecraftBedrockProxy, minecraftJavaServer, minecraftBedrockServer;

    ServiceEnvironmentType(ServiceEnvironment[] environments, boolean minecraftJavaProxy, boolean minecraftBedrockProxy, boolean minecraftJavaServer, boolean minecraftBedrockServer) {
        this.environments = environments;
        this.minecraftJavaProxy = minecraftJavaProxy;
        this.minecraftBedrockProxy = minecraftBedrockProxy;
        this.minecraftJavaServer = minecraftJavaServer;
        this.minecraftBedrockServer = minecraftBedrockServer;
    }

    public ServiceEnvironment[] getEnvironments() {
        return this.environments;
    }

    public boolean isMinecraftJavaProxy() {
        return this.minecraftJavaProxy;
    }

    public boolean isMinecraftBedrockProxy() {
        return this.minecraftBedrockProxy;
    }

    public boolean isMinecraftJavaServer() {
        return this.minecraftJavaServer;
    }

    public boolean isMinecraftBedrockServer() {
        return this.minecraftBedrockServer;
    }
}