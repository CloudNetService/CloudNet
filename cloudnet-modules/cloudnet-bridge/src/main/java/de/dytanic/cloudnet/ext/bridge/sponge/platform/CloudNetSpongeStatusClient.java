package de.dytanic.cloudnet.ext.bridge.sponge.platform;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.MinecraftVersion;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.network.status.StatusClient;

import java.net.InetSocketAddress;
import java.util.Optional;

public class CloudNetSpongeStatusClient implements StatusClient {

    public static final StatusClient INSTANCE = new CloudNetSpongeStatusClient();

    private final InetSocketAddress address = new InetSocketAddress("127.0.0.1", 53345);
    private final MinecraftVersion minecraftVersion = Sponge.getPlatform().getMinecraftVersion();

    @Override
    public @NotNull InetSocketAddress getAddress() {
        return this.address;
    }

    @Override
    public @NotNull MinecraftVersion getVersion() {
        return this.minecraftVersion;
    }

    @Override
    public @NotNull Optional<InetSocketAddress> getVirtualHost() {
        return Optional.empty();
    }
}
