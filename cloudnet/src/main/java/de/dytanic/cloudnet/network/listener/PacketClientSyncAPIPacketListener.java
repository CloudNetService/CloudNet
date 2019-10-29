package de.dytanic.cloudnet.network.listener;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import de.dytanic.cloudnet.driver.service.*;

import java.util.Collection;
import java.util.UUID;

public final class PacketClientSyncAPIPacketListener implements IPacketListener {

    @Override
    public void handle(INetworkChannel channel, IPacket packet) {
        handle0(channel, packet);
    }

    private void handle0(INetworkChannel channel, IPacket packet) {
        if (packet.getHeader().contains(PacketConstants.SYNC_PACKET_ID_PROPERTY) && packet.getHeader().contains(PacketConstants.SYNC_PACKET_CHANNEL_PROPERTY) &&
                packet.getHeader().getString(PacketConstants.SYNC_PACKET_CHANNEL_PROPERTY).equals("cloudnet_driver_sync_api")) {
            switch (packet.getHeader().getString(PacketConstants.SYNC_PACKET_ID_PROPERTY)) {
                case "set_service_life_cycle":
                    getCloudNet().setCloudServiceLifeCycle(
                            packet.getHeader().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                            }.getType()),
                            packet.getHeader().get("lifeCycle", ServiceLifeCycle.class)
                    );
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                    break;
                case "add_permanent_service_task":
                    getCloudNet().addPermanentServiceTask(
                            packet.getHeader().get("serviceTask", new TypeToken<ServiceTask>() {
                            }.getType())
                    );
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                    break;
                case "remove_permanent_service_task":
                    getCloudNet().removePermanentServiceTask(packet.getHeader().getString("name"));
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                    break;
                case "add_group_configuration":
                    getCloudNet().addGroupConfiguration(
                            packet.getHeader().get("groupConfiguration", new TypeToken<GroupConfiguration>() {
                            }.getType())
                    );
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                    break;
                case "remove_group_configuration":
                    getCloudNet().removeGroupConfiguration(
                            packet.getHeader().getString("name")
                    );
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                    break;
                case "console_commands": {
                    if (packet.getHeader().contains("commandLine")) {
                        this.sendResponse(
                                channel,
                                packet.getUniqueId(),
                                new JsonDocument()
                                        .append(
                                                "commandInfo",
                                                getCloudNet().getConsoleCommand(
                                                        packet.getHeader().getString("commandLine")
                                                )
                                        )
                        );
                    } else {
                        this.sendResponse(
                                channel,
                                packet.getUniqueId(),
                                new JsonDocument().append("commandInfos", getCloudNet().getConsoleCommands())
                        );
                    }
                }
                break;
                case "send_commandLine": {
                    String[] messages = getCloudNet().sendCommandLine(packet.getHeader().getString("commandLine"));
                    sendResponse(channel, packet.getUniqueId(), new JsonDocument("responseMessages", messages));
                }
                break;
                case "create_CloudService_by_serviceTask": {
                    ServiceInfoSnapshot serviceInfoSnapshot = getCloudNet().
                            createCloudService((ServiceTask) packet.getHeader().get("serviceTask", new TypeToken<ServiceTask>() {
                            }.getType()));

                    sendResponse(channel, packet.getUniqueId(), new JsonDocument("serviceInfoSnapshot", serviceInfoSnapshot));
                }
                break;
                case "create_CloudService_by_serviceConfiguration": {
                    ServiceInfoSnapshot serviceInfoSnapshot = getCloudNet().createCloudService(
                            (ServiceConfiguration) packet.getHeader().get("serviceConfiguration", new TypeToken<ServiceConfiguration>() {
                            }.getType())
                    );

                    sendResponse(channel, packet.getUniqueId(), new JsonDocument("serviceInfoSnapshot", serviceInfoSnapshot));
                }
                break;
                case "get_cloudServiceInfos": {
                    sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("serviceInfoSnapshots", getCloudNet().getCloudServices()
                            ));
                }
                break;
                case "get_cloudServiceInfos_started": {
                    sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("serviceInfoSnapshots", getCloudNet().getStartedCloudServices()
                            ));
                }
                break;
                case "get_cloudServiceInfos_by_taskName": {
                    sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("serviceInfoSnapshots", getCloudNet().getCloudService(packet.getHeader().getString("taskName"))
                            ));
                }
                break;
                case "get_cloudServiceInfos_by_group": {
                    sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("serviceInfoSnapshots", getCloudNet().getCloudServiceByGroup(packet.getHeader().getString("group"))),
                            null);
                }
                break;
                case "get_cloudServiceInfos_by_uniqueId": {
                    sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("serviceInfoSnapshot", getCloudNet().getCloudService(packet.getHeader().get("uniqueId", UUID.class))
                            ));
                }
                break;
                case "get_permanent_serviceTasks": {
                    sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("serviceTasks", getCloudNet().getPermanentServiceTasks()
                            ));
                }
                break;
                case "get_service_task": {
                    sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("serviceTask", getCloudNet().getServiceTask(packet.getHeader().getString("name"))
                            ));
                }
                break;
                case "is_service_task_present": {
                    sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("result", getCloudNet().isServiceTaskPresent(packet.getHeader().getString("name"))
                            ));
                }
                break;
                case "get_groupConfigurations": {
                    sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("groupConfigurations", getCloudNet().getGroupConfigurations()
                            ));
                }
                break;
                case "get_group_configuration": {
                    sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("groupConfiguration", getCloudNet().getGroupConfiguration(packet.getHeader().getString("name"))
                            ));
                }
                break;
                case "is_group_configuration_present": {
                    sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("result", getCloudNet().isGroupConfigurationPresent(packet.getHeader().getString("name"))
                            ));
                }
                break;
                case "get_nodes": {
                    sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("nodes", getCloudNet().getNodes()
                            ));
                }
                break;
                case "get_node_by_uniqueId": {
                    sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("clusterNode", getCloudNet().getNode(packet.getHeader().getString("uniqueId"))
                            ));
                }
                break;
                case "get_node_info_snapshots": {
                    sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("nodeInfoSnapshots", getCloudNet().getNodeInfoSnapshots()
                            ));
                }
                break;
                case "get_node_info_snapshot_by_uniqueId": {
                    sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("clusterNodeInfoSnapshot", getCloudNet().getNodeInfoSnapshot(packet.getHeader().getString("uniqueId"))
                            ));
                }
                break;
                case "restart_cloud_service": {
                    getCloudNet().restartCloudService(packet.getHeader().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                    }.getType()));
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "send_commandLine_on_node": {
                    sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("responseMessages", getCloudNet().sendCommandLine(
                                    packet.getHeader().getString("nodeUniqueId"),
                                    packet.getHeader().getString("commandLine")
                            )));
                }
                break;
                case "create_cloud_service_custom": {
                    sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("serviceInfoSnapshot", getCloudNet().createCloudService(
                                    packet.getHeader().getString("name"),
                                    packet.getHeader().getString("runtime"),
                                    packet.getHeader().getBoolean("autoDeleteOnStop"),
                                    packet.getHeader().getBoolean("staticService"),
                                    packet.getHeader().get("includes", new TypeToken<Collection<ServiceRemoteInclusion>>() {
                                    }.getType()),
                                    packet.getHeader().get("templates", new TypeToken<Collection<ServiceTemplate>>() {
                                    }.getType()),
                                    packet.getHeader().get("deployments", new TypeToken<Collection<ServiceDeployment>>() {
                                    }.getType()),
                                    packet.getHeader().get("groups", new TypeToken<Collection<String>>() {
                                    }.getType()),
                                    packet.getHeader().get("processConfiguration", new TypeToken<ProcessConfiguration>() {
                                    }.getType()),
                                    packet.getHeader().get("properties", new TypeToken<JsonDocument>() {
                                    }.getType()),
                                    packet.getHeader().getInt("port")
                            ))
                    );
                }
                break;
                case "create_cloud_service_custom_selected_node_and_amount": {
                    sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("serviceInfoSnapshot", getCloudNet().createCloudService(
                                    packet.getHeader().getString("nodeUniqueId"),
                                    packet.getHeader().getInt("amount"),
                                    packet.getHeader().getString("name"),
                                    packet.getHeader().getString("runtime"),
                                    packet.getHeader().getBoolean("autoDeleteOnStop"),
                                    packet.getHeader().getBoolean("staticService"),
                                    packet.getHeader().get("includes", new TypeToken<Collection<ServiceRemoteInclusion>>() {
                                    }.getType()),
                                    packet.getHeader().get("templates", new TypeToken<Collection<ServiceTemplate>>() {
                                    }.getType()),
                                    packet.getHeader().get("deployments", new TypeToken<Collection<ServiceDeployment>>() {
                                    }.getType()),
                                    packet.getHeader().get("groups", new TypeToken<Collection<String>>() {
                                    }.getType()),
                                    packet.getHeader().get("processConfiguration", new TypeToken<ProcessConfiguration>() {
                                    }.getType()),
                                    packet.getHeader().get("properties", new TypeToken<JsonDocument>() {
                                    }.getType()),
                                    packet.getHeader().getInt("port")
                            ))
                    );
                }
                break;
                case "send_commandline_to_cloud_service": {
                    sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("serviceInfoSnapshot", getCloudNet().sendCommandLineToCloudService(
                                    packet.getHeader().get("uniqueId", UUID.class),
                                    packet.getHeader().getString("commandLine")
                            )),
                            null);
                }
                break;
                case "add_service_template_to_cloud_service": {
                    sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("serviceInfoSnapshot", getCloudNet().addServiceTemplateToCloudService(
                                    packet.getHeader().get("uniqueId", UUID.class),
                                    packet.getHeader().get("serviceTemplate", new TypeToken<ServiceTemplate>() {
                                    }.getType())
                            )),
                            null
                    );
                }
                break;
                case "add_service_remote_inclusion_to_cloud_service": {
                    sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("serviceInfoSnapshot", getCloudNet().addServiceRemoteInclusionToCloudService(
                                    packet.getHeader().get("uniqueId", UUID.class),
                                    packet.getHeader().get("serviceRemoteInclusion", new TypeToken<ServiceRemoteInclusion>() {
                                    }.getType())
                            )),
                            null
                    );
                }
                break;
                case "add_service_deployment_to_cloud_service": {
                    sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("serviceInfoSnapshot", getCloudNet().addServiceDeploymentToCloudService(
                                    packet.getHeader().get("uniqueId", UUID.class),
                                    packet.getHeader().get("serviceDeployment", new TypeToken<ServiceDeployment>() {
                                    }.getType())
                            )),
                            null
                    );
                }
                break;
                case "include_all_waiting_service_templates": {
                    getCloudNet().includeWaitingServiceTemplates(packet.getHeader().get("uniqueId", UUID.class));
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "include_all_waiting_service_inclusions": {
                    getCloudNet().includeWaitingServiceInclusions(packet.getHeader().get("uniqueId", UUID.class));
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "deploy_resources_from_service": {
                    getCloudNet().deployResources(
                            packet.getHeader().get("uniqueId", UUID.class),
                            packet.getHeader().getBoolean("removeDeployments", true));
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "get_services_as_uuid": {
                    sendResponse(channel, packet.getUniqueId(), new JsonDocument("serviceUniqueIds",
                            getCloudNet().getServicesAsUniqueId()
                    ));
                }
                break;
                case "get_services_count": {
                    sendResponse(channel, packet.getUniqueId(), new JsonDocument("servicesCount",
                            getCloudNet().getServicesCount()
                    ));
                }
                break;
                case "get_services_count_by_group": {
                    sendResponse(channel, packet.getUniqueId(), new JsonDocument("servicesCount",
                            getCloudNet().getServicesCountByGroup(packet.getHeader().getString("group"))
                    ));
                }
                break;
                case "get_services_count_by_task": {
                    sendResponse(channel, packet.getUniqueId(), new JsonDocument("servicesCount",
                            getCloudNet().getServicesCountByTask(packet.getHeader().getString("taskName"))
                    ));
                }
                break;
                case "get_local_template_storage_templates": {
                    sendResponse(channel, packet.getUniqueId(), new JsonDocument(
                            "templates",
                            getCloudNet().getLocalTemplateStorageTemplates()
                    ));
                }
                break;
                case "kill_cloud_service": {
                    CloudNetDriver.getInstance().killCloudService(packet.getHeader().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                    }.getType()));
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "run_command_cloud_service": {
                    CloudNetDriver.getInstance().runCommand(
                            packet.getHeader().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                            }.getType()),
                            packet.getHeader().getString("command")
                    );
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "get_cached_log_messages_from_service": {
                    sendResponse(channel, packet.getUniqueId(), new JsonDocument(
                            "cachedLogMessages",
                            getCloudNet().getCachedLogMessagesFromService(packet.getHeader().get("uniqueId", UUID.class))
                    ));
                }
                break;
                case "permission_management_add_user": {
                    getCloudNet().addUser(packet.getHeader().get("permissionUser", PermissionUser.TYPE));
                    sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "permission_management_update_user": {
                    getCloudNet().updateUser(packet.getHeader().get("permissionUser", PermissionUser.TYPE));
                    sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "permission_management_delete_user_with_name": {
                    getCloudNet().deleteUser(packet.getHeader().getString("name"));
                    sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "permission_management_delete_user": {
                    getCloudNet().deleteUser((PermissionUser) packet.getHeader().get("permissionUser", PermissionUser.TYPE));
                    sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "permission_management_set_users": {
                    getCloudNet().setUsers(packet.getHeader().get("permissionUsers", new TypeToken<Collection<PermissionUser>>() {
                    }.getType()));
                    sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "permission_management_add_group": {
                    getCloudNet().addGroup(packet.getHeader().get("permissionGroup", PermissionGroup.TYPE));
                    sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "permission_management_update_group": {
                    getCloudNet().updateGroup(packet.getHeader().get("permissionGroup", PermissionGroup.TYPE));
                    sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "permission_management_delete_group_with_name": {
                    getCloudNet().deleteGroup(packet.getHeader().getString("name"));
                    sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "permission_management_delete_group": {
                    getCloudNet().deleteGroup((PermissionGroup) packet.getHeader().get("permissionGroup", PermissionGroup.TYPE));
                    sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "permission_management_set_groups": {
                    getCloudNet().setGroups(packet.getHeader().get("permissionGroups", new TypeToken<Collection<PermissionGroup>>() {
                    }.getType()));
                    sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "get_cloud_services_with_environment": {
                    sendResponse(channel, packet.getUniqueId(), new JsonDocument("serviceInfoSnapshots",
                            getCloudNet().getCloudServices(packet.getHeader().get("serviceEnvironment", ServiceEnvironmentType.class))
                    ));
                }
                break;
                case "get_template_storage_templates": {
                    sendResponse(channel, packet.getUniqueId(), new JsonDocument("templates", getCloudNet()
                            .getTemplateStorageTemplates(packet.getHeader().getString("serviceName")))
                    );
                }
                break;
                case "send_commandline_as_permission_user": {
                    sendResponse(channel, packet.getUniqueId(), new JsonDocument("executionResponse", getCloudNet().sendCommandLineAsPermissionUser(
                            packet.getHeader().get("uniqueId", UUID.class),
                            packet.getHeader().getString("commandLine")
                    )));
                }
                break;
                case "permission_management_contains_user_with_uuid": {
                    sendResponse(channel, packet.getUniqueId(), new JsonDocument("result", getCloudNet().containsUser(packet.getHeader().get("uniqueId", UUID.class))));
                }
                break;
                case "permission_management_contains_user_with_name": {
                    sendResponse(channel, packet.getUniqueId(), new JsonDocument("result", getCloudNet().containsUser(packet.getHeader().getString("name"))));
                }
                break;
                case "permission_management_get_user_by_uuid": {
                    sendResponse(channel, packet.getUniqueId(), new JsonDocument("permissionUser", getCloudNet().getUser(packet.getHeader().get("uniqueId", UUID.class))));
                }
                break;
                case "permission_management_get_user_by_name": {
                    sendResponse(channel, packet.getUniqueId(), new JsonDocument("permissionUsers", getCloudNet().getUser(packet.getHeader().getString("name"))));
                }
                break;
                case "permission_management_get_users": {
                    sendResponse(channel, packet.getUniqueId(), new JsonDocument("permissionUsers", getCloudNet().getUsers()));
                }
                break;
                case "permission_management_get_users_by_group": {
                    sendResponse(channel, packet.getUniqueId(), new JsonDocument("permissionUsers", getCloudNet().getUserByGroup(packet.getHeader().getString("group"))));
                }
                break;
                case "permission_management_contains_group": {
                    sendResponse(channel, packet.getUniqueId(), new JsonDocument("result", getCloudNet().containsGroup(packet.getHeader().getString("name"))));
                }
                break;
                case "permission_management_get_group": {
                    sendResponse(channel, packet.getUniqueId(), new JsonDocument("permissionGroup", getCloudNet().getGroup(packet.getHeader().getString("name"))));
                }
                break;
                case "permission_management_get_groups": {
                    sendResponse(channel, packet.getUniqueId(), new JsonDocument("permissionGroups", getCloudNet().getGroups()));
                }
                break;
                case "get_cloudService_by_name": {
                    sendResponse(channel, packet.getUniqueId(), new JsonDocument("serviceInfoSnapshot",
                            getCloudNet().getCloudServiceByName(packet.getHeader().getString("name"))));
                }
                break;
                default:
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                    break;
            }
        }
    }

    private CloudNet getCloudNet() {
        return CloudNet.getInstance();
    }

    private void sendResponse(INetworkChannel channel, UUID uniqueId, JsonDocument header) {
        sendResponse(channel, uniqueId, header, null);
    }

    private void sendResponse(INetworkChannel channel, UUID uniqueId, JsonDocument header, byte[] body) {
        channel.sendPacket(new Packet(PacketConstants.INTERNAL_CALLABLE_CHANNEL, uniqueId, header, body));
    }

    private void sendEmptyResponse(INetworkChannel channel, UUID uniqueId) {
        sendResponse(channel, uniqueId, new JsonDocument(), null);
    }
}