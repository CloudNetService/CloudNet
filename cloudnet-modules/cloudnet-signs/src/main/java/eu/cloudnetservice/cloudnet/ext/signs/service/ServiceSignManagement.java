package eu.cloudnetservice.cloudnet.ext.signs.service;

import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.channel.ChannelMessageTarget;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
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
import java.util.function.Function;

public abstract class ServiceSignManagement<T> extends AbstractSignManagement implements SignManagement {

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

    public abstract boolean canConnect(@NotNull Sign sign, @NotNull Function<String, Boolean> permissionChecker);

    protected abstract void startKnockbackTask();

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

    @Override
    protected ChannelMessage.Builder channelMessage(@NotNull String message) {
        return super.channelMessage(message)
                .target(ChannelMessageTarget.Type.NODE, Wrapper.getInstance().getNodeUniqueId());
    }

    protected @Nullable SignConfigurationEntry getApplicableSignConfigurationEntry() {
        for (SignConfigurationEntry entry : this.signsConfiguration.getConfigurationEntries()) {
            if (Arrays.asList(Wrapper.getInstance().getServiceConfiguration().getGroups()).contains(entry.getTargetGroup())) {
                return entry;
            }
        }
        return null;
    }
}
