package eu.cloudnetservice.cloudnet.ext.signs;

import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignsConfiguration;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface SignManagement {

    @Nullable
    Sign getSignAt(@NotNull WorldPosition position);

    void createSign(@NotNull Sign sign);

    void deleteSign(@NotNull Sign sign);

    void deleteSign(@NotNull WorldPosition position);

    int deleteAllSigns(@NotNull String group);

    int deleteAllSigns(@NotNull String group, @Nullable String templatePath);

    int deleteAllSigns();

    @NotNull Collection<Sign> getSigns();

    @NotNull Collection<Sign> getSigns(@NotNull String[] group);

    @NotNull SignsConfiguration getSignsConfiguration();

    void setSignsConfiguration(@NotNull SignsConfiguration configuration);

    // Internal methods

    @ApiStatus.Internal
    void registerToServiceRegistry();

    @ApiStatus.Internal
    void unregisterFromServiceRegistry();

    @ApiStatus.Internal
    void handleInternalSignCreate(@NotNull Sign sign);

    @ApiStatus.Internal
    void handleInternalSignRemove(@NotNull WorldPosition position);

    @ApiStatus.Internal
    void handleInternalSignConfigUpdate(@NotNull SignsConfiguration configuration);
}
