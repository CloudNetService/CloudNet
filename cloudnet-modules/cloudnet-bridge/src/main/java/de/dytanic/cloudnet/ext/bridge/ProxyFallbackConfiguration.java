package de.dytanic.cloudnet.ext.bridge;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProxyFallbackConfiguration {

  protected String targetGroup, defaultFallbackTask;

  protected List<ProxyFallback> fallbacks;

}