package de.dytanic.cloudnet.ext.bridge;


import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStartEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStopEvent;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.wrapper.Wrapper;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OnlyProxyProtection {

    public final Map<UUID, String> proxyIpAddress = new HashMap<>();

    private final boolean enabled;

    private final ServiceEnvironmentType environmentType = Wrapper.getInstance().getServiceId().getEnvironment();

    public OnlyProxyProtection(boolean serverOnlineMode) {
        BridgeConfiguration bridgeConfiguration = BridgeConfigurationProvider.load();

        this.enabled = !serverOnlineMode
                && bridgeConfiguration != null
                && bridgeConfiguration.isOnlyProxyProtection()
                && bridgeConfiguration.getExcludedOnlyProxyWalkableGroups() != null
                && bridgeConfiguration.getExcludedOnlyProxyWalkableGroups().stream()
                .noneMatch(group -> Arrays.asList(Wrapper.getInstance().getServiceConfiguration().getGroups()).contains(group));

        if (this.enabled) {
            CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServices().forEach(this::addProxyAddress);

            CloudNetDriver.getInstance().getEventManager().registerListener(this);
        }
    }

    public boolean shouldDisallowPlayer(String playerAddress) {
        return this.enabled && !this.proxyIpAddress.containsValue(playerAddress);
    }

    private void addProxyAddress(ServiceInfoSnapshot proxySnapshot) {
        if (proxySnapshot.getServiceId().getEnvironment().isMinecraftJavaProxy() && this.environmentType.isMinecraftJavaServer()
                || proxySnapshot.getServiceId().getEnvironment().isMinecraftBedrockProxy() && this.environmentType.isMinecraftBedrockServer()) {
            try {
                InetAddress proxyAddress = InetAddress.getByName(proxySnapshot.getAddress().getHost());

                if (proxyAddress.isLoopbackAddress() || proxyAddress.isAnyLocalAddress()) {
                    CloudNetDriver.getInstance().getLogger().warning(
                            String.format(
                                    "[OnlyProxyJoin] This server will reject all connections from proxy %s because it's running on a local or loopback address! " +
                                            "Please set a remote address by changing the 'hostAddress' property in the config.json of %s.",
                                    proxySnapshot.getName(), proxySnapshot.getServiceId().getNodeUniqueId()
                            )
                    );
                    return;
                }

                this.proxyIpAddress.put(proxySnapshot.getServiceId().getUniqueId(), proxyAddress.getHostName());
            } catch (UnknownHostException exception) {
                exception.printStackTrace();
            }
        }
    }

    @EventListener
    public void handleServiceStart(CloudServiceStartEvent event) {
        this.addProxyAddress(event.getServiceInfo());
    }

    @EventListener
    public void handleServiceStop(CloudServiceStopEvent event) {
        this.proxyIpAddress.remove(event.getServiceInfo().getServiceId().getUniqueId());
    }

}
