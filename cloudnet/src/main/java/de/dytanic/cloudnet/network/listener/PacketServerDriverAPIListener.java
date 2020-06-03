package de.dytanic.cloudnet.network.listener;

import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.api.ServiceDriverAPIResponse;
import de.dytanic.cloudnet.driver.command.CommandInfo;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.permission.*;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Queue;
import java.util.UUID;
import java.util.function.Consumer;

public class PacketServerDriverAPIListener implements IPacketListener {
    @Override
    public void handle(INetworkChannel channel, IPacket packet) {
        ProtocolBuffer input = packet.getBuffer();
        DriverAPIRequestType requestType = input.readEnumConstant(DriverAPIRequestType.class);

        switch (requestType) {
            case FORCE_UPDATE_SERVICE: {
                this.getCloudServiceProvider(channel, packet, provider -> channel.sendPacket(Packet.createResponseFor(packet,
                        ProtocolBuffer.create()
                                .writeEnumConstant(ServiceDriverAPIResponse.SUCCESS)
                                .writeOptionalObject(provider.forceUpdateServiceInfo())
                )));
            }
            break;

            case SET_CLOUD_SERVICE_LIFE_CYCLE: {
                this.getCloudServiceProvider(channel, packet, provider -> {
                    provider.setCloudServiceLifeCycle(input.readEnumConstant(ServiceLifeCycle.class));
                    channel.sendPacket(this.createSuccessServiceResponseFor(packet));
                });
            }
            break;

            case ADD_PERMANENT_SERVICE_TASK: {
                CloudNetDriver.getInstance().getServiceTaskProvider().addPermanentServiceTask(input.readObject(ServiceTask.class));
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.EMPTY));
            }
            break;

