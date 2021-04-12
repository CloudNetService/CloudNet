package eu.cloudnetservice.cloudnet.ext.npcs.node;


import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.database.Database;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.util.DefaultModuleHelper;
import de.dytanic.cloudnet.module.NodeCloudNetModule;
import eu.cloudnetservice.cloudnet.ext.npcs.CloudNPC;
import eu.cloudnetservice.cloudnet.ext.npcs.NPCConstants;
import eu.cloudnetservice.cloudnet.ext.npcs.configuration.NPCConfiguration;
import eu.cloudnetservice.cloudnet.ext.npcs.node.command.CommandNPC;
import eu.cloudnetservice.cloudnet.ext.npcs.node.listener.CloudNetNPCMessageListener;
import eu.cloudnetservice.cloudnet.ext.npcs.node.listener.IncludePluginListener;
import eu.cloudnetservice.cloudnet.ext.npcs.node.listener.NPCTaskSetupListener;

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
        FileUtils.createDirectoryReported(this.getModuleWrapper().getDataDirectory());

        this.npcConfiguration = super.getConfig().get("config", NPCConfiguration.class, new NPCConfiguration());
        for (Map.Entry<String, String> entry : NPCConfiguration.DEFAULT_MESSAGES.entrySet()) {
            if (!this.npcConfiguration.getMessages().containsKey(entry.getKey())) {
                this.npcConfiguration.getMessages().put(entry.getKey(), entry.getValue());
            }
        }

        NPCConfiguration.sendNPCConfigurationUpdate(this.npcConfiguration);
        this.saveNPCConfiguration();

        this.cachedNPCs = this.loadNPCs();
    }

    public void saveNPCConfiguration() {
        super.getConfig().append("config", this.npcConfiguration);
        super.saveConfig();
    }

    @ModuleTask(event = ModuleLifeCycle.STARTED, order = 125)
    public void registerCommands() {
        super.registerCommand(new CommandNPC(this));
    }

    @ModuleTask(event = ModuleLifeCycle.STARTED, order = 126)
    public void registerListeners() {
        super.registerListeners(
                new IncludePluginListener(this),
                new CloudNetNPCMessageListener(this),
                new NPCTaskSetupListener(this)
        );
    }

    public void addNPC(CloudNPC npc) {
        this.cachedNPCs.remove(npc);
        this.cachedNPCs.add(npc);

        this.saveNPCs(this.cachedNPCs);
    }

    public void removeNPC(CloudNPC npc) {
        if (this.cachedNPCs.contains(npc)) {
            this.cachedNPCs.remove(npc);

            this.saveNPCs(this.cachedNPCs);
        }
    }

    public Set<CloudNPC> loadNPCs() {
        Database database = super.getDatabaseProvider().getDatabase(DefaultModuleHelper.DEFAULT_CONFIGURATION_DATABASE_NAME);
        JsonDocument document = database.get(NPC_DOCUMENT_NAME);

        return document == null ? new HashSet<>() : document.get(DOCUMENT_NPC_KEY, NPCConstants.NPC_COLLECTION_TYPE);
    }

    public void saveNPCs(Set<CloudNPC> npcs) {
        Database database = super.getDatabaseProvider().getDatabase(DefaultModuleHelper.DEFAULT_CONFIGURATION_DATABASE_NAME);

        database.update(NPC_DOCUMENT_NAME, new JsonDocument(DOCUMENT_NPC_KEY, npcs));
    }

    public NPCConfiguration getNPCConfiguration() {
        return this.npcConfiguration;
    }

    public void setNPCConfiguration(NPCConfiguration npcConfiguration) {
        this.npcConfiguration = npcConfiguration;
    }

    public Set<CloudNPC> getCachedNPCs() {
        return this.cachedNPCs;
    }

    public void setCachedNPCs(Set<CloudNPC> cachedNPCs) {
        this.cachedNPCs = cachedNPCs;
    }

}
