package de.dytanic.cloudnet.driver.serialization;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeExtensionSnapshot;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.service.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class NodeInfoSnapshotSerializerTest {

    @Test
    public void serializeNodeInfoSnapshot() {
        NetworkClusterNodeInfoSnapshot original = new NetworkClusterNodeInfoSnapshot(
                Long.MAX_VALUE,
                new NetworkClusterNode("Node-XXX", new HostAndPort[]{new HostAndPort("127.0.0.1", 12345), new HostAndPort("123.456.789.012", 98765)}),
                "3.3.0-RELEASE",
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                Integer.MIN_VALUE,
                new ProcessSnapshot(-1, -1, -1, -1, -1, -1, Collections.emptyList(), Double.MAX_VALUE, Integer.MIN_VALUE),
                Arrays.asList(
                        new NetworkClusterNodeExtensionSnapshot("eu.cloudnetservice.cloudnet.modules", "some-module", "1.2.3.4", "CloudNetService", "https://cloudnetservice.eu", "description for this module"),
                        new NetworkClusterNodeExtensionSnapshot("eu.cloudnetservice.cloudnet.x", "random-modules", "x.y.z", "unknown", "no website", "another description")
                ),
                Double.MIN_VALUE
        );

        ProtocolBuffer buffer = ProtocolBuffer.create();
        buffer.writeObject(original);

        NetworkClusterNodeInfoSnapshot deserialized = buffer.readObject(NetworkClusterNodeInfoSnapshot.class);

        Assert.assertEquals(original, deserialized);
    }

}
