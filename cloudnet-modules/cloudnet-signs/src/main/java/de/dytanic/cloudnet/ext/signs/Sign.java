package de.dytanic.cloudnet.ext.signs;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.lang.reflect.Type;

@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Sign implements Comparable<Sign> {

    public static final Type TYPE = new TypeToken<Sign>() {
    }.getType();

    protected long signId;

    protected String providedGroup, targetGroup, templatePath;

    @EqualsAndHashCode.Include
    protected WorldPosition worldPosition;

    private volatile ServiceInfoSnapshot serviceInfoSnapshot;

    public Sign(String providedGroup, String targetGroup, WorldPosition worldPosition, String templatePath) {
        this.signId = System.currentTimeMillis();

        this.providedGroup = providedGroup;
        this.targetGroup = targetGroup;
        this.templatePath = templatePath;
        this.worldPosition = worldPosition;
    }

    @Override
    public int compareTo(Sign toCompare) {
        return Long.compare(signId, toCompare.getSignId());
    }

    public long getSignId() {
        return this.signId;
    }

    public void setSignId(long signId) {
        this.signId = signId;
    }

    public String getProvidedGroup() {
        return this.providedGroup;
    }

    public void setProvidedGroup(String providedGroup) {
        this.providedGroup = providedGroup;
    }

    public String getTargetGroup() {
        return this.targetGroup;
    }

    public void setTargetGroup(String targetGroup) {
        this.targetGroup = targetGroup;
    }

    public String getTemplatePath() {
        return this.templatePath;
    }

    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }

    public WorldPosition getWorldPosition() {
        return this.worldPosition;
    }

    public void setWorldPosition(WorldPosition worldPosition) {
        this.worldPosition = worldPosition;
    }

    public ServiceInfoSnapshot getServiceInfoSnapshot() {
        return this.serviceInfoSnapshot;
    }

    public void setServiceInfoSnapshot(ServiceInfoSnapshot serviceInfoSnapshot) {
        this.serviceInfoSnapshot = serviceInfoSnapshot;
    }

}