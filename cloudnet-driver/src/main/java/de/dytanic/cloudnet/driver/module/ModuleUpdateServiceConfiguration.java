package de.dytanic.cloudnet.driver.module;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModuleUpdateServiceConfiguration {

  protected boolean autoInstall;

  protected String url, currentVersion, infoMessage;

}