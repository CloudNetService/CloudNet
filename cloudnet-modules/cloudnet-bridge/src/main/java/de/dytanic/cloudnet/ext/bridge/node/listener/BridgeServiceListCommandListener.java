package de.dytanic.cloudnet.ext.bridge.node.listener;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.event.ServiceListCommandEvent;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;

public class BridgeServiceListCommandListener {

    @EventListener
    public void handleCommand(ServiceListCommandEvent event) {
        event.addParameter(serviceInfoSnapshot -> serviceInfoSnapshot.getProperty(BridgeServiceProperty.IS_ONLINE).orElse(false) ?
                "Players: " + serviceInfoSnapshot.getProperty(BridgeServiceProperty.ONLINE_COUNT).orElse(0) + "/" + serviceInfoSnapshot.getProperty(BridgeServiceProperty.MAX_PLAYERS).orElse(0) :
                null);
        event.addParameter(serviceInfoSnapshot -> serviceInfoSnapshot.getProperty(BridgeServiceProperty.IS_IN_GAME).orElse(false) ? "Ingame" : null);

        long onlineServices = event.getTargetServiceInfoSnapshots().stream().filter(serviceInfoSnapshot -> serviceInfoSnapshot.getProperty(BridgeServiceProperty.IS_ONLINE).orElse(false)).count();
        long inGameServices = event.getTargetServiceInfoSnapshots().stream().filter(serviceInfoSnapshot -> serviceInfoSnapshot.getProperty(BridgeServiceProperty.IS_IN_GAME).orElse(false)).count();

        event.addSummaryParameter("Online: " + onlineServices);
        event.addSummaryParameter("Ingame: " + inGameServices);
    }

}
