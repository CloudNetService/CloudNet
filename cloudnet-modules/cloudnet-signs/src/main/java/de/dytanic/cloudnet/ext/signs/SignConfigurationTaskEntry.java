package de.dytanic.cloudnet.ext.signs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignConfigurationTaskEntry {

  protected String task;

  protected SignLayout onlineLayout, emptyLayout, fullLayout;

}