            case REMOVE_PERMANENT_SERVICE_TASK: {
                CloudNetDriver.getInstance().getServiceTaskProvider().removePermanentServiceTask(input.readString());
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.EMPTY));
            }
            break;

            case ADD_GROUP_CONFIGURATION: {
                CloudNetDriver.getInstance().getGroupConfigurationProvider().addGroupConfiguration(input.readObject(GroupConfiguration.class));
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.EMPTY));
            }
            break;

            case REMOVE_GROUP_CONFIGURATION: {
                CloudNetDriver.getInstance().getGroupConfigurationProvider().removeGroupConfiguration(input.readString());
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.EMPTY));
            }
            break;

            case GET_CONSOLE_COMMANDS: {
                Collection<CommandInfo> infos = CloudNetDriver.getInstance().getNodeInfoProvider().getConsoleCommands();
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeObjectCollection(infos)));
            }
            break;

            case GET_CONSOLE_COMMAND_BY_LINE: {
                CommandInfo info = CloudNetDriver.getInstance().getNodeInfoProvider().getConsoleCommand(input.readString());
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeOptionalObject(info)));
            }
            break;

            case TAB_COMPLETE_CONSOLE_COMMAND: {
                Collection<String> results = CloudNetDriver.getInstance().getNodeInfoProvider().getConsoleTabCompleteResults(input.readString());
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeStringCollection(results)));
            }
            break;

            case SEND_COMMAND_LINE: {
                String[] results = CloudNetDriver.getInstance().getNodeInfoProvider().sendCommandLine(input.readString());
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeStringArray(results)));
            }
            break;

            case SEND_COMMAND_LINE_TO_NODE: {
                String[] results = CloudNetDriver.getInstance().getNodeInfoProvider().sendCommandLine(input.readString(), input.readString());
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeStringArray(results)));
            }
            break;

            case CREATE_CLOUD_SERVICE_BY_SERVICE_TASK: {
                ServiceInfoSnapshot snapshot = CloudNetDriver.getInstance().getCloudServiceFactory().createCloudService(input.readObject(ServiceTask.class));
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeOptionalObject(snapshot)));
            }
            break;

            case CREATE_CLOUD_SERVICE_BY_SERVICE_TASK_AND_ID: {
                ServiceInfoSnapshot snapshot = CloudNetDriver.getInstance().getCloudServiceFactory().createCloudService(input.readObject(ServiceTask.class), input.readInt());
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeOptionalObject(snapshot)));
            }
            break;

            case CREATE_CLOUD_SERVICE_BY_CONFIGURATION: {
                ServiceInfoSnapshot snapshot = CloudNetDriver.getInstance().getCloudServiceFactory().createCloudService(input.readObject(ServiceConfiguration.class));
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeOptionalObject(snapshot)));
            }
            break;

            case GET_CLOUD_SERVICES: {
                Collection<ServiceInfoSnapshot> snapshots = CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServices();
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeObjectCollection(snapshots)));
            }
            break;

            case GET_STARTED_CLOUD_SERVICES: {
                Collection<ServiceInfoSnapshot> snapshots = CloudNetDriver.getInstance().getCloudServiceProvider().getStartedCloudServices();
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeObjectCollection(snapshots)));
            }
            break;

            case GET_CLOUD_SERVICES_BY_SERVICE_TASK: {
                Collection<ServiceInfoSnapshot> snapshots = CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServices(input.readString());
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeObjectCollection(snapshots)));
            }
            break;

            case GET_CLOUD_SERVICES_BY_GROUP: {
                Collection<ServiceInfoSnapshot> snapshots = CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServicesByGroup(input.readString());
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeObjectCollection(snapshots)));
            }
            break;

            case GET_CLOUD_SERVICE_BY_UNIQUE_ID: {
                ServiceInfoSnapshot snapshot = CloudNetDriver.getInstance().getCloudServiceProvider().getCloudService(input.readUUID());
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeOptionalObject(snapshot)));
            }
            break;

            case GET_PERMANENT_SERVICE_TASKS: {
                Collection<ServiceTask> tasks = CloudNetDriver.getInstance().getServiceTaskProvider().getPermanentServiceTasks();
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeObjectCollection(tasks)));
            }
            break;

            case GET_PERMANENT_SERVICE_TASK_BY_NAME: {
                ServiceTask task = CloudNetDriver.getInstance().getServiceTaskProvider().getServiceTask(input.readString());
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeOptionalObject(task)));
            }
            break;

            case IS_SERVICE_TASK_PRESENT: {
                boolean present = CloudNetDriver.getInstance().getServiceTaskProvider().isServiceTaskPresent(input.readString());
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeBoolean(present)));
            }
            break;

            case GET_GROUP_CONFIGURATIONS: {
                Collection<GroupConfiguration> groups = CloudNetDriver.getInstance().getGroupConfigurationProvider().getGroupConfigurations();
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeObjectCollection(groups)));
            }
            break;

            case GET_GROUP_CONFIGURATION_BY_NAME: {
                GroupConfiguration group = CloudNetDriver.getInstance().getGroupConfigurationProvider().getGroupConfiguration(input.readString());
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeOptionalObject(group)));
            }
            break;

            case IS_GROUP_CONFIGURATION_PRESENT: {
                boolean present = CloudNetDriver.getInstance().getGroupConfigurationProvider().isGroupConfigurationPresent(input.readString());
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeBoolean(present)));
            }
            break;

            case GET_NODES: {
                NetworkClusterNode[] nodes = CloudNetDriver.getInstance().getNodeInfoProvider().getNodes();
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeObjectArray(nodes)));
            }
            break;

            case GET_NODE_BY_UNIQUE_ID: {
                NetworkClusterNode node = CloudNetDriver.getInstance().getNodeInfoProvider().getNode(input.readString());
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeOptionalObject(node)));
            }
            break;

            case GET_NODE_INFO_SNAPSHOTS: {
                NetworkClusterNodeInfoSnapshot[] snapshots = CloudNetDriver.getInstance().getNodeInfoProvider().getNodeInfoSnapshots();
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeObjectArray(snapshots)));
            }
            break;

            case GET_NODE_INFO_SNAPSHOT_BY_UNIQUE_ID: {
                NetworkClusterNodeInfoSnapshot snapshot = CloudNetDriver.getInstance().getNodeInfoProvider().getNodeInfoSnapshot(input.readString());
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeOptionalObject(snapshot)));
            }
            break;

            case RESTART_CLOUD_SERVICE: {
                this.getCloudServiceProvider(channel, packet, provider -> {
                    provider.restart();
                    channel.sendPacket(this.createSuccessServiceResponseFor(packet));
                });
            }
            break;

            case CREATE_CUSTOM_CLOUD_SERVICE: {
                String name = input.readString();
                String runtime = input.readString();
                boolean autoDeleteOnStop = input.readBoolean();
                boolean staticService = input.readBoolean();
                Collection<ServiceRemoteInclusion> includes = input.readObjectCollection(ServiceRemoteInclusion.class);
                Collection<ServiceTemplate> templates = input.readObjectCollection(ServiceTemplate.class);
                Collection<ServiceDeployment> deployments = input.readObjectCollection(ServiceDeployment.class);
                Collection<String> groups = input.readStringCollection();
                ProcessConfiguration processConfiguration = input.readObject(ProcessConfiguration.class);
                JsonDocument properties = input.readJsonDocument();
                int port = input.readInt();

                ServiceInfoSnapshot snapshot = CloudNetDriver.getInstance().getCloudServiceFactory().createCloudService(
                        name, runtime,
                        autoDeleteOnStop, staticService,
                        includes, templates, deployments,
                        groups, processConfiguration,
                        properties, port
                );

                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeOptionalObject(snapshot)));
            }
            break;

            case CREATE_CUSTOM_CLOUD_SERVICE_WITH_NODE_AND_AMOUNT: {
                String nodeUniqueId = input.readString();
                int amount = input.readInt();
                String name = input.readString();
                String runtime = input.readString();
                boolean autoDeleteOnStop = input.readBoolean();
                boolean staticService = input.readBoolean();
                Collection<ServiceRemoteInclusion> includes = input.readObjectCollection(ServiceRemoteInclusion.class);
                Collection<ServiceTemplate> templates = input.readObjectCollection(ServiceTemplate.class);
                Collection<ServiceDeployment> deployments = input.readObjectCollection(ServiceDeployment.class);
                Collection<String> groups = input.readStringCollection();
                ProcessConfiguration processConfiguration = input.readObject(ProcessConfiguration.class);
                JsonDocument properties = input.readJsonDocument();
                int port = input.readInt();

                Collection<ServiceInfoSnapshot> snapshots = CloudNetDriver.getInstance().getCloudServiceFactory().createCloudService(
                        nodeUniqueId, amount,
                        name, runtime,
                        autoDeleteOnStop, staticService,
                        includes, templates, deployments,
                        groups, processConfiguration,
                        properties, port
                );

                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeObjectCollection(snapshots != null ? snapshots : Collections.emptyList())));
            }
            break;

            case RUN_COMMAND_ON_CLOUD_SERVICE: {
                this.getCloudServiceProvider(channel, packet, provider -> {
                    provider.runCommand(input.readString());
                    channel.sendPacket(this.createSuccessServiceResponseFor(packet));
                });
            }
            break;

            case ADD_SERVICE_TEMPLATE_TO_CLOUD_SERVICE: {
                this.getCloudServiceProvider(channel, packet, provider -> {
                    provider.addServiceTemplate(input.readObject(ServiceTemplate.class));
                    channel.sendPacket(this.createSuccessServiceResponseFor(packet));
                });
            }
            break;

            case ADD_SERVICE_REMOTE_INCLUSION_TO_CLOUD_SERVICE: {
                this.getCloudServiceProvider(channel, packet, provider -> {
                    provider.addServiceRemoteInclusion(input.readObject(ServiceRemoteInclusion.class));
                    channel.sendPacket(this.createSuccessServiceResponseFor(packet));
                });
            }
            break;

            case ADD_SERVICE_DEPLOYMENT_TO_CLOUD_SERVICE: {
                this.getCloudServiceProvider(channel, packet, provider -> {
                    provider.addServiceDeployment(input.readObject(ServiceDeployment.class));
                    channel.sendPacket(this.createSuccessServiceResponseFor(packet));
                });
            }
            break;

            case INCLUDE_WAITING_TEMPLATES_ON_CLOUD_SERVICE: {
                this.getCloudServiceProvider(channel, packet, provider -> {
                    provider.includeWaitingServiceTemplates();
                    channel.sendPacket(this.createSuccessServiceResponseFor(packet));
                });
            }
            break;

            case INCLUDE_WAITING_INCLUSIONS_ON_CLOUD_SERVICE: {
                this.getCloudServiceProvider(channel, packet, provider -> {
                    provider.includeWaitingServiceInclusions();
                    channel.sendPacket(this.createSuccessServiceResponseFor(packet));
                });
            }
            break;

            case DEPLOY_RESOURCES_ON_CLOUD_SERVICE: {
                this.getCloudServiceProvider(channel, packet, provider -> {
                    provider.deployResources(input.readBoolean());
                    channel.sendPacket(this.createSuccessServiceResponseFor(packet));
                });
            }
            break;

            case GET_SERVICES_AS_UNIQUE_ID: {
                Collection<UUID> uuids = CloudNetDriver.getInstance().getCloudServiceProvider().getServicesAsUniqueId();
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeUUIDCollection(uuids)));
            }
            break;

            case GET_SERVICES_COUNT: {
                int count = CloudNetDriver.getInstance().getCloudServiceProvider().getServicesCount();
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeInt(count)));
            }
            break;

            case GET_SERVICES_COUNT_BY_GROUP: {
                int count = CloudNetDriver.getInstance().getCloudServiceProvider().getServicesCountByGroup(input.readString());
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeInt(count)));
            }
            break;

            case GET_SERVICES_COUNT_BY_TASK: {
                int count = CloudNetDriver.getInstance().getCloudServiceProvider().getServicesCountByTask(input.readString());
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeInt(count)));
            }
            break;

            case GET_TEMPLATE_STORAGE_TEMPLATES: {
                Collection<ServiceTemplate> templates = CloudNetDriver.getInstance().getTemplateStorageTemplates(input.readString());
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeObjectCollection(templates)));
            }
            break;

            case KILL_CLOUD_SERVICE: {
                this.getCloudServiceProvider(channel, packet, provider -> {
                    provider.kill();
                    channel.sendPacket(this.createSuccessServiceResponseFor(packet));
                });
            }
            break;

            case GET_CACHED_LOG_MESSAGES_FROM_CLOUD_SERVICE: {
                this.getCloudServiceProvider(channel, packet, provider -> {
                    Queue<String> messages = provider.getCachedLogMessages();
                    channel.sendPacket(this.createSuccessServiceResponseFor(packet, buffer -> buffer.writeStringCollection(messages)));
                });
            }
            break;

            case PERMISSION_MANAGEMENT_RELOAD: {
                boolean success = this.permissionManagement().reload();
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeBoolean(success)));
            }
            break;

            case PERMISSION_MANAGEMENT_ADD_USER: {
                IPermissionUser user = input.readObject(PermissionUser.class);
                IPermissionUser response = this.permissionManagement().addUser(user);
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeObject(response)));
            }
            break;

            case PERMISSION_MANAGEMENT_UPDATE_USER: {
                IPermissionUser user = input.readObject(PermissionUser.class);
                this.permissionManagement().updateUser(user);
                channel.sendPacket(Packet.createResponseFor(packet));
            }
            break;

            case PERMISSION_MANAGEMENT_DELETE_USERS_BY_NAME: {
                String name = input.readString();
                boolean success = this.permissionManagement().deleteUser(name);
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeBoolean(success)));
            }
            break;

            case PERMISSION_MANAGEMENT_DELETE_USER: {
                IPermissionUser user = input.readObject(PermissionUser.class);
                boolean success = this.permissionManagement().deleteUser(user);
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeBoolean(success)));
            }
            break;

            case PERMISSION_MANAGEMENT_SET_USERS: {
                Collection<? extends IPermissionUser> users = input.readObjectCollection(PermissionUser.class);
                this.permissionManagement().setUsers(users);
                channel.sendPacket(Packet.createResponseFor(packet));
            }
            break;

            case PERMISSION_MANAGEMENT_ADD_GROUP: {
                IPermissionGroup group = input.readObject(PermissionGroup.class);
                IPermissionGroup response = this.permissionManagement().addGroup(group);
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeObject(response)));
            }
            break;

            case PERMISSION_MANAGEMENT_UPDATE_GROUP: {
                IPermissionGroup group = input.readObject(PermissionGroup.class);
                this.permissionManagement().updateGroup(group);
                channel.sendPacket(Packet.createResponseFor(packet));
            }
            break;

            case PERMISSION_MANAGEMENT_DELETE_GROUP_BY_NAME: {
                String name = input.readString();
                this.permissionManagement().deleteGroup(name);
                channel.sendPacket(Packet.createResponseFor(packet));
            }
            break;

            case PERMISSION_MANAGEMENT_DELETE_GROUP: {
                IPermissionGroup group = input.readObject(PermissionGroup.class);
                this.permissionManagement().deleteGroup(group);
                channel.sendPacket(Packet.createResponseFor(packet));
            }
            break;

            case PERMISSION_MANAGEMENT_SET_GROUPS: {
                Collection<? extends IPermissionGroup> groups = input.readObjectCollection(PermissionGroup.class);
                this.permissionManagement().setGroups(groups);
                channel.sendPacket(Packet.createResponseFor(packet));
            }
            break;

            case GET_CLOUD_SERVICES_BY_ENVIRONMENT: {
                ServiceEnvironmentType environment = input.readEnumConstant(ServiceEnvironmentType.class);
                Collection<ServiceInfoSnapshot> snapshots = CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServices(environment);
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeObjectCollection(snapshots)));
            }
            break;

            case SEND_COMMAND_LINE_AS_PERMISSION_USER: {
                Pair<Boolean, String[]> response = CloudNetDriver.getInstance().sendCommandLineAsPermissionUser(input.readUUID(), input.readString());
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeBoolean(response.getFirst()).writeStringArray(response.getSecond())));
            }
            break;

            case PERMISSION_MANAGEMENT_CONTAINS_USER_BY_UNIQUE_ID: {
                boolean contains = this.permissionManagement().containsUser(input.readUUID());
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeBoolean(contains)));
            }
            break;

            case PERMISSION_MANAGEMENT_CONTAINS_USER_BY_NAME: {
                boolean contains = this.permissionManagement().containsUser(input.readString());
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeBoolean(contains)));
            }
            break;

            case PERMISSION_MANAGEMENT_GET_USER_BY_UNIQUE_ID: {
                IPermissionUser user = this.permissionManagement().getUser(input.readUUID());
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeOptionalObject(user)));
            }
            break;

            case PERMISSION_MANAGEMENT_GET_USERS_BY_NAME: {
                Collection<IPermissionUser> users = this.permissionManagement().getUsers(input.readString());
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeObjectCollection(users)));
            }
            break;

            case PERMISSION_MANAGEMENT_GET_FIRST_USER_BY_NAME: {
                IPermissionUser user = this.permissionManagement().getFirstUser(input.readString());
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeOptionalObject(user)));
            }
            break;

            case PERMISSION_MANAGEMENT_GET_USERS: {
                Collection<IPermissionUser> users = this.permissionManagement().getUsers();
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeObjectCollection(users)));
            }
            break;

            case PERMISSION_MANAGEMENT_GET_USERS_BY_GROUP: {
                Collection<IPermissionUser> users = this.permissionManagement().getUsersByGroup(input.readString());
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeObjectCollection(users)));
            }
            break;

            case PERMISSION_MANAGEMENT_CONTAINS_GROUP: {
                boolean contains = this.permissionManagement().containsGroup(input.readString());
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeBoolean(contains)));
            }
            break;

            case PERMISSION_MANAGEMENT_GET_GROUP_BY_NAME: {
                IPermissionGroup group = this.permissionManagement().getGroup(input.readString());
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeOptionalObject(group)));
            }
            break;

            case PERMISSION_MANAGEMENT_GET_GROUPS: {
                Collection<IPermissionGroup> groups = this.permissionManagement().getGroups();
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeObjectCollection(groups)));
            }
            break;

            case PERMISSION_MANAGEMENT_GET_DEFAULT_GROUP: {
                IPermissionGroup group = this.permissionManagement().getDefaultPermissionGroup();
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeOptionalObject(group)));
            }
            break;

            case GET_CLOUD_SERVICE_BY_NAME: {
                ServiceInfoSnapshot snapshot = CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServiceByName(input.readString());
                channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeOptionalObject(snapshot)));
            }
            break;
        }

    }

    private void getCloudServiceProvider(INetworkChannel channel, IPacket packet, Consumer<SpecificCloudServiceProvider> consumer) {
        UUID uniqueId = packet.getBuffer().readOptionalUUID();
        String name = packet.getBuffer().readOptionalString();
        SpecificCloudServiceProvider provider;
        if (uniqueId != null) {
            provider = CloudNetDriver.getInstance().getCloudServiceProvider(uniqueId);
        } else if (name != null) {
            provider = CloudNetDriver.getInstance().getCloudServiceProvider(name);
        } else {
            return;
        }

        if (provider.isValid()) {
            consumer.accept(provider);
            return;
        }

        channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeEnumConstant(ServiceDriverAPIResponse.SERVICE_NOT_FOUND)));
    }

    private IPacket createSuccessServiceResponseFor(IPacket packet) {
        return this.createSuccessServiceResponseFor(packet, null);
    }

    private IPacket createSuccessServiceResponseFor(IPacket packet, Consumer<ProtocolBuffer> modifier) {
        ProtocolBuffer buffer = ProtocolBuffer.create().writeEnumConstant(ServiceDriverAPIResponse.SUCCESS);
        if (modifier != null) {
            modifier.accept(buffer);
        }
        return Packet.createResponseFor(packet, buffer);
    }

    private IPermissionManagement permissionManagement() {
        return CloudNetDriver.getInstance().getPermissionManagement();
    }

}
