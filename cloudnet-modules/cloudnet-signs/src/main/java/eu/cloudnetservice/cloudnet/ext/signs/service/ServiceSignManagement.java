package eu.cloudnetservice.cloudnet.ext.signs.service;

import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.channel.ChannelMessageTarget;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import de.dytanic.cloudnet.wrapper.Wrapper;
import eu.cloudnetservice.cloudnet.ext.signs.AbstractSignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.Sign;
import eu.cloudnetservice.cloudnet.ext.signs.SignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignConfigurationEntry;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignsConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * An abstract sign management shared between the platform implementations.
 *
 * @param <T> the type of the platform dependant sign extend.
 */
public abstract class ServiceSignManagement<T> extends AbstractSignManagement implements SignManagement {

    public static final String SIGN_GET_SIGNS_BY_GROUPS = "signs_get_signs_by_groups";

    /**
     * {@inheritDoc}
     */
    protected ServiceSignManagement(SignsConfiguration signsConfiguration) {
        super(signsConfiguration);
        // get the signs for the current group
        for (Sign sign : this.getSigns(Wrapper.getInstance().getServiceConfiguration().getGroups())) {
            this.signs.put(sign.getWorldPosition(), sign);
        }
    }

    public abstract void handleServiceAdd(@NotNull ServiceInfoSnapshot snapshot);

    public abstract void handleServiceUpdate(@NotNull ServiceInfoSnapshot snapshot);

    public abstract void handleServiceRemove(@NotNull ServiceInfoSnapshot snapshot);

    @Nullable
    public abstract Sign getSignAt(@NotNull T t);

    @Nullable
    public abstract Sign createSign(@NotNull T t, @NotNull String group);

    @Nullable
    public abstract Sign createSign(@NotNull T t, @NotNull String group, @Nullable String templatePath);

    public abstract void deleteSign(@NotNull T t);

    public abstract int removeMissingSigns();

    /**
     * Checks if the given permissible can connect to the sign.
     *
     * @param sign              the sign to check.
     * @param permissionChecker a function which checks if the supplied string is set as a permission.
     * @return true if the permissible can connect using the sign, false otherwise
     */
    public abstract boolean canConnect(@NotNull Sign sign, @NotNull Function<String, Boolean> permissionChecker);

    @ApiStatus.Internal
    public abstract void initialize();

    @ApiStatus.Internal
    public abstract void initialize(@NotNull Map<SignLayoutsHolder, Set<Sign>> signsNeedingTicking);

    @ApiStatus.Internal
    protected abstract void startKnockbackTask();

    /**
     * Get the signs of all groups the wrapper belongs to.
     *
     * @return the signs of all groups the wrapper belongs to.
     */
    @Override
    public @NotNull Collection<Sign> getSigns() {
        return super.getSigns();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Collection<Sign> getSigns(@NotNull String[] groups) {
        ChannelMessage response = this.channelMessage(SIGN_GET_SIGNS_BY_GROUPS)
                .buffer(ProtocolBuffer.create().writeStringArray(groups))
                .build().sendSingleQuery();
        return response == null ? Collections.emptySet() : response.getBuffer().readObjectCollection(Sign.class);
    }

    @Override
    public void handleInternalSignCreate(@NotNull Sign sign) {
        if (Arrays.asList(Wrapper.getInstance().getServiceConfiguration().getGroups()).contains(sign.getCreatedGroup())) {
            super.handleInternalSignCreate(sign);
        }
    }

    /**
     * Creates a channel message which is targeting the node the wrapper was started by.
     *
     * @param message the message of the channel message.
     * @return the channel message builder for further configuration.
     */
    @Override
    protected ChannelMessage.Builder channelMessage(@NotNull String message) {
        return super.channelMessage(message)
                .target(ChannelMessageTarget.Type.NODE, Wrapper.getInstance().getNodeUniqueId());
    }

    /**
     * Get a sign configuration entry from the sign configuration which targets a group the wrapper belongs to.
     *
     * @return a sign configuration entry from the sign configuration which targets a group the wrapper belongs to.
     */
    public @Nullable SignConfigurationEntry getApplicableSignConfigurationEntry() {
        for (SignConfigurationEntry entry : this.signsConfiguration.getConfigurationEntries()) {
            if (Arrays.asList(Wrapper.getInstance().getServiceConfiguration().getGroups()).contains(entry.getTargetGroup())) {
                return entry;
            }
        }
        return null;
    }
}
