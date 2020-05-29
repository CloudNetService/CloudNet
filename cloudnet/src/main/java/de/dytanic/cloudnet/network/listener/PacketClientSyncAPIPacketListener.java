package de.dytanic.cloudnet.network.listener;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.*;

import java.util.Collection;
import java.util.UUID;

public final class PacketClientSyncAPIPacketListener implements IPacketListener {

    @Override
    public void handle(INetworkChannel channel, IPacket packet) {
        this.handle0(channel, packet);
    }

    private void handle0(INetworkChannel channel, IPacket packet) {
        if (packet.getHeader().contains(PacketConstants.SYNC_PACKET_ID_PROPERTY) && packet.getHeader().contains(PacketConstants.SYNC_PACKET_CHANNEL_PROPERTY) &&
                packet.getHeader().getString(PacketConstants.SYNC_PACKET_CHANNEL_PROPERTY).equals("cloudnet_driver_sync_api")) {
            switch (packet.getHeader().getString(PacketConstants.SYNC_PACKET_ID_PROPERTY)) {
                case "force_update_service": {
                    ServiceInfoSnapshot serviceInfoSnapshot = this.getCloudServiceProvider(packet.getHeader()).forceUpdateServiceInfo();
                    this.sendResponse(channel, packet.getUniqueId(), JsonDocument.newDocument(serviceInfoSnapshot));
                    break;
                }
                case "set_service_life_cycle":
                    this.getCloudServiceProvider(packet.getHeader()).setCloudServiceLifeCycle(packet.getHeader().get("lifeCycle", ServiceLifeCycle.class));
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                    break;
                case "add_permanent_service_task":
                    this.getCloudNet().getServiceTaskProvider().addPermanentServiceTask(
                            packet.getHeader().get("serviceTask", new TypeToken<ServiceTask>() {
                            }.getType())
                    );
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                    break;
                case "remove_permanent_service_task":
                    this.getCloudNet().getServiceTaskProvider().removePermanentServiceTask(packet.getHeader().getString("name"));
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                    break;
                case "add_group_configuration":
                    this.getCloudNet().getGroupConfigurationProvider().addGroupConfiguration(
                            packet.getHeader().get("groupConfiguration", new TypeToken<GroupConfiguration>() {
                            }.getType())
                    );
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                    break;
                case "remove_group_configuration":
                    this.getCloudNet().getGroupConfigurationProvider().removeGroupConfiguration(
                            packet.getHeader().getString("name")
                    );
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                    break;
                case "console_commands": {
                    if (packet.getHeader().contains("commandLine")) {
                        this.sendResponse(
                                channel,
                                packet.getUniqueId(),
                                new JsonDocument().append(
                                        "commandInfo",
                                        this.getCloudNet().getNodeInfoProvider().getConsoleCommand(packet.getHeader().getString("commandLine"))
                                )
                        );
                    } else {
                        this.sendResponse(
                                channel,
                                packet.getUniqueId(),
                                new JsonDocument().append("commandInfos", this.getCloudNet().getNodeInfoProvider().getConsoleCommands())
                        );
                    }
                }
                break;
                case "tab_complete": {
                    Collection<String> responses = this.getCloudNet().getNodeInfoProvider().getConsoleTabCompleteResults(packet.getHeader().getString("commandLine"));
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument("responses", responses));
                }
                break;
                case "send_commandLine": {
                    String[] messages = this.getCloudNet().getNodeInfoProvider().sendCommandLine(packet.getHeader().getString("commandLine"));
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument("responseMessages", messages));
                }
                break;
                case "create_CloudService_by_serviceTask": {
                    ServiceTask serviceTask = packet.getHeader().get("serviceTask", new TypeToken<ServiceTask>() {
                    }.getType());
                    ServiceInfoSnapshot serviceInfoSnapshot;
                    if (packet.getHeader().contains("taskId")) {
                        serviceInfoSnapshot = this.getCloudNet().getCloudServiceFactory().createCloudService(serviceTask, packet.getHeader().getInt("taskId"));
                    } else {
                        serviceInfoSnapshot = this.getCloudNet().getCloudServiceFactory().createCloudService(serviceTask);
                    }

                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument("serviceInfoSnapshot", serviceInfoSnapshot));
                }
                break;
                case "create_CloudService_by_serviceConfiguration": {
                    ServiceInfoSnapshot serviceInfoSnapshot = this.getCloudNet().getCloudServiceFactory()
                            .createCloudService(
                                    (ServiceConfiguration) packet.getHeader().get("serviceConfiguration", new TypeToken<ServiceConfiguration>() {
                                    }.getType())
                            );

                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument("serviceInfoSnapshot", serviceInfoSnapshot));
                }
                break;
                case "get_cloudServiceInfos": {
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument("serviceInfoSnapshots", this.getCloudNet().getCloudServiceProvider().getCloudServices()));
                }
                break;
                case "get_cloudServiceInfos_started": {
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument("serviceInfoSnapshots", this.getCloudNet().getCloudServiceProvider().getStartedCloudServices()));
                }
                break;
                case "get_cloudServiceInfos_by_taskName": {
                    this.sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("serviceInfoSnapshots", this.getCloudNet().getCloudServiceProvider().getCloudServices(packet.getHeader().getString("taskName"))
                            ));
                }
                break;
                case "get_cloudServiceInfos_by_group": {
                    this.sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("serviceInfoSnapshots", this.getCloudNet().getCloudServiceProvider().getCloudServicesByGroup(packet.getHeader().getString("group"))),
                            null);
                }
                break;
                case "get_cloudServiceInfos_by_uniqueId": {
                    this.sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("serviceInfoSnapshot", this.getCloudNet().getCloudServiceProvider().getCloudService(packet.getHeader().get("uniqueId", UUID.class))));
                }
                break;
                case "get_permanent_serviceTasks": {
                    this.sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("serviceTasks", this.getCloudNet().getServiceTaskProvider().getPermanentServiceTasks()));
                }
                break;
                case "get_service_task": {
                    this.sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("serviceTask", this.getCloudNet().getServiceTaskProvider().getServiceTask(packet.getHeader().getString("name"))));
                }
                break;
                case "is_service_task_present": {
                    this.sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("result", this.getCloudNet().getServiceTaskProvider().isServiceTaskPresent(packet.getHeader().getString("name"))));
                }
                break;
                case "get_groupConfigurations": {
                    this.sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("groupConfigurations", this.getCloudNet().getGroupConfigurationProvider().getGroupConfigurations()));
                }
                break;
                case "get_group_configuration": {
                    this.sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("groupConfiguration", this.getCloudNet().getGroupConfigurationProvider().getGroupConfiguration(packet.getHeader().getString("name"))));
                }
                break;
                case "is_group_configuration_present": {
                    this.sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("result", this.getCloudNet().getGroupConfigurationProvider().isGroupConfigurationPresent(packet.getHeader().getString("name"))));
                }
                break;
                case "get_nodes": {
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument("nodes", this.getCloudNet().getNodeInfoProvider().getNodes()));
                }
                break;
                case "get_node_by_uniqueId": {
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument("clusterNode", this.getCloudNet().getNodeInfoProvider().getNode(packet.getHeader().getString("uniqueId"))));
                }
                break;
                case "get_node_info_snapshots": {
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument("nodeInfoSnapshots", this.getCloudNet().getNodeInfoProvider().getNodeInfoSnapshots()));
                }
                break;
                case "get_node_info_snapshot_by_uniqueId": {
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument("clusterNodeInfoSnapshot", this.getCloudNet().getNodeInfoProvider().getNodeInfoSnapshot(packet.getHeader().getString("uniqueId"))));
                }
                break;
                case "restart_cloud_service": {
                    this.getCloudServiceProvider(packet.getHeader()).restart();
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "send_commandLine_on_node": {
                    this.sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("responseMessages", this.getCloudNet().sendCommandLine(
                                    packet.getHeader().getString("nodeUniqueId"),
                                    packet.getHeader().getString("commandLine")
                            )));
                }
                break;
                case "create_cloud_service_custom": {
                    this.sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("serviceInfoSnapshot", this.getCloudNet().getCloudServiceFactory().createCloudService(
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
                    this.sendResponse(channel, packet.getUniqueId(),
                            new JsonDocument("serviceInfoSnapshot", this.getCloudNet().getCloudServiceFactory().createCloudService(
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
                    this.getCloudServiceProvider(packet.getHeader())
                            .runCommand(packet.getHeader().getString("commandLine"));
                }
                break;
                case "add_service_template_to_cloud_service": {
                    this.getCloudServiceProvider(packet.getHeader())
                            .addServiceTemplate(packet.getHeader().get("serviceTemplate", new TypeToken<ServiceTemplate>() {
                            }.getType()));
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "add_service_remote_inclusion_to_cloud_service": {
                    this.getCloudServiceProvider(packet.getHeader())
                            .addServiceRemoteInclusion(packet.getHeader().get("serviceRemoteInclusion", new TypeToken<ServiceRemoteInclusion>() {
                            }.getType()));
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "add_service_deployment_to_cloud_service": {
                    this.getCloudServiceProvider(packet.getHeader())
                            .addServiceDeployment(packet.getHeader().get("serviceDeployment", new TypeToken<ServiceDeployment>() {
                            }.getType()));
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "include_all_waiting_service_templates": {
                    this.getCloudServiceProvider(packet.getHeader()).includeWaitingServiceTemplates();
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "include_all_waiting_service_inclusions": {
                    this.getCloudServiceProvider(packet.getHeader()).includeWaitingServiceInclusions();
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "deploy_resources_from_service": {
                    this.getCloudServiceProvider(packet.getHeader()).deployResources(packet.getHeader().getBoolean("removeDeployments", true));
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "get_services_as_uuid": {
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument("serviceUniqueIds", this.getCloudNet().getCloudServiceProvider().getServicesAsUniqueId()));
                }
                break;
                case "get_services_count": {
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument("servicesCount", this.getCloudNet().getCloudServiceProvider().getServicesCount()));
                }
                break;
                case "get_services_count_by_group": {
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument("servicesCount",
                            this.getCloudNet().getCloudServiceProvider().getServicesCountByGroup(packet.getHeader().getString("group"))
                    ));
                }
                break;
                case "get_services_count_by_task": {
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument("servicesCount",
                            this.getCloudNet().getCloudServiceProvider().getServicesCountByTask(packet.getHeader().getString("taskName"))
                    ));
                }
                break;
                case "get_local_template_storage_templates": {
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument(
                            "templates",
                            this.getCloudNet().getLocalTemplateStorageTemplates()
                    ));
                }
                break;
                case "kill_cloud_service": {
                    this.getCloudServiceProvider(packet.getHeader()).kill();
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "run_command_cloud_service": {
                    this.getCloudServiceProvider(packet.getHeader()).runCommand(packet.getHeader().getString("command"));
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "get_cached_log_messages_from_service": {
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument(
                            "cachedLogMessages",
                            this.getCloudServiceProvider(packet.getHeader()).getCachedLogMessages()
                    ));
                }
                break;
                case "permission_management_reload": {
                    boolean success = this.getCloudNet().getPermissionManagement().reload();
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument(), new byte[]{(byte) (success ? 1 : 0)});
                }
                break;
                case "permission_management_add_user": {
                    this.getCloudNet().getPermissionManagement().addUser(packet.getHeader().get("permissionUser", PermissionUser.TYPE));
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "permission_management_update_user": {
                    this.getCloudNet().getPermissionManagement().updateUser(packet.getHeader().get("permissionUser", PermissionUser.TYPE));
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "permission_management_delete_user_with_name": {
                    boolean success = this.getCloudNet().getPermissionManagement().deleteUser(packet.getHeader().getString("name"));
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument(), new byte[]{(byte) (success ? 1 : 0)});
                }
                break;
                case "permission_management_delete_user": {
                    boolean success = this.getCloudNet().getPermissionManagement().deleteUser((PermissionUser) packet.getHeader().get("permissionUser", PermissionUser.TYPE));
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument(), new byte[]{(byte) (success ? 1 : 0)});
                }
                break;
                case "permission_management_set_users": {
                    this.getCloudNet().getPermissionManagement().setUsers(packet.getHeader().get("permissionUsers", new TypeToken<Collection<PermissionUser>>() {
                    }.getType()));
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "permission_management_add_group": {
                    this.getCloudNet().getPermissionManagement().addGroup(packet.getHeader().get("permissionGroup", PermissionGroup.TYPE));
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "permission_management_update_group": {
                    this.getCloudNet().getPermissionManagement().updateGroup(packet.getHeader().get("permissionGroup", PermissionGroup.TYPE));
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "permission_management_delete_group_with_name": {
                    this.getCloudNet().getPermissionManagement().deleteGroup(packet.getHeader().getString("name"));
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "permission_management_delete_group": {
                    this.getCloudNet().getPermissionManagement().deleteGroup((PermissionGroup) packet.getHeader().get("permissionGroup", PermissionGroup.TYPE));
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "permission_management_set_groups": {
                    this.getCloudNet().getPermissionManagement().setGroups(packet.getHeader().get("permissionGroups", new TypeToken<Collection<PermissionGroup>>() {
                    }.getType()));
                    this.sendEmptyResponse(channel, packet.getUniqueId());
                }
                break;
                case "get_cloud_services_with_environment": {
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument("serviceInfoSnapshots",
                            this.getCloudNet().getCloudServiceProvider().getCloudServices(packet.getHeader().get("serviceEnvironment", ServiceEnvironmentType.class))
                    ));
                }
                break;
                case "get_template_storage_templates": {
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument("templates", this.getCloudNet()
                            .getTemplateStorageTemplates(packet.getHeader().getString("serviceName")))
                    );
                }
                break;
                case "send_commandline_as_permission_user": {
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument("executionResponse", this.getCloudNet().sendCommandLineAsPermissionUser(
                            packet.getHeader().get("uniqueId", UUID.class),
                            packet.getHeader().getString("commandLine")
                    )));
                }
                break;
                case "permission_management_contains_user_with_uuid": {
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument("result", this.getCloudNet().getPermissionManagement().containsUser(packet.getHeader().get("uniqueId", UUID.class))));
                }
                break;
                case "permission_management_contains_user_with_name": {
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument("result", this.getCloudNet().getPermissionManagement().containsUser(packet.getHeader().getString("name"))));
                }
                break;
                case "permission_management_get_user_by_uuid": {
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument("permissionUser", this.getCloudNet().getPermissionManagement().getUser(packet.getHeader().get("uniqueId", UUID.class))));
                }
                break;
                case "permission_management_get_user_by_name": {
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument("permissionUsers", this.getCloudNet().getPermissionManagement().getUsers(packet.getHeader().getString("name"))));
                }
                break;
                case "permission_management_get_first_user": {
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument("permissionUser", this.getCloudNet().getPermissionManagement().getFirstUser(packet.getHeader().getString("name"))));
                }
                break;
                case "permission_management_get_users": {
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument("permissionUsers", this.getCloudNet().getPermissionManagement().getUsers()));
                }
                break;
                case "permission_management_get_users_by_group": {
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument("permissionUsers", this.getCloudNet().getPermissionManagement().getUsersByGroup(packet.getHeader().getString("group"))));
                }
                break;
                case "permission_management_contains_group": {
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument("result", this.getCloudNet().getPermissionManagement().containsGroup(packet.getHeader().getString("name"))));
                }
                break;
                case "permission_management_get_group": {
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument("permissionGroup", this.getCloudNet().getPermissionManagement().getGroup(packet.getHeader().getString("name"))));
                }
                break;
                case "permission_management_get_groups": {
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument("permissionGroups", this.getCloudNet().getPermissionManagement().getGroups()));
                }
                break;
                case "permission_management_get_default_group": {
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument("permissionGroup", this.getCloudNet().getPermissionManagement().getDefaultPermissionGroup()));
                }
                break;
                case "get_cloudService_by_name": {
                    this.sendResponse(channel, packet.getUniqueId(), new JsonDocument("serviceInfoSnapshot",
                            this.getCloudNet().getCloudServiceProvider().getCloudServiceByName(packet.getHeader().getString("name"))));
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

    private SpecificCloudServiceProvider getCloudServiceProvider(JsonDocument header) {
        if (header.contains("uniqueId")) {
            return this.getCloudNet().getCloudServiceProvider(header.get("uniqueId", UUID.class));
        }
        if (header.contains("name")) {
            return this.getCloudNet().getCloudServiceProvider(header.getString("name"));
        }
        throw new IllegalArgumentException("No name or uniqueId provided by the client");
    }

    private void sendResponse(INetworkChannel channel, UUID uniqueId, JsonDocument header) {
        this.sendResponse(channel, uniqueId, header, null);
    }

    private void sendResponse(INetworkChannel channel, UUID uniqueId, JsonDocument header, byte[] body) {
        channel.sendPacket(new Packet(PacketConstants.INTERNAL_CALLABLE_CHANNEL, uniqueId, header, body));
    }

    private void sendEmptyResponse(INetworkChannel channel, UUID uniqueId) {
        this.sendResponse(channel, uniqueId, new JsonDocument(), null);
    }
}