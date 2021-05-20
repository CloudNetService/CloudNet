package eu.cloudnetservice.cloudnet.ext.signs;

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.ServiceInfoStateWatcher;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

/**
 * A sign object representation. It's used for database entries and general handling in the api.
 */
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Sign implements SerializableObject, Comparable<Sign> {

    protected String targetGroup;
    protected String createdGroup;
    protected String templatePath;

    @EqualsAndHashCode.Include
    protected WorldPosition worldPosition;

    protected transient AtomicReference<ServiceInfoSnapshot> currentTarget;

    /**
     * Creates a new sign object
     *
     * @param targetGroup   the group the sign is targeting
     * @param createdGroup  the group the sign was created on
     * @param worldPosition the position of the sign in the world
     */
    public Sign(String targetGroup, String createdGroup, WorldPosition worldPosition) {
        this(targetGroup, createdGroup, null, worldPosition);
    }

    /**
     * Creates a new sign object
     *
     * @param targetGroup   the group the sign is targeting
     * @param createdGroup  the group the sign was created on
     * @param templatePath  the template of this
     * @param worldPosition the position of the sign in the world
     */
    public Sign(String targetGroup, String createdGroup, String templatePath, WorldPosition worldPosition) {
        this.targetGroup = targetGroup;
        this.createdGroup = createdGroup;
        this.templatePath = templatePath;
        this.worldPosition = worldPosition;
    }

    public String getTargetGroup() {
        return targetGroup;
    }

    public void setTargetGroup(String targetGroup) {
        this.targetGroup = targetGroup;
    }

    public String getCreatedGroup() {
        return createdGroup;
    }

    public void setCreatedGroup(String createdGroup) {
        this.createdGroup = createdGroup;
    }

    public String getTemplatePath() {
        return templatePath;
    }

    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }

    public WorldPosition getWorldPosition() {
        return worldPosition;
    }

    public void setWorldPosition(WorldPosition worldPosition) {
        this.worldPosition = worldPosition;
    }

    public ServiceInfoSnapshot getCurrentTarget() {
        return this.currentTarget == null ? null : this.currentTarget.get();
    }

    public void setCurrentTarget(ServiceInfoSnapshot currentTarget) {
        if (this.currentTarget == null) {
            this.currentTarget = new AtomicReference<>(currentTarget);
        } else {
            this.currentTarget.lazySet(currentTarget);
        }
    }

    /**
     * Get the priority of the sign to be on the sign wall
     *
     * @return the priority of the sign to be on the sign wall
     */
    public int getPriority() {
        // check if the service has a snapshot
        ServiceInfoSnapshot target = this.getCurrentTarget();
        if (target == null) {
            // no target has the lowest priority
            return 0;
        }
        // Get the state of the service
        ServiceInfoStateWatcher.ServiceInfoState state = ServiceInfoStateWatcher.stateFromServiceInfoSnapshot(target);
        switch (state) {
            case FULL_ONLINE:
                // full (premium) service are preferred
                return 4;
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

    @Override
    public void write(@NotNull ProtocolBuffer buffer) {
        buffer.writeString(this.targetGroup);
        buffer.writeString(this.createdGroup);
        buffer.writeOptionalString(this.templatePath);
        buffer.writeObject(this.worldPosition);
    }

    @Override
    public void read(@NotNull ProtocolBuffer buffer) {
        this.targetGroup = buffer.readString();
        this.createdGroup = buffer.readString();
        this.templatePath = buffer.readOptionalString();
        this.worldPosition = buffer.readObject(WorldPosition.class);
    }

    @Override
    public int compareTo(@NotNull Sign sign) {
        return Integer.compare(this.getPriority(), sign.getPriority());
    }
}
