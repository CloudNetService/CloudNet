package eu.cloudnetservice.cloudnet.ext.signs;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignsConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractSignManagement implements SignManagement {

    public static final String SIGN_CHANNEL_NAME = "internal_sign_channel";

    protected static final String SIGN_CREATED = "signs_sign_created";
    protected static final String SIGN_DELETED = "signs_sign_deleted";
    protected static final String SIGN_BULK_DELETE = "signs_sign_bulk_deleted";
    protected static final String SIGN_CONFIGURATION_UPDATE = "signs_sign_config_update";
    protected static final String SIGN_GET_SIGNS_BY_GROUPS = "signs_get_signs_by_groups";

    protected final Map<WorldPosition, Sign> signs = new ConcurrentHashMap<>();
    protected SignsConfiguration signsConfiguration;

    protected AbstractSignManagement(SignsConfiguration signsConfiguration) {
        this.signsConfiguration = signsConfiguration;
    }

    @Override
    public @Nullable Sign getSignAt(@NotNull WorldPosition position) {
        return this.signs.get(position);
    }

    @Override
    public void deleteSign(@NotNull Sign sign) {
        this.deleteSign(sign.getWorldPosition());
    }

    @Override
    public int deleteAllSigns(@NotNull String group) {
        return this.deleteAllSigns(group, null);
    }

    @Override
    public @NotNull Collection<Sign> getSigns() {
        return this.signs.values();
    }

    @Override
    public @NotNull SignsConfiguration getSignsConfiguration() {
        return this.signsConfiguration;
    }

    @Override
    public void setSignsConfiguration(@NotNull SignsConfiguration signsConfiguration) {
        this.signsConfiguration = signsConfiguration;
    }

    @Override
    public void registerToServiceRegistry() {
        CloudNetDriver.getInstance().getServicesRegistry().registerService(SignManagement.class, "SignManagement", this);
    }

    @Override
    public void unregisterFromServiceRegistry() {
        CloudNetDriver.getInstance().getServicesRegistry().unregisterService(SignManagement.class, "SignManagement");
    }

    @Override
    public void handleInternalSignCreate(@NotNull Sign sign) {
        this.signs.put(sign.getWorldPosition(), sign);
    }

    @Override
    public void handleInternalSignRemove(@NotNull WorldPosition position) {
        this.signs.remove(position);
    }

    @Override
    public void handleInternalSignConfigUpdate(@NotNull SignsConfiguration configuration) {
        this.signsConfiguration = configuration;
    }

    protected ChannelMessage.Builder channelMessage(@NotNull String message) {
        return ChannelMessage.builder()
                .channel(SIGN_CHANNEL_NAME)
                .message(message);
    }
}
