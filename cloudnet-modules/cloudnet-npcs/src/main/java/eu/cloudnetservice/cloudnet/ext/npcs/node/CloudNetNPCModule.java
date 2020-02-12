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
import eu.cloudnetservice.cloudnet.ext.npcs.node.listener.IncludePluginListener;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

public class CloudNetNPCModule extends NodeCloudNetModule {

    private static final String NPC_DOCUMENT_NAME = "npc_store";

    private static final String DOCUMENT_NPC_KEY = "npcs";

    private NPCConfiguration configuration;

    @ModuleTask(event = ModuleLifeCycle.STARTED)
    public void loadConfiguration() {
        super.getModuleWrapper().getDataFolder().mkdirs();

        this.configuration = super.getConfig().get("config", NPCConfiguration.class, new NPCConfiguration());

        for (Map.Entry<String, String> entry : NPCConfiguration.DEFAULT_MESSAGES.entrySet()) {
            if (!this.configuration.getMessages().containsKey(entry.getKey())) {
                this.configuration.getMessages().put(entry.getKey(), entry.getValue());
            }
        }

        super.saveConfig();
    }

    @ModuleTask(event = ModuleLifeCycle.STARTED, order = 64)
    public void registerListeners() {
        super.registerListeners(new IncludePluginListener(this.configuration));
    }

    public Collection<CloudNPC> loadNPCs() {
        IDatabase database = super.getDatabaseProvider().getDatabase(DefaultModuleHelper.DEFAULT_CONFIGURATION_DATABASE_NAME);
        JsonDocument document = database.get(NPC_DOCUMENT_NAME);

        return document == null ? new HashSet<>() : document.get(DOCUMENT_NPC_KEY, NPCConstants.NPC_COLLECTION_TYPE);
    }

    public void saveNPCs(Collection<CloudNPC> npcs) {
        IDatabase database = super.getDatabaseProvider().getDatabase(DefaultModuleHelper.DEFAULT_CONFIGURATION_DATABASE_NAME);

        database.update(NPC_DOCUMENT_NAME, new JsonDocument(DOCUMENT_NPC_KEY, npcs));
    }

    public NPCConfiguration getConfiguration() {
        return configuration;
    }

}
