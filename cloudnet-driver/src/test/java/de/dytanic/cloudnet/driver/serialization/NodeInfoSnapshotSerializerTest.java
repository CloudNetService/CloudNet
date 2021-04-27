package de.dytanic.cloudnet.driver.serialization;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.module.ModuleConfiguration;
import de.dytanic.cloudnet.driver.module.ModuleDependency;
import de.dytanic.cloudnet.driver.module.ModuleRepository;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ProcessSnapshot;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class NodeInfoSnapshotSerializerTest {

    @Test
    public void serializeNodeInfoSnapshot() {
        NetworkClusterNodeInfoSnapshot original = new NetworkClusterNodeInfoSnapshot(
                System.currentTimeMillis(),
                System.nanoTime(),
                new NetworkClusterNode("Node-XXX", new HostAndPort[]{new HostAndPort("127.0.0.1", 12345), new HostAndPort("123.456.789.012", 98765)}),
                "3.3.0-RELEASE",
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                Integer.MIN_VALUE,
                new ProcessSnapshot(-1, -1, -1, -1, -1, -1, Collections.emptyList(), Double.MAX_VALUE, Integer.MIN_VALUE),
                Arrays.asList(
                        new ModuleConfiguration(
                                true, true,
                                "eu.cloudnetservice.cloudnet.x", "random-modules", "x.y.z",
                                "main", "desc", "CloudNetService",
                                null,
                                null,
                                new ModuleDependency[]{
                                        new ModuleDependency("URL")
                                },
                                null
                        ),
                        new ModuleConfiguration(
                                true, false,
                                "eu.cloudnetservice.cloudnet.x", "random-modules", "x.y.z",
                                "eu.cloudnetservice....RandomModule", "description", "CloudNetService",
                                null,
                                new ModuleRepository[]{
                                        new ModuleRepository("cloudnet", "https://repo.cloudnetservice.eu/repository/releases")
                                },
                                new ModuleDependency[]{
                                        new ModuleDependency("cloudnet", "group", "name", "version")
                                },
                                JsonDocument.newDocument("properties", "")
                        )
                ),
                100D
        );

        ProtocolBuffer buffer = ProtocolBuffer.create();
        buffer.writeObject(original);

        NetworkClusterNodeInfoSnapshot deserialized = buffer.readObject(NetworkClusterNodeInfoSnapshot.class);

        Assert.assertEquals(original, deserialized);
    }

}
