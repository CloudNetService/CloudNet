package de.dytanic.cloudnet.network;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.permission.DefaultJsonFilePermissionManagement;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.network.packet.*;
import de.dytanic.cloudnet.permission.DefaultDatabasePermissionManagement;
import de.dytanic.cloudnet.template.ITemplateStorage;
import de.dytanic.cloudnet.template.LocalTemplateStorage;

public class ClusterUtils {

    private ClusterUtils() {
        throw new UnsupportedOperationException();
    }

    public static void sendSetupInformationPackets(INetworkChannel channel) {
        sendSetupInformationPackets(channel, false);
    }

    public static void sendSetupInformationPackets(INetworkChannel channel, boolean secondNodeConnection) {
        channel.sendPacket(new PacketServerSetGlobalServiceInfoList(CloudNet.getInstance().getCloudServiceManager().getGlobalServiceInfoSnapshots().values()));
        if (!secondNodeConnection) {
            channel.sendPacket(new PacketServerSetGroupConfigurationList(CloudNet.getInstance().getGroupConfigurations(), NetworkUpdateType.ADD));
            channel.sendPacket(new PacketServerSetServiceTaskList(CloudNet.getInstance().getPermanentServiceTasks(), NetworkUpdateType.ADD));

            if (CloudNet.getInstance().getPermissionManagement() instanceof DefaultJsonFilePermissionManagement) {
                channel.sendPacket(new PacketServerSetPermissionData(
                        CloudNet.getInstance().getPermissionManagement().getUsers(),
                        CloudNet.getInstance().getPermissionManagement().getGroups(),
                        NetworkUpdateType.ADD
                ));
            }

            if (CloudNet.getInstance().getPermissionManagement() instanceof DefaultDatabasePermissionManagement) {
                channel.sendPacket(new PacketServerSetPermissionData(CloudNet.getInstance().getPermissionManagement().getGroups(), NetworkUpdateType.ADD, true));
            }

            ITemplateStorage templateStorage = CloudNetDriver.getInstance().getServicesRegistry().getService(ITemplateStorage.class, LocalTemplateStorage.LOCAL_TEMPLATE_STORAGE);

            byte[] bytes;
            for (ServiceTemplate serviceTemplate : templateStorage.getTemplates()) {
                bytes = templateStorage.toZipByteArray(serviceTemplate);
                channel.sendPacket(new PacketServerDeployLocalTemplate(serviceTemplate, bytes, false));
            }

            CloudNet.getInstance().publishH2DatabaseDataToCluster(channel);
        }
    }

}
