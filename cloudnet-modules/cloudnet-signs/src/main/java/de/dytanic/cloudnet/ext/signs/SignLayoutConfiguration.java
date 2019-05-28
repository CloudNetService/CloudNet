package de.dytanic.cloudnet.ext.signs;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignLayoutConfiguration {

  protected List<SignLayout> signLayouts;

  protected int animationsPerSecond;

}