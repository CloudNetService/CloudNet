package de.dytanic.cloudnet.ext.bridge;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorldPosition {

    protected double x, y, z, yaw, pitch;

    protected String world;

}