package de.dytanic.cloudnet.driver.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ServiceEnvironmentType {

    MINECRAFT_SERVER(new ServiceEnvironment[]{
        ServiceEnvironment.MINECRAFT_SERVER_MODPACK,
        ServiceEnvironment.MINECRAFT_SERVER_CAULDRON,
        ServiceEnvironment.MINECRAFT_SERVER_FORGE,
        ServiceEnvironment.MINECRAFT_SERVER_SPONGE_FORGE,
        ServiceEnvironment.MINECRAFT_SERVER_SPONGE_VANILLA,
        ServiceEnvironment.MINECRAFT_SERVER_SPONGE,
        ServiceEnvironment.MINECRAFT_SERVER_TORCH,
        ServiceEnvironment.MINECRAFT_SERVER_HOSE,
        ServiceEnvironment.MINECRAFT_SERVER_TACO,
        ServiceEnvironment.MINECRAFT_SERVER_CRAFTBUKKIT,
        ServiceEnvironment.MINECRAFT_SERVER_SPIGOT,
        ServiceEnvironment.MINECRAFT_SERVER_AKARIN,
        ServiceEnvironment.MINECRAFT_SERVER_DEFAULT
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
    PROX_PROX(new ServiceEnvironment[]{
        ServiceEnvironment.PROX_PROX_DEFAULT
    }, false, true, false, false);

    private final ServiceEnvironment[] environments;

    private final boolean minecraftJavaProxy, minecraftBedrockProxy, minecraftJavaServer, minecraftBedrockServer;

}