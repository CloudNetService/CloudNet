package de.dytanic.cloudnet.ext.signs;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

import java.lang.reflect.Type;

public class Sign implements Comparable<Sign> {

    public static final Type TYPE = new TypeToken<Sign>() {
    }.getType();

    protected long signId;

    protected String providedGroup, targetGroup, templatePath;

    protected SignPosition worldPosition;

    //*=-- ---------------------------------------------------

    private volatile ServiceInfoSnapshot serviceInfoSnapshot;

    public Sign(String providedGroup, String targetGroup, SignPosition worldPosition, String templatePath) {
        this.signId = System.currentTimeMillis();
        //=- * | * -=//
        this.providedGroup = providedGroup;
        this.targetGroup = targetGroup;
        this.templatePath = templatePath;
        this.worldPosition = worldPosition;
    }

    @Override
    public int compareTo(Sign o) {
        return Long.compare(signId, o.getSignId());
    }

    public long getSignId() {
        return this.signId;
    }

    public String getProvidedGroup() {
        return this.providedGroup;
    }

    public String getTargetGroup() {
        return this.targetGroup;
    }

    public String getTemplatePath() {
        return this.templatePath;
    }

    public SignPosition getWorldPosition() {
        return this.worldPosition;
    }

    public ServiceInfoSnapshot getServiceInfoSnapshot() {
        return this.serviceInfoSnapshot;
    }

    public void setSignId(long signId) {
        this.signId = signId;
    }

    public void setProvidedGroup(String providedGroup) {
        this.providedGroup = providedGroup;
    }

    public void setTargetGroup(String targetGroup) {
        this.targetGroup = targetGroup;
    }

    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }

    public void setWorldPosition(SignPosition worldPosition) {
        this.worldPosition = worldPosition;
    }

    public void setServiceInfoSnapshot(ServiceInfoSnapshot serviceInfoSnapshot) {
        this.serviceInfoSnapshot = serviceInfoSnapshot;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Sign)) return false;
        final Sign other = (Sign) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.getSignId() != other.getSignId()) return false;
        final Object this$providedGroup = this.getProvidedGroup();
        final Object other$providedGroup = other.getProvidedGroup();
        if (this$providedGroup == null ? other$providedGroup != null : !this$providedGroup.equals(other$providedGroup))
            return false;
        final Object this$targetGroup = this.getTargetGroup();
        final Object other$targetGroup = other.getTargetGroup();
        if (this$targetGroup == null ? other$targetGroup != null : !this$targetGroup.equals(other$targetGroup))
            return false;
        final Object this$templatePath = this.getTemplatePath();
        final Object other$templatePath = other.getTemplatePath();
        if (this$templatePath == null ? other$templatePath != null : !this$templatePath.equals(other$templatePath))
            return false;
        final Object this$worldPosition = this.getWorldPosition();
        final Object other$worldPosition = other.getWorldPosition();
        if (this$worldPosition == null ? other$worldPosition != null : !this$worldPosition.equals(other$worldPosition))
            return false;
        final Object this$serviceInfoSnapshot = this.getServiceInfoSnapshot();
        final Object other$serviceInfoSnapshot = other.getServiceInfoSnapshot();
        if (this$serviceInfoSnapshot == null ? other$serviceInfoSnapshot != null : !this$serviceInfoSnapshot.equals(other$serviceInfoSnapshot))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Sign;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $signId = this.getSignId();
        result = result * PRIME + (int) ($signId >>> 32 ^ $signId);
        final Object $providedGroup = this.getProvidedGroup();
        result = result * PRIME + ($providedGroup == null ? 43 : $providedGroup.hashCode());
        final Object $targetGroup = this.getTargetGroup();
        result = result * PRIME + ($targetGroup == null ? 43 : $targetGroup.hashCode());
        final Object $templatePath = this.getTemplatePath();
        result = result * PRIME + ($templatePath == null ? 43 : $templatePath.hashCode());
        final Object $worldPosition = this.getWorldPosition();
        result = result * PRIME + ($worldPosition == null ? 43 : $worldPosition.hashCode());
        final Object $serviceInfoSnapshot = this.getServiceInfoSnapshot();
        result = result * PRIME + ($serviceInfoSnapshot == null ? 43 : $serviceInfoSnapshot.hashCode());
        return result;
    }

    public String toString() {
        return "Sign(signId=" + this.getSignId() + ", providedGroup=" + this.getProvidedGroup() + ", targetGroup=" + this.getTargetGroup() + ", templatePath=" + this.getTemplatePath() + ", worldPosition=" + this.getWorldPosition() + ", serviceInfoSnapshot=" + this.getServiceInfoSnapshot() + ")";
    }
}