package de.dytanic.cloudnet.driver.service;

import java.util.Arrays;
import java.util.Collection;

public enum ServiceEnvironmentType {

    MINECRAFT_SERVER(
            new ServiceEnvironment[]{
                    ServiceEnvironment.MINECRAFT_SERVER_FORGE,
                    ServiceEnvironment.MINECRAFT_SERVER_SPONGE_VANILLA,
                    ServiceEnvironment.MINECRAFT_SERVER_TACO,
                    ServiceEnvironment.MINECRAFT_SERVER_PAPER_SPIGOT,
                    ServiceEnvironment.MINECRAFT_SERVER_SPIGOT,
                    ServiceEnvironment.MINECRAFT_SERVER_AKARIN,
                    ServiceEnvironment.MINECRAFT_SERVER_DEFAULT,
            },
            false, false, true, false,
            44955
    ),
    GLOWSTONE(new ServiceEnvironment[]{ServiceEnvironment.GLOWSTONE_DEFAULT},
            false, false, true, false,
            44955
    ),
    NUKKIT(
            new ServiceEnvironment[]{ServiceEnvironment.NUKKIT_DEFAULT},
            false, false, false, true,
            44955
    ),
    BUNGEECORD(
            new ServiceEnvironment[]{
                    ServiceEnvironment.BUNGEECORD_HEXACORD,
                    ServiceEnvironment.BUNGEECORD_TRAVERTINE,
                    ServiceEnvironment.BUNGEECORD_WATERFALL,
                    ServiceEnvironment.BUNGEECORD_DEFAULT
            },
            true, false, false, false,
            25565,
            new String[]{">"}
    ),
    VELOCITY(
            new ServiceEnvironment[]{ServiceEnvironment.VELOCITY_DEFAULT},
            true, false, false, false,
            25565
    ),
    WATERDOG(
            new ServiceEnvironment[]{ServiceEnvironment.WATERDOG_DEFAULT},
            false, true, false, false,
            19132
    );

    private final ServiceEnvironment[] environments;

    private final boolean minecraftJavaProxy;
    private final boolean minecraftBedrockProxy;
    private final boolean minecraftJavaServer;
    private final boolean minecraftBedrockServer;

    private final int defaultStartPort;
    private final Collection<String> ignoredConsoleLines;

    ServiceEnvironmentType(ServiceEnvironment[] environments, boolean minecraftJavaProxy, boolean minecraftBedrockProxy, boolean minecraftJavaServer, boolean minecraftBedrockServer, int defaultStartPort) {
        this(environments, minecraftJavaProxy, minecraftBedrockProxy, minecraftJavaServer, minecraftBedrockServer, defaultStartPort, new String[0]);
    }

    ServiceEnvironmentType(ServiceEnvironment[] environments, boolean minecraftJavaProxy, boolean minecraftBedrockProxy, boolean minecraftJavaServer, boolean minecraftBedrockServer, int defaultStartPort, String[] ignoredConsoleLines) {
        this.environments = environments;
        this.minecraftJavaProxy = minecraftJavaProxy;
        this.minecraftBedrockProxy = minecraftBedrockProxy;
        this.minecraftJavaServer = minecraftJavaServer;
        this.minecraftBedrockServer = minecraftBedrockServer;
        this.defaultStartPort = defaultStartPort;
        this.ignoredConsoleLines = Arrays.asList(ignoredConsoleLines);
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

    public boolean isMinecraftProxy() {
        return this.isMinecraftJavaProxy() || this.isMinecraftBedrockProxy();
    }

    public boolean isMinecraftServer() {
        return this.isMinecraftJavaServer() || this.isMinecraftBedrockServer();
    }

    public int getDefaultStartPort() {
        return this.defaultStartPort;
    }

    public Collection<String> getIgnoredConsoleLines() {
        return this.ignoredConsoleLines;
    }
}