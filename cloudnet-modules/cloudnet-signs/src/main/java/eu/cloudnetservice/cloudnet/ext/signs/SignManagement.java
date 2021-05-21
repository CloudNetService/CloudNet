package eu.cloudnetservice.cloudnet.ext.signs;

import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignsConfiguration;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Represents a shared management for the sign system.
 */
public interface SignManagement {

    /**
     * Get a sign which is located at the specified location.
     *
     * @param position the position the sign is located at
     * @return the sign at the given location or null if there is no sign
     */
    @Nullable Sign getSignAt(@NotNull WorldPosition position);

    /**
     * Creates a new sign.
     *
     * @param sign the sign to create
     */
    void createSign(@NotNull Sign sign);

    /**
     * Deletes the specified sign.
     *
     * @param sign the sign to delete.
     */
    void deleteSign(@NotNull Sign sign);

    /**
     * Deletes the sign at the given position.
     *
     * @param position the position of the sign to delete.
     */
    void deleteSign(@NotNull WorldPosition position);

    /**
     * Deletes all signs of the specified group.
     *
     * @param group the group to delete the signs of
     * @return the amount of deleted signs
     */
    int deleteAllSigns(@NotNull String group);

    /**
     * Deletes all signs of the specified group.
     *
     * @param group        the group to delete the signs of
     * @param templatePath the template path of the signs to delete
     * @return the amount of deleted signs
     */
    int deleteAllSigns(@NotNull String group, @Nullable String templatePath);

    /**
     * Deletes all signs.
     *
     * @return the amount of deleted signs
     */
    int deleteAllSigns();

    /**
     * Get all registered signs.
     *
     * @return all registered signs.
     */
    @NotNull Collection<Sign> getSigns();

    /**
     * Get all signs of the specified groups.
     *
     * @param groups the groups the signs are created on
     * @return all signs that are created on the given groups
     */
    @NotNull Collection<Sign> getSigns(@NotNull String[] groups);

    /**
     * Get the current sign configuration.
     *
     * @return the current sign configuration
     */
    @NotNull SignsConfiguration getSignsConfiguration();

    /**
     * Sets the sign configuration and updates it to all connected components.
     *
     * @param configuration the new signs configuration.
     */
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
