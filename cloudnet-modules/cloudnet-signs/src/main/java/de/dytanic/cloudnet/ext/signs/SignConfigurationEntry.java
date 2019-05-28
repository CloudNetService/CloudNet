package de.dytanic.cloudnet.ext.signs;

import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignConfigurationEntry {

  protected String targetGroup;

  protected boolean switchToSearchingWhenServiceIsFull;

  protected Collection<SignConfigurationTaskEntry> taskLayouts;

  protected SignLayout defaultOnlineLayout, defaultEmptyLayout, defaultFullLayout;

  protected SignLayoutConfiguration startingLayouts, searchLayouts;

}