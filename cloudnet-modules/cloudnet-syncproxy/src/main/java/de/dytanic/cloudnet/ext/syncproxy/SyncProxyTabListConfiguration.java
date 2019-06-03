package de.dytanic.cloudnet.ext.syncproxy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncProxyTabListConfiguration {

    protected String targetGroup;

    protected List<SyncProxyTabList> entries;

    protected int animationsPerSecond;

}