package de.dytanic.cloudnet.ext.signs.configuration.entry;

import de.dytanic.cloudnet.ext.signs.SignLayout;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.ToString;

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

  public void setSignLayouts(List<SignLayout> signLayouts) {
    this.signLayouts = signLayouts;
  }

  public int getAnimationsPerSecond() {
    return this.animationsPerSecond;
  }

  public void setAnimationsPerSecond(int animationsPerSecond) {
    this.animationsPerSecond = animationsPerSecond;
  }

}
