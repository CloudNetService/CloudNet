package de.dytanic.cloudnet.driver.network.cluster;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import de.dytanic.cloudnet.driver.service.ProcessSnapshot;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.lang.reflect.Type;
import java.util.Collection;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class NetworkClusterNodeInfoSnapshot extends BasicJsonDocPropertyable {

    public static final Type TYPE = new TypeToken<NetworkClusterNodeInfoSnapshot>() {
    }.getType();

    protected long creationTime;

    protected NetworkClusterNode node;

    protected String version;

    protected int currentServicesCount, usedMemory, reservedMemory, maxMemory;

    protected ProcessSnapshot processSnapshot;

    protected Collection<NetworkClusterNodeExtensionSnapshot> extensions;

}