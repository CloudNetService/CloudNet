package de.dytanic.cloudnet.ext.bridge;

import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WorldInfo {

  protected UUID uniqueId;

  protected String name;

  protected String difficulty;

  protected Map<String, String> gameRules;

}