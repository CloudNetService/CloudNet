package de.dytanic.cloudnet.examples.syncproxy;


import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.syncproxy.AbstractSyncProxyManagement;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyMotd;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyProxyLoginConfiguration;

import java.util.Collections;

/**
 * Examples for plugins running on the proxy
 */
public class ProxySPExample {

    private final AbstractSyncProxyManagement syncProxyManagement = CloudNetDriver.getInstance().getServicesRegistry().getFirstService(AbstractSyncProxyManagement.class);

    /**
     * Actives maintenance for the group of the current proxy
     */
    public void changeLocalMaintenance() {
        SyncProxyProxyLoginConfiguration loginConfiguration = this.syncProxyManagement.getLoginConfiguration();
        loginConfiguration.setMaintenance(true);

        // updating in cluster
        SyncProxyConfiguration.updateSyncProxyConfigurationInNetwork(this.syncProxyManagement.getSyncProxyConfiguration());
    }

    /**
     * Changes the MOTD for the group of the current proxy
     */
    public void changeLocalMOTD() {
        SyncProxyProxyLoginConfiguration loginConfiguration = this.syncProxyManagement.getLoginConfiguration();

        loginConfiguration.setMotds(Collections.singletonList(
                new SyncProxyMotd("Welcome to my server!",
                        "You'll connect to %proxy%",
                        true,
                        1,
                        new String[0],
                        null
                )
        ));

        // updating in cluster
        SyncProxyConfiguration.updateSyncProxyConfigurationInNetwork(this.syncProxyManagement.getSyncProxyConfiguration());
    }


}
