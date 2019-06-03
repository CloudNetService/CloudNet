package de.dytanic.cloudnet.ext.signs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignPosition {

    protected double x, y, z, yaw, pitch;

    protected String group, world;

}