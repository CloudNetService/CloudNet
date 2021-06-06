package de.dytanic.cloudnet.ext.bridge;

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class ProxyFallbackConfiguration {

  protected String targetGroup, defaultFallbackTask;

  protected List<ProxyFallback> fallbacks;

  public ProxyFallbackConfiguration(String targetGroup, String defaultFallbackTask, List<ProxyFallback> fallbacks) {
    this.targetGroup = targetGroup;
    this.defaultFallbackTask = defaultFallbackTask;
    this.fallbacks = fallbacks;
  }

  public String getTargetGroup() {
    return this.targetGroup;
  }

  public void setTargetGroup(String targetGroup) {
    this.targetGroup = targetGroup;
  }

  public String getDefaultFallbackTask() {
    return this.defaultFallbackTask;
  }

  public void setDefaultFallbackTask(String defaultFallbackTask) {
    this.defaultFallbackTask = defaultFallbackTask;
  }

  public List<ProxyFallback> getFallbacks() {
    return this.fallbacks;
  }

  public void setFallbacks(List<ProxyFallback> fallbacks) {
    this.fallbacks = fallbacks;
  }

}
