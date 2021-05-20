package eu.cloudnetservice.cloudnet.ext.signs.configuration;

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@ToString
public class SignLayoutsHolder implements SerializableObject {

    protected int animationsPerSecond;
    protected List<SignLayout> signLayouts;

    protected transient AtomicInteger currentAnimation;

    public SignLayoutsHolder(int animationsPerSecond, List<SignLayout> signLayouts) {
        this.animationsPerSecond = animationsPerSecond;
        this.signLayouts = signLayouts;
    }

    public int getAnimationsPerSecond() {
        return animationsPerSecond;
    }

    public void setAnimationsPerSecond(int animationsPerSecond) {
        this.animationsPerSecond = animationsPerSecond;
    }

    public List<SignLayout> getSignLayouts() {
        return signLayouts;
    }

    public void setSignLayouts(List<SignLayout> signLayouts) {
        this.signLayouts = signLayouts;
    }

    public SignLayout getCurrentLayout() {
        return this.getSignLayouts().get(this.getCurrentAnimation());
    }

    public SignLayout tickAndGetCurrentLayout() {
        return this.getSignLayouts().get(this.getCurrentAnimationAndUp());
    }

    public int getCurrentAnimation() {
        return this.currentAnimation == null ? 0 : this.currentAnimation.get();
    }

    public int getCurrentAnimationAndUp() {
        if (this.currentAnimation == null) {
            return (this.currentAnimation = new AtomicInteger(1)).get();
        } else {
            return this.currentAnimation.getAndIncrement();
        }
    }

    @Override
    public void write(@NotNull ProtocolBuffer buffer) {
        buffer.writeInt(this.animationsPerSecond);
        buffer.writeObjectCollection(this.signLayouts);
    }

    @Override
    public void read(@NotNull ProtocolBuffer buffer) {
        this.animationsPerSecond = buffer.readInt();
        this.signLayouts = buffer.readObjectCollection(SignLayout.class);
    }
}
