package de.dytanic.cloudnet.driver.provider.service;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.api.DriverAPIUser;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.service.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class RemoteCloudServiceFactory extends DefaultCloudServiceFactory implements CloudServiceFactory, DriverAPIUser {

    private final Supplier<INetworkChannel> channelSupplier;

    public RemoteCloudServiceFactory(Supplier<INetworkChannel> channelSupplier) {
        this.channelSupplier = channelSupplier;
    }

    @Override
    public @Nullable ServiceInfoSnapshot createCloudService(ServiceConfiguration serviceConfiguration) {
        return this.createCloudServiceAsync(serviceConfiguration).get(5, TimeUnit.SECONDS, null);
    }

    @Override
    @NotNull
    public ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceConfiguration serviceConfiguration) {
        Preconditions.checkNotNull(serviceConfiguration);

        return this.executeDriverAPIMethod(
                DriverAPIRequestType.CREATE_CLOUD_SERVICE_BY_CONFIGURATION,
                buffer -> buffer.writeObject(serviceConfiguration),
                packet -> packet.getBuffer().readObject(ServiceInfoSnapshot.class)
        );
    }

    @Override
    public INetworkChannel getNetworkChannel() {
        return this.channelSupplier.get();
    }
}
