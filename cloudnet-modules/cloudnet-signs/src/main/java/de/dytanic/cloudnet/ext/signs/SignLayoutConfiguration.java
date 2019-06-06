package de.dytanic.cloudnet.ext.signs;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@ToString
@EqualsAndHashCode
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

}