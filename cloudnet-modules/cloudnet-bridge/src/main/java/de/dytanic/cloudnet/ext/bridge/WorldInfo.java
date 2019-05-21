package de.dytanic.cloudnet.ext.bridge;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
public class WorldInfo {

    protected UUID uniqueId;

    protected String name;

    protected String difficulty;

    protected Map<String, String> gameRules;

}