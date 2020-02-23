package de.dytanic.cloudnet.ext.bridge.node.listener;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.event.ServiceListCommandEvent;
import de.dytanic.cloudnet.ext.bridge.ServiceInfoSnapshotUtil;

public class BridgeServiceListCommandListener {

    @EventListener
    public void handleCommand(ServiceListCommandEvent event) {
        event.addParameter(serviceInfoSnapshot -> ServiceInfoSnapshotUtil.isOnline(serviceInfoSnapshot) ?
                "Players: " + ServiceInfoSnapshotUtil.getOnlineCount(serviceInfoSnapshot) + "/" + ServiceInfoSnapshotUtil.getMaxPlayers(serviceInfoSnapshot) :
                null);
    }

}
