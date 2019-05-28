package de.dytanic.cloudnet.ext.bridge;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PluginInfo extends BasicJsonDocPropertyable {

  private final String name, version;

}