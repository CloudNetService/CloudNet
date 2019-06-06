package de.dytanic.cloudnet.ext.signs;

import java.util.List;

public class SignLayoutConfiguration {

    protected List<SignLayout> signLayouts;

    protected int animationsPerSecond;

    public SignLayoutConfiguration(List<SignLayout> signLayouts, int animationsPerSecond) {
        this.signLayouts = signLayouts;
        this.animationsPerSecond = animationsPerSecond;
    }

    public SignLayoutConfiguration() {
    }

    public List<SignLayout> getSignLayouts() {
        return this.signLayouts;
    }

    public int getAnimationsPerSecond() {
        return this.animationsPerSecond;
    }

    public void setSignLayouts(List<SignLayout> signLayouts) {
        this.signLayouts = signLayouts;
    }

    public void setAnimationsPerSecond(int animationsPerSecond) {
        this.animationsPerSecond = animationsPerSecond;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof SignLayoutConfiguration)) return false;
        final SignLayoutConfiguration other = (SignLayoutConfiguration) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$signLayouts = this.getSignLayouts();
        final Object other$signLayouts = other.getSignLayouts();
        if (this$signLayouts == null ? other$signLayouts != null : !this$signLayouts.equals(other$signLayouts))
            return false;
        if (this.getAnimationsPerSecond() != other.getAnimationsPerSecond()) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof SignLayoutConfiguration;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $signLayouts = this.getSignLayouts();
        result = result * PRIME + ($signLayouts == null ? 43 : $signLayouts.hashCode());
        result = result * PRIME + this.getAnimationsPerSecond();
        return result;
    }

    public String toString() {
        return "SignLayoutConfiguration(signLayouts=" + this.getSignLayouts() + ", animationsPerSecond=" + this.getAnimationsPerSecond() + ")";
    }
}