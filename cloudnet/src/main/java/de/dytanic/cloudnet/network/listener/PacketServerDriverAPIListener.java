package de.dytanic.cloudnet.network.listener;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.api.ServiceDriverAPIResponse;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;

import java.util.UUID;
import java.util.function.Consumer;

public class PacketServerDriverAPIListener implements IPacketListener {
    @Override
    public void handle(INetworkChannel channel, IPacket packet) throws Exception {
        DriverAPIRequestType requestType = packet.getBuffer().readEnumConstant(DriverAPIRequestType.class);

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
                    provider.setCloudServiceLifeCycle(packet.getBuffer().readEnumConstant(ServiceLifeCycle.class));
                    channel.sendPacket(Packet.createResponseFor(packet,
                            ProtocolBuffer.create()
                                    .writeEnumConstant(ServiceDriverAPIResponse.SUCCESS)
                    ));
                });
            }
            break;

            case ADD_PERMANENT_SERVICE_TASK: {

            }
            break;

            case REMOVE_PERMANENT_SERVICE_TASK: {

            }
            break;

            case ADD_GROUP_CONFIGURATION: {

            }
            break;

            case REMOVE_GROUP_CONFIGURATION: {

            }
            break;

            case GET_CONSOLE_COMMANDS: {

            }
            break;

            case GET_CONSOLE_COMMAND_BY_LINE: {

            }
            break;

            case TAB_COMPLETE_CONSOLE_COMMAND: {

            }
            break;

            case SEND_COMMAND_LINE: {

            }
            break;

            case CREATE_CLOUD_SERVICE_BY_SERVICE_TASK: {

            }
            break;

            case CREATE_CLOUD_SERVICE_BY_SERVICE_TASK_AND_ID: {

            }
            break;

            case CREATE_CLOUD_SERVICE_BY_CONFIGURATION: {

            }
            break;

            case GET_CLOUD_SERVICES: {

            }
            break;

            case GET_STARTED_CLOUD_SERVICES: {

            }
            break;

            case GET_CLOUD_SERVICES_BY_SERVICE_TASK: {

            }
            break;

            case GET_CLOUD_SERVICES_BY_GROUP: {

            }
            break;

            case GET_CLOUD_SERVICE_BY_UNIQUE_ID: {

            }
            break;

            case GET_PERMANENT_SERVICE_TASKS: {

            }
            break;

            case GET_PERMANENT_SERVICE_TASK_BY_NAME: {

            }
            break;

            case IS_SERVICE_TASK_PRESENT: {

            }
            break;

            case GET_GROUP_CONFIGURATIONS: {

            }
            break;

            case GET_GROUP_CONFIGURATION_BY_NAME: {

            }
            break;

            case IS_GROUP_CONFIGURATION_PRESENT: {

            }
            break;

            case GET_NODES: {

            }
            break;

            case GET_NODE_BY_UNIQUE_ID: {

            }
            break;

            case GET_NODE_INFO_SNAPSHOTS: {

            }
            break;

            case GET_NODE_INFO_SNAPSHOT_BY_UNIQUE_ID: {

            }
            break;

            case RESTART_CLOUD_SERVICE: {

            }
            break;

            case SEND_COMMAND_LINE_TO_NODE: {

            }
            break;

            case CREATE_CUSTOM_CLOUD_SERVICE: {

            }
            break;

            case CREATE_CUSTOM_CLOUD_SERVICE_WITH_NODE_AND_AMOUNT: {

            }
            break;

            case RUN_COMMAND_ON_CLOUD_SERVICE: {

            }
            break;

            case ADD_SERVICE_TEMPLATE_TO_CLOUD_SERVICE: {

            }
            break;

            case ADD_SERVICE_REMOTE_INCLUSION_TO_CLOUD_SERVICE: {

            }
            break;

            case ADD_SERVICE_DEPLOYMENT_TO_CLOUD_SERVICE: {

            }
            break;

            case INCLUDE_WAITING_TEMPLATES_ON_CLOUD_SERVICE: {

            }
            break;

            case INCLUDE_WAITING_INCLUSIONS_ON_CLOUD_SERVICE: {

            }
            break;

            case DEPLOY_RESOURCES_ON_CLOUD_SERVICE: {

            }
            break;

            case GET_SERVICES_AS_UNIQUE_ID: {

            }
            break;

            case GET_SERVICES_COUNT: {

            }
            break;

            case GET_SERVICES_COUNT_BY_GROUP: {

            }
            break;

            case GET_SERVICES_COUNT_BY_TASK: {

            }
            break;

            case GET_TEMPLATE_STORAGE_TEMPLATES: {

            }
            break;

            case KILL_CLOUD_SERVICE: {

            }
            break;

            case GET_CACHED_LOG_MESSAGES_FROM_CLOUD_SERVICE: {

            }
            break;

            case PERMISSION_MANAGEMENT_RELOAD: {

            }
            break;

            case PERMISSION_MANAGEMENT_ADD_USER: {

            }
            break;

            case PERMISSION_MANAGEMENT_UPDATE_USER: {

            }
            break;

            case PERMISSION_MANAGEMENT_DELETE_USERS_BY_NAME: {

            }
            break;

            case PERMISSION_MANAGEMENT_DELETE_USER_BY_UNIQUE_ID: {

            }
            break;

            case PERMISSION_MANAGEMENT_SET_USERS: {

            }
            break;

            case PERMISSION_MANAGEMENT_ADD_GROUP: {

            }
            break;

            case PERMISSION_MANAGEMENT_UPDATE_GROUP: {

            }
            break;

            case PERMISSION_MANAGEMENT_DELETE_GROUP_BY_NAME: {

            }
            break;

            case PERMISSION_MANAGEMENT_DELETE_GROUP: {

            }
            break;

            case PERMISSION_MANAGEMENT_SET_GROUPS: {

            }
            break;

            case GET_CLOUD_SERVICES_BY_ENVIRONMENT: {

            }
            break;

            case SEND_COMMAND_LINE_AS_PERMISSION_USER: {

            }
            break;

            case PERMISSION_MANAGEMENT_CONTAINS_USER_BY_UNIQUE_ID: {

            }
            break;

            case PERMISSION_MANAGEMENT_CONTAINS_USER_BY_NAME: {

            }
            break;

            case PERMISSION_MANAGEMENT_GET_USER_BY_UNIQUE_ID: {

            }
            break;

            case PERMISSION_MANAGEMENT_GET_USERS_BY_NAME: {

            }
            break;

            case PERMISSION_MANAGEMENT_GET_FIRST_USER_BY_NAME: {

            }
            break;

            case PERMISSION_MANAGEMENT_GET_USERS: {

            }
            break;

            case PERMISSION_MANAGEMENT_GET_USERS_BY_GROUP: {

            }
            break;

            case PERMISSION_MANAGEMENT_CONTAINS_GROUP: {

            }
            break;

            case PERMISSION_MANAGEMENT_GET_GROUP_BY_NAME: {

            }
            break;

            case PERMISSION_MANAGEMENT_GET_GROUPS: {

            }
            break;

            case PERMISSION_MANAGEMENT_GET_DEFAULT_GROUP: {

            }
            break;

            case GET_CLOUD_SERVICE_BY_NAME: {

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

}
