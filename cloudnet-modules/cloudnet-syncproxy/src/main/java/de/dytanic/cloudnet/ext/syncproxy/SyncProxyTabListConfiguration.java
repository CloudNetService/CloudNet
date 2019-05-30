package de.dytanic.cloudnet.ext.syncproxy;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncProxyTabListConfiguration {

  protected String targetGroup;

  protected List<SyncProxyTabList> entries;

  protected int animationsPerSecond;

}