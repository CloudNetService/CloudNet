package de.dytanic.cloudnet.driver.service;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

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
            MinecraftServiceType.JAVA_SERVER,
            44955
    ),
    GLOWSTONE(new ServiceEnvironment[]{ServiceEnvironment.GLOWSTONE_DEFAULT},
            MinecraftServiceType.JAVA_SERVER,
            44955
    ),
    NUKKIT(
            new ServiceEnvironment[]{ServiceEnvironment.NUKKIT_DEFAULT},
            MinecraftServiceType.BEDROCK_SERVER,
            44955
    ),
    GO_MINT(
            new ServiceEnvironment[]{ServiceEnvironment.GO_MINT_DEFAULT},
            MinecraftServiceType.BEDROCK_SERVER,
            44955
    ) {
        @Override
        public String getMainClass(@Nullable Path applicationFile) {
            return "io.gomint.server.Bootstrap";
        }

        @Override
        public @NotNull String getClasspath(@NotNull Path wrapperFile, @Nullable Path applicationFile) {
            return wrapperFile.toAbsolutePath() + File.pathSeparator + "modules/*";
        }
    },
    BUNGEECORD(
            new ServiceEnvironment[]{
                    ServiceEnvironment.BUNGEECORD_HEXACORD,
                    ServiceEnvironment.BUNGEECORD_TRAVERTINE,
                    ServiceEnvironment.BUNGEECORD_WATERFALL,
                    ServiceEnvironment.BUNGEECORD_DEFAULT
            },
            MinecraftServiceType.JAVA_PROXY,
            25565,
            new String[]{">"}
    ),
    VELOCITY(
            new ServiceEnvironment[]{ServiceEnvironment.VELOCITY_DEFAULT},
            MinecraftServiceType.JAVA_PROXY,
            25565
    ),
    WATERDOG(
            new ServiceEnvironment[]{ServiceEnvironment.WATERDOG_DEFAULT},
            MinecraftServiceType.BEDROCK_PROXY,
            19132
    );

    private final ServiceEnvironment[] environments;
    private final MinecraftServiceType type;

    private final int defaultStartPort;
    private final Collection<String> ignoredConsoleLines;

    ServiceEnvironmentType(ServiceEnvironment[] environments, MinecraftServiceType type, int defaultStartPort) {
        this(environments, type, defaultStartPort, new String[0]);
    }

    ServiceEnvironmentType(ServiceEnvironment[] environments, MinecraftServiceType type, int defaultStartPort, String[] ignoredConsoleLines) {
        this.environments = environments;
        this.type = type;
        this.defaultStartPort = defaultStartPort;
        this.ignoredConsoleLines = Arrays.asList(ignoredConsoleLines);
    }

    @Deprecated
    public @Nullable String getMainClass(@Nullable File applicationFile) {
        return applicationFile == null ? null : this.getMainClass(applicationFile.toPath());
    }

    public @Nullable String getMainClass(@Nullable Path applicationFile) {
        if (applicationFile != null && Files.exists(applicationFile)) {
            try (JarInputStream stream = new JarInputStream(Files.newInputStream(applicationFile))) {
                Manifest manifest = stream.getManifest();
                return manifest == null ? null : manifest.getMainAttributes().getValue("Main-Class");
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
        return null;
    }

    @Deprecated
    public @NotNull String getClasspath(@NotNull File wrapperFile, @Nullable File applicationFile) {
        return this.getClasspath(wrapperFile.toPath(), applicationFile == null ? null : applicationFile.toPath());
    }

    public @NotNull String getClasspath(@NotNull Path wrapperFile, @Nullable Path applicationFile) {
        return wrapperFile.toAbsolutePath() + File.pathSeparator + (applicationFile == null ? "" : applicationFile.toAbsolutePath());
    }

    public ServiceEnvironment[] getEnvironments() {
        return this.environments;
    }

    public MinecraftServiceType getMinecraftType() {
        return this.type;
    }

    public boolean isMinecraftJavaProxy() {
        return this.type == MinecraftServiceType.JAVA_PROXY;
    }

    public boolean isMinecraftBedrockProxy() {
        return this.type == MinecraftServiceType.BEDROCK_PROXY;
    }

    public boolean isMinecraftJavaServer() {
        return this.type == MinecraftServiceType.JAVA_SERVER;
    }

    public boolean isMinecraftBedrockServer() {
        return this.type == MinecraftServiceType.BEDROCK_SERVER;
    }

    public boolean isMinecraftProxy() {
        return this.isMinecraftJavaProxy() || this.isMinecraftBedrockProxy();
    }

    public boolean isMinecraftServer() {
        return this.isMinecraftJavaServer() || this.isMinecraftBedrockServer();
    }

    public boolean isMinecraftJava() {
        return this.isMinecraftJavaServer() || this.isMinecraftJavaProxy();
    }

    public boolean isMinecraftBedrock() {
        return this.isMinecraftBedrockServer() || this.isMinecraftBedrockProxy();
    }

    public int getDefaultStartPort() {
        return this.defaultStartPort;
    }

    public Collection<String> getIgnoredConsoleLines() {
        return this.ignoredConsoleLines;
    }
}