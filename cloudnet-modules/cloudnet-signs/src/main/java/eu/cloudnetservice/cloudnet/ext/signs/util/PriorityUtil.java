package eu.cloudnetservice.cloudnet.ext.signs.util;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.ServiceInfoStateWatcher;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignConfigurationEntry;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public final class PriorityUtil {

    private PriorityUtil() {
        throw new UnsupportedOperationException();
    }

    public static int getPriority(@NotNull ServiceInfoSnapshot snapshot) {
        // Get the state of the service
        return getPriority(snapshot, false);
    }

    public static int getPriority(@NotNull ServiceInfoSnapshot snapshot, @Nullable SignConfigurationEntry entry) {
        // Get the state of the service
        return getPriority(snapshot, entry != null && entry.isSwitchToSearchingWhenServiceIsFull());
    }

    public static int getPriority(@NotNull ServiceInfoSnapshot snapshot, boolean lowerFullToSearching) {
        // Get the state of the service
        ServiceInfoStateWatcher.ServiceInfoState state = ServiceInfoStateWatcher.stateFromServiceInfoSnapshot(snapshot);
        switch (state) {
            case FULL_ONLINE:
                // full (premium) service are preferred
                return lowerFullToSearching ? 1 : 4;
            case ONLINE:
                // online has the second highest priority as full is preferred
                return 3;
            case EMPTY_ONLINE:
                // empty services are not the first choice for a sign wall
                return 2;
            case STARTING:
            case STOPPED:
                // this sign should only be on the wall when there is no other service
                return 1;
            default:
                return 0;
        }
    }
}
