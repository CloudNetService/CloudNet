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

        long onlineServices = event.getTargetServiceInfoSnapshots().stream().filter(ServiceInfoSnapshotUtil::isOnline).count();
        long inGameServices = event.getTargetServiceInfoSnapshots().stream().filter(ServiceInfoSnapshotUtil::isIngameService).count();

        event.addSummaryParameter("Online: " + onlineServices);
        event.addSummaryParameter("Ingame: " + inGameServices);
    }

}
