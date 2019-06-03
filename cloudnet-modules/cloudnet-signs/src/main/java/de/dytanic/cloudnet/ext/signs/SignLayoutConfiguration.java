package de.dytanic.cloudnet.ext.signs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignLayoutConfiguration {

    protected List<SignLayout> signLayouts;

    protected int animationsPerSecond;

}