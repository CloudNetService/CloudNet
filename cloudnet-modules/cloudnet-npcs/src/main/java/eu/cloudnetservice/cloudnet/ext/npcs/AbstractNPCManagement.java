package eu.cloudnetservice.cloudnet.ext.npcs;


import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.ServiceInfoStateWatcher;
import de.dytanic.cloudnet.wrapper.Wrapper;
import eu.cloudnetservice.cloudnet.ext.npcs.configuration.NPCConfiguration;
import eu.cloudnetservice.cloudnet.ext.npcs.configuration.NPCConfigurationEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class AbstractNPCManagement extends ServiceInfoStateWatcher {

    protected NPCConfiguration npcConfiguration;

    protected NPCConfigurationEntry ownNPCConfigurationEntry;

    protected Set<CloudNPC> cloudNPCS;

    public AbstractNPCManagement() {
        this.setNPCConfiguration(this.getNPCConfigurationFromNode());

        Set<CloudNPC> npcsFromNode = this.getNPCsFromNode();
        this.cloudNPCS = npcsFromNode == null ? new HashSet<>() : npcsFromNode.stream()
                .filter(npc -> Arrays.asList(Wrapper.getInstance().getServiceConfiguration().getGroups()).contains(npc.getPosition().getGroup()))
                .collect(Collectors.toSet());

        super.includeExistingServices();
    }

    @Override
    protected boolean shouldWatchService(ServiceInfoSnapshot serviceInfoSnapshot) {
        if (serviceInfoSnapshot != null) {

            ServiceEnvironmentType currentEnvironment = Wrapper.getInstance().getServiceId().getEnvironment();
            ServiceEnvironmentType serviceEnvironment = serviceInfoSnapshot.getServiceId().getEnvironment();

            return serviceEnvironment.isMinecraftJavaServer() && currentEnvironment.isMinecraftJavaServer();
        }

        return false;
    }

    @Override
    protected boolean shouldShowFullServices() {
        return this.ownNPCConfigurationEntry.isShowFullServices();
    }

    @EventListener
    public void handle(ChannelMessageReceiveEvent event) {

        if (event.getChannel().equals(NPCConstants.NPC_CHANNEL_NAME)) {

            switch (event.getMessage().toLowerCase()) {
                case NPCConstants.NPC_CHANNEL_UPDATE_CONFIGURATION_MESSAGE: {
                    NPCConfiguration npcConfiguration = event.getData().get("npcConfiguration", NPCConfiguration.class);
                    this.setNPCConfiguration(npcConfiguration);
                }
                break;
                case NPCConstants.NPC_CHANNEL_ADD_NPC_MESSAGE: {
                    CloudNPC npc = event.getData().get("npc", CloudNPC.class);

                    if (npc != null) {
                        this.addNPC(npc);
                    }
                }
                break;
                case NPCConstants.NPC_CHANNEL_REMOVE_NPC_MESSAGE: {
                    CloudNPC npc = event.getData().get("npc", CloudNPC.class);

                    if (npc != null) {
                        this.removeNPC(npc);
                    }
                }
                break;
            }

        }

    }

    public abstract void updateNPC(CloudNPC cloudNPC);

    public abstract boolean isWorldLoaded(CloudNPC cloudNPC);

    public List<Pair<ServiceInfoSnapshot, ServiceInfoState>> filterNPCServices(@NotNull CloudNPC cloudNPC) {
        return super.services.values().stream()
                .filter(pair -> (pair.getSecond() != ServiceInfoState.STOPPED && pair.getSecond() != ServiceInfoState.STARTING)
                        && Arrays.asList(pair.getFirst().getConfiguration().getGroups()).contains(cloudNPC.getTargetGroup()))
                .sorted(Comparator.comparingInt(pair -> pair.getFirst().getServiceId().getTaskServiceId()))
                .collect(Collectors.toList());
    }

    /**
     * Adds a NPC to this wrapper instance
     *
     * @param npc the NPC to add
     * @return if the NPC is allowed to exist on this wrapper instance
     */
    public boolean addNPC(@NotNull CloudNPC npc) {
        if (Arrays.asList(Wrapper.getInstance().getServiceConfiguration().getGroups()).contains(npc.getPosition().getGroup())) {
            this.cloudNPCS.remove(npc);
            this.cloudNPCS.add(npc);

            return true;
        }
        return false;
    }

    /**
     * Removes a NPC from this wrapper instance
     *
     * @param npc the NPC to remove
     */
    public void removeNPC(@NotNull CloudNPC npc) {
        this.cloudNPCS.remove(npc);
    }

    /**
     * Adds a NPC to the whole cluster and the database
     *
     * @param npc the NPC to add
     */
    public void sendNPCAddUpdate(@NotNull CloudNPC npc) {
        CloudNetDriver.getInstance().getMessenger()
                .sendChannelMessage(
                        NPCConstants.NPC_CHANNEL_NAME,
                        NPCConstants.NPC_CHANNEL_ADD_NPC_MESSAGE,
                        new JsonDocument("npc", npc)
                );
    }

    /**
     * Removes a NPC from the whole cluster and the database
     *
     * @param npc the NPC to remove
     */
    public void sendNPCRemoveUpdate(@NotNull CloudNPC npc) {
        CloudNetDriver.getInstance().getMessenger()
                .sendChannelMessage(
                        NPCConstants.NPC_CHANNEL_NAME,
                        NPCConstants.NPC_CHANNEL_REMOVE_NPC_MESSAGE,
                        new JsonDocument("npc", npc)
                );
    }

    public NPCConfiguration getNPCConfigurationFromNode() {
        ITask<NPCConfiguration> npcConfiguration = CloudNetDriver.getInstance().getPacketQueryProvider().sendCallablePacket(
                CloudNetDriver.getInstance().getNetworkClient().getChannels().iterator().next(),
                NPCConstants.NPC_CHANNEL_NAME,
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, NPCConstants.NPC_CHANNEL_GET_CONFIGURATION_MESSAGE),
                new byte[0],
                documentPair -> documentPair.getFirst().get("npcConfiguration", NPCConfiguration.class)
        );

        try {
            return npcConfiguration.get(5, TimeUnit.SECONDS);
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return null;
    }

    /**
     * Returns all NPCs contained in the CloudNet NPC database
     *
     * @return all NPCs or null, if an error occurred
     */
    @Nullable
    public Set<CloudNPC> getNPCsFromNode() {
        ITask<Set<CloudNPC>> npcs = CloudNetDriver.getInstance().getPacketQueryProvider().sendCallablePacket(
                CloudNetDriver.getInstance().getNetworkClient().getChannels().iterator().next(),
                NPCConstants.NPC_CHANNEL_NAME,
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, NPCConstants.NPC_CHANNEL_GET_NPCS_MESSAGE),
                new byte[0],
                documentPair -> documentPair.getFirst().get("npcs", NPCConstants.NPC_COLLECTION_TYPE)
        );

        try {
            return npcs.get(5, TimeUnit.SECONDS);
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return null;
    }

    public NPCConfiguration getNPCConfiguration() {
        return npcConfiguration;
    }

    public void setNPCConfiguration(NPCConfiguration npcConfiguration) {
        this.npcConfiguration = npcConfiguration;
        this.ownNPCConfigurationEntry = npcConfiguration.getConfigurations().stream()
                .filter(entry -> Arrays.asList(Wrapper.getInstance().getServiceConfiguration().getGroups()).contains(entry.getTargetGroup()))
                .findFirst()
                .orElse(new NPCConfigurationEntry());
    }

    public NPCConfigurationEntry getOwnNPCConfigurationEntry() {
        return ownNPCConfigurationEntry;
    }


    /**
     * Returns a copy of the NPCs allowed to exist on this wrapper instance
     * Use {@link AbstractNPCManagement#addNPC(CloudNPC)} and {@link AbstractNPCManagement#removeNPC(CloudNPC)} for local modification
     *
     * @return a copy of the NPCs
     */
    public Set<CloudNPC> getCloudNPCS() {
        return new HashSet<>(this.cloudNPCS);
    }

}
