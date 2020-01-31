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
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public final class DefaultClusterNodeServer implements IClusterNodeServer {

    private final DefaultClusterNodeServerProvider provider;

    private volatile NetworkClusterNodeInfoSnapshot nodeInfoSnapshot;

    private NetworkClusterNode nodeInfo;

    private INetworkChannel channel;

    DefaultClusterNodeServer(DefaultClusterNodeServerProvider provider, NetworkClusterNode nodeInfo) {
        this.provider = provider;
        this.nodeInfo = nodeInfo;
    }

    @Override
    public void sendClusterChannelMessage(@NotNull String channel, @NotNull String message, @NotNull JsonDocument header, byte[] body) {
        saveSendPacket(new PacketServerClusterChannelMessage(channel, message, header, body));
    }

    @Override
    public void sendCustomChannelMessage(@NotNull String channel, @NotNull String message, @NotNull JsonDocument data) {
        saveSendPacket(new PacketClientServerChannelMessage(channel, message, data));
    }

    @Override
    public boolean isConnected() {
        return this.channel != null;
    }

    @Override
    public void saveSendPacket(@NotNull IPacket packet) {
        if (this.channel != null) {
            this.channel.sendPacket(packet);
        }

    }

    @Override
    public boolean isAcceptableConnection(@NotNull INetworkChannel channel, @NotNull String nodeId) {
        return this.channel == null && this.nodeInfo.getUniqueId().equals(nodeId);
    }

    @Override
    public String[] sendCommandLine(@NotNull String commandLine) {
        if (this.channel != null) {
            try {
                return CloudNetDriver.getInstance().getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPI(channel,
                        new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "send_commandLine")
                                .append("commandLine", commandLine)
                        , new byte[0],
                        (Function<Pair<JsonDocument, byte[]>, String[]>) documentPair -> documentPair.getFirst().get("responseMessages", new TypeToken<String[]>() {
                        }.getType())).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException exception) {
                exception.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public void deployTemplateInCluster(@NotNull ServiceTemplate serviceTemplate, @NotNull byte[] zipResource) {
        this.saveSendPacket(new PacketServerDeployLocalTemplate(serviceTemplate, zipResource, true));
    }

    @Override
    public ServiceInfoSnapshot createCloudService(@NotNull ServiceTask serviceTask) {
        Validate.checkNotNull(serviceTask);

        if (this.channel != null) {
            try {
                ServiceTask clone = serviceTask.makeClone();
                clone.getAssociatedNodes().clear();
                clone.getAssociatedNodes().add(this.nodeInfo.getUniqueId());
                JsonDocument data = new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "create_CloudService_by_serviceTask").append("serviceTask", clone);
                return CloudNetDriver.getInstance().getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPI(this.channel,
                        data, new byte[0],
                        (Function<Pair<JsonDocument, byte[]>, ServiceInfoSnapshot>) documentPair -> documentPair.getFirst().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                        }.getType())).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException exception) {
                exception.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public ServiceInfoSnapshot createCloudService(@NotNull ServiceConfiguration serviceConfiguration) {
        Validate.checkNotNull(serviceConfiguration);

        if (this.channel != null) {
            try {
                return CloudNetDriver.getInstance().getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPI(this.channel,
                        new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "create_CloudService_by_serviceConfiguration").append("serviceConfiguration", serviceConfiguration), new byte[0],
                        (Function<Pair<JsonDocument, byte[]>, ServiceInfoSnapshot>) documentPair -> documentPair.getFirst().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                        }.getType())).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException exception) {
                exception.printStackTrace();
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
                return CloudNetDriver.getInstance().getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPI(this.channel,
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
            } catch (InterruptedException | ExecutionException | TimeoutException exception) {
                exception.printStackTrace();
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
                return CloudNetDriver.getInstance().getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPI(this.channel,
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
            } catch (InterruptedException | ExecutionException | TimeoutException exception) {
                exception.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public ServiceInfoSnapshot sendCommandLineToCloudService(@NotNull UUID uniqueId, @NotNull String commandLine) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(commandLine);

        if (this.channel != null) {
            try {
                return CloudNetDriver.getInstance().getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPI(this.channel,
                        new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "send_commandline_to_cloud_service")
                                .append("uniqueId", uniqueId)
                                .append("commandLine", commandLine)
                        , new byte[0],
                        (Function<Pair<JsonDocument, byte[]>, ServiceInfoSnapshot>) documentPair -> documentPair.getFirst().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                        }.getType())).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException exception) {
                exception.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public ServiceInfoSnapshot addServiceTemplateToCloudService(@NotNull UUID uniqueId, @NotNull ServiceTemplate serviceTemplate) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(serviceTemplate);

        if (this.channel != null) {
            try {
                return CloudNetDriver.getInstance().getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPI(this.channel,
                        new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "add_service_template_to_cloud_service")
                                .append("uniqueId", uniqueId)
                                .append("serviceTemplate", serviceTemplate)
                        , new byte[0],
                        (Function<Pair<JsonDocument, byte[]>, ServiceInfoSnapshot>) documentPair -> documentPair.getFirst().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                        }.getType())).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException exception) {
                exception.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public ServiceInfoSnapshot addServiceRemoteInclusionToCloudService(@NotNull UUID uniqueId, @NotNull ServiceRemoteInclusion serviceRemoteInclusion) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(serviceRemoteInclusion);

        if (this.channel != null) {
            try {
                return CloudNetDriver.getInstance().getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPI(this.channel,
                        new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "add_service_remote_inclusion_to_cloud_service")
                                .append("uniqueId", uniqueId)
                                .append("serviceRemoteInclusion", serviceRemoteInclusion)
                        , new byte[0],
                        (Function<Pair<JsonDocument, byte[]>, ServiceInfoSnapshot>) documentPair -> documentPair.getFirst().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                        }.getType())).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException exception) {
                exception.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public ServiceInfoSnapshot addServiceDeploymentToCloudService(@NotNull UUID uniqueId, @NotNull ServiceDeployment serviceDeployment) {
        Validate.checkNotNull(uniqueId);
        Validate.checkNotNull(serviceDeployment);

        if (this.channel != null) {
            try {
                return CloudNetDriver.getInstance().getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPI(this.channel,
                        new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "add_service_deployment_to_cloud_service")
                                .append("uniqueId", uniqueId)
                                .append("serviceDeployment", serviceDeployment)
                        , new byte[0],
                        (Function<Pair<JsonDocument, byte[]>, ServiceInfoSnapshot>) documentPair -> documentPair.getFirst().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                        }.getType())).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException exception) {
                exception.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public Queue<String> getCachedLogMessagesFromService(@NotNull UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        if (this.channel != null) {
            try {
                return CloudNetDriver.getInstance().getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPI(this.channel,
                        new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "get_cached_log_messages_from_service")
                                .append("uniqueId", uniqueId)
                        , new byte[0],
                        (Function<Pair<JsonDocument, byte[]>, Queue<String>>) documentPair -> documentPair.getFirst().get("cachedLogMessages", new TypeToken<Queue<String>>() {
                        }.getType())).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException exception) {
                exception.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public void setCloudServiceLifeCycle(@NotNull ServiceInfoSnapshot serviceInfoSnapshot, @NotNull ServiceLifeCycle lifeCycle) {
        Validate.checkNotNull(serviceInfoSnapshot);
        Validate.checkNotNull(lifeCycle);

        if (this.channel != null) {
            try {
                CloudNetDriver.getInstance().getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPI(this.channel,
                        new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "set_service_life_cycle")
                                .append("uniqueId", serviceInfoSnapshot.getServiceId().getUniqueId()).append("lifeCycle", lifeCycle)
                        , new byte[0],
                        (Function<Pair<JsonDocument, byte[]>, Void>) documentPair -> null).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException exception) {
                exception.printStackTrace();
            }
        }
    }

    @Override
    public void restartCloudService(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
        Validate.checkNotNull(serviceInfoSnapshot);

        if (this.channel != null) {
            try {
                CloudNetDriver.getInstance().getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPI(this.channel,
                        new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "restart_cloud_service")
                                .append("uniqueId", serviceInfoSnapshot.getServiceId().getUniqueId())
                        , new byte[0],
                        (Function<Pair<JsonDocument, byte[]>, Void>) documentPair -> null).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException exception) {
                exception.printStackTrace();
            }
        }
    }

    @Override
    public void killCloudService(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
        Validate.checkNotNull(serviceInfoSnapshot);

        if (this.channel != null) {
            try {
                CloudNetDriver.getInstance().getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPI(
                        this.channel,
                        new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "kill_cloud_service").append("uniqueId", serviceInfoSnapshot.getServiceId().getUniqueId()),
                        new byte[0],
                        documentPair -> null).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException exception) {
                exception.printStackTrace();
            }
        }
    }

    @Override
    public void runCommand(@NotNull ServiceInfoSnapshot serviceInfoSnapshot, @NotNull String command) {
        Validate.checkNotNull(serviceInfoSnapshot);
        Validate.checkNotNull(command);

        if (this.channel != null) {
            try {
                CloudNetDriver.getInstance().getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPI(
                        this.channel,
                        new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "run_command_cloud_service").append("uniqueId", serviceInfoSnapshot.getServiceId().getUniqueId())
                                .append("command", command),
                        new byte[0],
                        documentPair -> null).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException exception) {
                exception.printStackTrace();
            }
        }
    }

    @Override
    public void includeWaitingServiceInclusions(@NotNull UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        if (this.channel != null) {
            try {
                CloudNetDriver.getInstance().getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPI(this.channel,
                        new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "include_all_waiting_service_inclusions")
                                .append("uniqueId", uniqueId)
                        , new byte[0],
                        (Function<Pair<JsonDocument, byte[]>, Void>) documentPair -> null).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException exception) {
                exception.printStackTrace();
            }
        }
    }

    @Override
    public void includeWaitingServiceTemplates(@NotNull UUID uniqueId) {
        Validate.checkNotNull(uniqueId);

        if (this.channel != null) {
            try {
                CloudNetDriver.getInstance().getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPI(this.channel,
                        new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "include_all_waiting_service_templates")
                                .append("uniqueId", uniqueId)
                        , new byte[0],
                        (Function<Pair<JsonDocument, byte[]>, Void>) documentPair -> null).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException exception) {
                exception.printStackTrace();
            }
        }
    }

    @Override
    public void deployResources(@NotNull UUID uniqueId, boolean removeDeployments) {
        Validate.checkNotNull(uniqueId);

        if (this.channel != null) {
            try {
                CloudNetDriver.getInstance().getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPI(this.channel,
                        new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "deploy_resources_from_service")
                                .append("uniqueId", uniqueId).append("removeDeployments", removeDeployments)
                        , new byte[0],
                        (Function<Pair<JsonDocument, byte[]>, Void>) documentPair -> null).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException exception) {
                exception.printStackTrace();
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

    @NotNull
    public DefaultClusterNodeServerProvider getProvider() {
        return this.provider;
    }

    @NotNull
    public NetworkClusterNodeInfoSnapshot getNodeInfoSnapshot() {
        return this.nodeInfoSnapshot;
    }

    public void setNodeInfoSnapshot(@NotNull NetworkClusterNodeInfoSnapshot nodeInfoSnapshot) {
        this.nodeInfoSnapshot = nodeInfoSnapshot;
    }

    @NotNull
    public NetworkClusterNode getNodeInfo() {
        return this.nodeInfo;
    }

    public void setNodeInfo(@NotNull NetworkClusterNode nodeInfo) {
        this.nodeInfo = nodeInfo;
    }

    public INetworkChannel getChannel() {
        return this.channel;
    }

    public void setChannel(@NotNull INetworkChannel channel) {
        this.channel = channel;
    }
}