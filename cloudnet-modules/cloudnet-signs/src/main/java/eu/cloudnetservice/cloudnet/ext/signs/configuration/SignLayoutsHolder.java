package eu.cloudnetservice.cloudnet.ext.signs.configuration;

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@ToString
public class SignLayoutsHolder implements SerializableObject {

    protected int animationsPerSecond;
    protected List<SignLayout> signLayouts;

    protected transient AtomicBoolean tickBlocked;
    protected transient AtomicInteger currentAnimation;

    public SignLayoutsHolder() {
    }

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

    public boolean hasLayouts() {
        return !this.signLayouts.isEmpty();
    }

    public boolean isTickedBlocked() {
        return this.tickBlocked != null && this.tickBlocked.get();
    }

    public void setTickBlocked(boolean tickBlocked) {
        if (this.tickBlocked == null) {
            this.tickBlocked = new AtomicBoolean(tickBlocked);
        } else {
            this.tickBlocked.set(tickBlocked);
        }
    }

    public SignLayoutsHolder releaseTickBlock() {
        if (this.tickBlocked != null) {
            this.tickBlocked.set(false);
        }
        return this;
    }

    public SignLayout getCurrentLayout() {
        return this.getSignLayouts().get(this.getCurrentAnimation());
    }

    public SignLayoutsHolder tick() {
        if (!this.isTickedBlocked()) {
            AtomicInteger currentIndex = this.getCurrentAnimationIndexOrInit();
            if (currentIndex.incrementAndGet() >= this.signLayouts.size()) {
                currentIndex.set(0);
            }
        }
        return this;
    }

    public int getCurrentAnimation() {
        return this.currentAnimation == null ? 0 : this.currentAnimation.get();
    }

    protected AtomicInteger getCurrentAnimationIndexOrInit() {
        if (this.currentAnimation == null) {
            return this.currentAnimation = new AtomicInteger(-1);
        } else {
            return this.currentAnimation;
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
