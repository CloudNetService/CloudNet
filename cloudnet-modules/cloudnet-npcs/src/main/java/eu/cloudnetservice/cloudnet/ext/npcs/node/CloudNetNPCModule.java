package eu.cloudnetservice.cloudnet.ext.npcs.node;


import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.IDatabase;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.util.DefaultModuleHelper;
import de.dytanic.cloudnet.module.NodeCloudNetModule;
import eu.cloudnetservice.cloudnet.ext.npcs.CloudNPC;
import eu.cloudnetservice.cloudnet.ext.npcs.NPCConstants;
import eu.cloudnetservice.cloudnet.ext.npcs.configuration.NPCConfiguration;
import eu.cloudnetservice.cloudnet.ext.npcs.node.listener.CloudNetNPCMessageListener;
import eu.cloudnetservice.cloudnet.ext.npcs.node.listener.IncludePluginListener;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CloudNetNPCModule extends NodeCloudNetModule {

    private static final String NPC_DOCUMENT_NAME = "npc_store";

    private static final String DOCUMENT_NPC_KEY = "npcs";

    private NPCConfiguration npcConfiguration;

    private Set<CloudNPC> cachedNPCs;

    @ModuleTask(event = ModuleLifeCycle.STARTED, order = 127)
    public void loadConfiguration() {
        super.getModuleWrapper().getDataFolder().mkdirs();

        this.npcConfiguration = super.getConfig().get("config", NPCConfiguration.class, new NPCConfiguration());

        for (Map.Entry<String, String> entry : NPCConfiguration.DEFAULT_MESSAGES.entrySet()) {
            if (!this.npcConfiguration.getMessages().containsKey(entry.getKey())) {
                this.npcConfiguration.getMessages().put(entry.getKey(), entry.getValue());
            }
        }

        this.saveNPCConfiguration();

        this.cachedNPCs = this.loadNPCs();
    }

    public void saveNPCConfiguration() {
        super.getConfig().append("config", this.npcConfiguration);
        super.saveConfig();
    }

    @ModuleTask(event = ModuleLifeCycle.STARTED, order = 126)
    public void registerListeners() {
        super.registerListeners(new IncludePluginListener(this), new CloudNetNPCMessageListener(this));
    }

    public void addNPC(CloudNPC npc) {
        if (!this.cachedNPCs.contains(npc)) {
            this.cachedNPCs.add(npc);

            this.saveNPCs(this.cachedNPCs);
        }
    }

    public void removeNPC(CloudNPC npc) {
        if (this.cachedNPCs.contains(npc)) {
            this.cachedNPCs.remove(npc);

            this.saveNPCs(this.cachedNPCs);
        }
    }

    public Set<CloudNPC> loadNPCs() {
        IDatabase database = super.getDatabaseProvider().getDatabase(DefaultModuleHelper.DEFAULT_CONFIGURATION_DATABASE_NAME);
        JsonDocument document = database.get(NPC_DOCUMENT_NAME);

        return document == null ? new HashSet<>() : document.get(DOCUMENT_NPC_KEY, NPCConstants.NPC_COLLECTION_TYPE);
    }

    public void saveNPCs(Set<CloudNPC> npcs) {
        IDatabase database = super.getDatabaseProvider().getDatabase(DefaultModuleHelper.DEFAULT_CONFIGURATION_DATABASE_NAME);

        database.update(NPC_DOCUMENT_NAME, new JsonDocument(DOCUMENT_NPC_KEY, npcs));
    }

    public NPCConfiguration getNPCConfiguration() {
        return npcConfiguration;
    }

    public void setNPCConfiguration(NPCConfiguration npcConfiguration) {
        this.npcConfiguration = npcConfiguration;
    }

    public Set<CloudNPC> getCachedNPCs() {
        return cachedNPCs;
    }

    public void setCachedNPCs(Set<CloudNPC> cachedNPCs) {
        this.cachedNPCs = cachedNPCs;
    }

}
