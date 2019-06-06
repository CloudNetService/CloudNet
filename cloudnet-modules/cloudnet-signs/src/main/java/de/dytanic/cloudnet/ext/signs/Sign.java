package de.dytanic.cloudnet.ext.signs;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.lang.reflect.Type;

@ToString
@EqualsAndHashCode
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

}