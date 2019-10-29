package de.dytanic.cloudnet.cluster;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerChannelMessage;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.service.*;
import de.dytanic.cloudnet.network.packet.PacketServerClusterChannelMessage;
import de.dytanic.cloudnet.network.packet.PacketServerDeployLocalTemplate;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public final class DefaultClusterNodeServer implements IClusterNodeServer {

    private static final Type TYPE_COLLECTION_INTEGER = new TypeToken<Collection<Integer>>() {
    }.getType();

    private final DefaultClusterNodeServerProvider provider;


    private volatile NetworkClusterNodeInfoSnapshot nodeInfoSnapshot;

    private NetworkClusterNode nodeInfo;

    private INetworkChannel channel;

    DefaultClusterNodeServer(DefaultClusterNodeServerProvider provider, NetworkClusterNode nodeInfo) {
        this.provider = provider;
        this.nodeInfo = nodeInfo;
    }

    @Override
    public void sendClusterChannelMessage(String channel, String message, JsonDocument header, byte[] body) {
        saveSendPacket(new PacketServerClusterChannelMessage(channel, message, header, body));
    }

    @Override
    public void sendCustomChannelMessage(String channel, String message, JsonDocument data) {
        Validate.checkNotNull(channel);
        Validate.checkNotNull(message);
        Validate.checkNotNull(data);

        saveSendPacket(new PacketClientServerChannelMessage(channel, message, data));
    }

    @Override
    public boolean isConnected() {
        return this.channel != null;
    }

    @Override
    public void saveSendPacket(IPacket packet) {
        Validate.checkNotNull(packet);

        if (this.channel != null) {
            this.channel.sendPacket(packet);
        }

    }

    @Override
    public boolean isAcceptableConnection(INetworkChannel channel, String nodeId) {
        return channel != null && this.channel == null && this.nodeInfo.getUniqueId().equals(nodeId);
    }

    @Override
    public String[] sendCommandLine(String commandLine) {
        Validate.checkNotNull(commandLine);

        if (this.channel != null) {
            try {
                return CloudNetDriver.getInstance().sendCallablePacketWithAsDriverSyncAPI(channel,
                        new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "send_commandLine")
                                .append("commandLine", commandLine)
                        , new byte[0],
                        (Function<Pair<JsonDocument, byte[]>, String[]>) documentPair -> documentPair.getFirst().get("responseMessages", new TypeToken<String[]>() {
                        }.getType())).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public void deployTemplateInCluster(ServiceTemplate serviceTemplate, byte[] zipResource) {
        this.saveSendPacket(new PacketServerDeployLocalTemplate(serviceTemplate, zipResource, true));
    }

    @Override
    public ServiceInfoSnapshot createCloudService(ServiceTask serviceTask) {
        Validate.checkNotNull(serviceTask);

        if (this.channel != null) {
            try {
                ServiceTask clone = serviceTask.makeClone();
                clone.getAssociatedNodes().clear();
                clone.getAssociatedNodes().add(this.nodeInfo.getUniqueId());
                JsonDocument data = new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "create_CloudService_by_serviceTask").append("serviceTask", clone);
                return CloudNetDriver.getInstance().sendCallablePacketWithAsDriverSyncAPI(this.channel,
                        data, new byte[0],
                        (Function<Pair<JsonDocument, byte[]>, ServiceInfoSnapshot>) documentPair -> documentPair.getFirst().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                        }.getType())).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public ServiceInfoSnapshot createCloudService(ServiceConfiguration serviceConfiguration) {
        Validate.checkNotNull(serviceConfiguration);

        if (this.channel != null) {
            try {
                return CloudNetDriver.getInstance().sendCallablePacketWithAsDriverSyncAPI(this.channel,
                        new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "create_CloudService_by_serviceConfiguration").append("serviceConfiguration", serviceConfiguration), new byte[0],
                        (Function<Pair<JsonDocument, byte[]>, ServiceInfoSnapshot>) documentPair -> documentPair.getFirst().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                        }.getType())).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public ServiceInfoSnapshot createCloudService(
            String name, String runtime, boolean autoDeleteOnStop, boolean staticService,
            Collection<ServiceRemoteInclusion> includes,
            Collection<ServiceTemplate> templates,
            Collection<ServiceDeployment> deployments,
            Collection<String> groups,
            ProcessConfiguration processConfiguration,
            JsonDocument properties, Integer port) {
        Validate.checkNotNull(name);
        Validate.checkNotNull(includes);
        Validate.checkNotNull(templates);
        Validate.checkNotNull(deployments);
        Validate.checkNotNull(groups);
        Validate.checkNotNull(processConfiguration);

        if (this.channel != null) {
            try {
                return CloudNetDriver.getInstance().sendCallablePacketWithAsDriverSyncAPI(this.channel,
                        new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "create_cloud_service_custom")
                                .append("name", name)
                                .append("runtime", runtime)
                                .append("autoDeleteOnStop", autoDeleteOnStop)
                                .append("staticService", staticService)
                                .append("includes", includes)
                                .append("templates", templates)
                                .append("deployments", deployments)
                                .append("groups", groups)
                                .append("processConfiguration", processConfiguration)
                                .append("properties", properties)
                                .append("port", port),
                        new byte[0],
                        (Function<Pair<JsonDocument, byte[]>, ServiceInfoSnapshot>) documentPair -> documentPair.getFirst().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                        }.getType())).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public Collection<ServiceInfoSnapshot> createCloudService(
            String nodeUniqueId, int amount, String name, String runtime, boolean autoDeleteOnStop, boolean staticService,
            Collection<ServiceRemoteInclusion> includes,
            Collection<ServiceTemplate> templates,
            Collection<ServiceDeployment> deployments,
            Collection<String> groups,
            ProcessConfiguration processConfiguration,
            JsonDocument properties, Integer port) {
        Validate.checkNotNull(nodeUniqueId);
        Validate.checkNotNull(name);
        Validate.checkNotNull(includes);
        Validate.checkNotNull(templates);
        Validate.checkNotNull(deployments);
        Validate.checkNotNull(groups);
        Validate.checkNotNull(processConfiguration);

        if (this.channel != null) {
            try {
                return CloudNetDriver.getInstance().sendCallablePacketWithAsDriverSyncAPI(this.channel,
                        new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "create_cloud_service_custom_selected_node_and_amount")
                                .append("nodeUniqueId", nodeUniqueId)
                                .append("amount", amount)
                                .append("name", name)
                                .append("runtime", runtime)
                                .append("autoDeleteOnStop", autoDeleteOnStop)
                                .append("staticService", staticService)
                                .append("includes", includes)
                                .append("templates", templates)
                                .append("deployments", deployments)
                                .append("groups", groups)
                                .append("processConfiguration", processConfiguration)
                                .append("properties", properties)
                                .append("port", port),
                        new byte[0],
                        (Function<Pair<JsonDocument, byte[]>, Collection<ServiceInfoSnapshot>>) documentPair -> documentPair.getFirst().get("serviceInfoSnapshots", new TypeToken<Collection<ServiceInfoSnapshot>>() {
                        }.getType())).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public ServiceInfoSnapshot sendCommandLineToCloudService(UUID uniqueId, String commandLine) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(commandLine);

        if (this.channel != null) {
            try {
                return CloudNetDriver.getInstance().sendCallablePacketWithAsDriverSyncAPI(this.channel,
                        new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "send_commandline_to_cloud_service")
                                .append("uniqueId", uniqueId)
                                .append("commandLine", commandLine)
                        , new byte[0],
                        (Function<Pair<JsonDocument, byte[]>, ServiceInfoSnapshot>) documentPair -> documentPair.getFirst().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                        }.getType())).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public ServiceInfoSnapshot addServiceTemplateToCloudService(UUID uniqueId, ServiceTemplate serviceTemplate) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(serviceTemplate);

        if (this.channel != null) {
            try {
                return CloudNetDriver.getInstance().sendCallablePacketWithAsDriverSyncAPI(this.channel,
                        new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "add_service_template_to_cloud_service")
                                .append("uniqueId", uniqueId)
                                .append("serviceTemplate", serviceTemplate)
                        , new byte[0],
                        (Function<Pair<JsonDocument, byte[]>, ServiceInfoSnapshot>) documentPair -> documentPair.getFirst().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                        }.getType())).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public ServiceInfoSnapshot addServiceRemoteInclusionToCloudService(UUID uniqueId, ServiceRemoteInclusion serviceRemoteInclusion) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(serviceRemoteInclusion);

        if (this.channel != null) {
            try {
                return CloudNetDriver.getInstance().sendCallablePacketWithAsDriverSyncAPI(this.channel,
                        new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "add_service_remote_inclusion_to_cloud_service")
                                .append("uniqueId", uniqueId)
                                .append("serviceRemoteInclusion", serviceRemoteInclusion)
                        , new byte[0],
                        (Function<Pair<JsonDocument, byte[]>, ServiceInfoSnapshot>) documentPair -> documentPair.getFirst().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                        }.getType())).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public ServiceInfoSnapshot addServiceDeploymentToCloudService(UUID uniqueId, ServiceDeployment serviceDeployment) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(serviceDeployment);

        if (this.channel != null) {
            try {
                return CloudNetDriver.getInstance().sendCallablePacketWithAsDriverSyncAPI(this.channel,
                        new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "add_service_deployment_to_cloud_service")
                                .append("uniqueId", uniqueId)
                                .append("serviceDeployment", serviceDeployment)
                        , new byte[0],
                        (Function<Pair<JsonDocument, byte[]>, ServiceInfoSnapshot>) documentPair -> documentPair.getFirst().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                        }.getType())).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public Queue<String> getCachedLogMessagesFromService(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        if (this.channel != null) {
            try {
                return CloudNetDriver.getInstance().sendCallablePacketWithAsDriverSyncAPI(this.channel,
                        new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_cached_log_messages_from_service")
                                .append("uniqueId", uniqueId)
                        , new byte[0],
                        (Function<Pair<JsonDocument, byte[]>, Queue<String>>) documentPair -> documentPair.getFirst().get("cachedLogMessages", new TypeToken<Queue<String>>() {
                        }.getType())).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public void setCloudServiceLifeCycle(ServiceInfoSnapshot serviceInfoSnapshot, ServiceLifeCycle lifeCycle) {
        Validate.checkNotNull(serviceInfoSnapshot);
        Validate.checkNotNull(lifeCycle);

        if (this.channel != null) {
            try {
                CloudNetDriver.getInstance().sendCallablePacketWithAsDriverSyncAPI(this.channel,
                        new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "set_service_life_cycle")
                                .append("serviceInfoSnapshot", serviceInfoSnapshot).append("lifeCycle", lifeCycle)
                        , new byte[0],
                        (Function<Pair<JsonDocument, byte[]>, Void>) documentPair -> null).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void restartCloudService(ServiceInfoSnapshot serviceInfoSnapshot) {
        Validate.checkNotNull(serviceInfoSnapshot);

        if (this.channel != null) {
            try {
                CloudNetDriver.getInstance().sendCallablePacketWithAsDriverSyncAPI(this.channel,
                        new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "restart_cloud_service")
                                .append("serviceInfoSnapshot", serviceInfoSnapshot)
                        , new byte[0],
                        (Function<Pair<JsonDocument, byte[]>, Void>) documentPair -> null).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void killCloudService(ServiceInfoSnapshot serviceInfoSnapshot) {
        Validate.checkNotNull(serviceInfoSnapshot);

        if (this.channel != null) {
            try {
                CloudNetDriver.getInstance().sendCallablePacketWithAsDriverSyncAPI(
                        this.channel,
                        new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "kill_cloud_service").append("serviceInfoSnapshot", serviceInfoSnapshot),
                        new byte[0],
                        documentPair -> null).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void runCommand(ServiceInfoSnapshot serviceInfoSnapshot, String command) {
        Validate.checkNotNull(serviceInfoSnapshot);
        Validate.checkNotNull(command);

        if (this.channel != null) {
            try {
                CloudNetDriver.getInstance().sendCallablePacketWithAsDriverSyncAPI(
                        this.channel,
                        new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "run_command_cloud_service").append("serviceInfoSnapshot", serviceInfoSnapshot)
                                .append("command", command),
                        new byte[0],
                        documentPair -> null).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void includeWaitingServiceInclusions(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        if (this.channel != null) {
            try {
                CloudNetDriver.getInstance().sendCallablePacketWithAsDriverSyncAPI(this.channel,
                        new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "include_all_waiting_service_inclusions")
                                .append("uniqueId", uniqueId)
                        , new byte[0],
                        (Function<Pair<JsonDocument, byte[]>, Void>) documentPair -> null).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void includeWaitingServiceTemplates(UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        if (this.channel != null) {
            try {
                CloudNetDriver.getInstance().sendCallablePacketWithAsDriverSyncAPI(this.channel,
                        new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "include_all_waiting_service_templates")
                                .append("uniqueId", uniqueId)
                        , new byte[0],
                        (Function<Pair<JsonDocument, byte[]>, Void>) documentPair -> null).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void deployResources(UUID uniqueId, boolean removeDeployments) {
        Validate.checkNotNull(uniqueId);

        if (this.channel != null) {
            try {
                CloudNetDriver.getInstance().sendCallablePacketWithAsDriverSyncAPI(this.channel,
                        new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "deploy_resources_from_service")
                                .append("uniqueId", uniqueId).append("removeDeployments", removeDeployments)
                        , new byte[0],
                        (Function<Pair<JsonDocument, byte[]>, Void>) documentPair -> null).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() throws Exception {
        if (this.channel != null) {
            this.channel.close();
        }

        this.nodeInfoSnapshot = null;
        this.channel = null;
    }

    public DefaultClusterNodeServerProvider getProvider() {
        return this.provider;
    }

    public NetworkClusterNodeInfoSnapshot getNodeInfoSnapshot() {
        return this.nodeInfoSnapshot;
    }

    public void setNodeInfoSnapshot(NetworkClusterNodeInfoSnapshot nodeInfoSnapshot) {
        this.nodeInfoSnapshot = nodeInfoSnapshot;
    }

    public NetworkClusterNode getNodeInfo() {
        return this.nodeInfo;
    }

    public void setNodeInfo(NetworkClusterNode nodeInfo) {
        this.nodeInfo = nodeInfo;
    }

    public INetworkChannel getChannel() {
        return this.channel;
    }

    public void setChannel(INetworkChannel channel) {
        this.channel = channel;
    }
}