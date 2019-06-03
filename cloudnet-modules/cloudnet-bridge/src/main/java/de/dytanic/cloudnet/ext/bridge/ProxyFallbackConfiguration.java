package de.dytanic.cloudnet.ext.bridge;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ProxyFallbackConfiguration {

    protected String targetGroup, defaultFallbackTask;

    protected List<ProxyFallback> fallbacks;

}