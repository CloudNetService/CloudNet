package eu.cloudnetservice.cloudnet.ext.signs.configuration;

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import org.jetbrains.annotations.NotNull;

public class SignGroupConfiguration implements Cloneable, SerializableObject {

    protected String targetGroup;

    protected SignLayoutsHolder emptyLayout;
    protected SignLayoutsHolder onlineLayout;
    protected SignLayoutsHolder fullLayout;

    public SignGroupConfiguration(String targetGroup, SignLayoutsHolder emptyLayout, SignLayoutsHolder onlineLayout, SignLayoutsHolder fullLayout) {
        this.targetGroup = targetGroup;
        this.emptyLayout = emptyLayout;
        this.onlineLayout = onlineLayout;
        this.fullLayout = fullLayout;
    }

    public String getTargetGroup() {
        return targetGroup;
    }

    public SignLayoutsHolder getEmptyLayout() {
        return emptyLayout;
    }

    public SignLayoutsHolder getOnlineLayout() {
        return onlineLayout;
    }

    public SignLayoutsHolder getFullLayout() {
        return fullLayout;
    }

    @Override
    public void write(@NotNull ProtocolBuffer buffer) {
        buffer.writeString(this.targetGroup);
        buffer.writeObject(this.emptyLayout);
        buffer.writeObject(this.onlineLayout);
        buffer.writeObject(this.fullLayout);
    }

    @Override
    public void read(@NotNull ProtocolBuffer buffer) {
        this.targetGroup = buffer.readString();
        this.emptyLayout = buffer.readObject(SignLayoutsHolder.class);
        this.onlineLayout = buffer.readObject(SignLayoutsHolder.class);
        this.fullLayout = buffer.readObject(SignLayoutsHolder.class);
    }
}
