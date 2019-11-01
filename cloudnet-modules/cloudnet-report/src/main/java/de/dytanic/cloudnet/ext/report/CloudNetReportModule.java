package de.dytanic.cloudnet.ext.report;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.ext.report.command.CommandPaste;
import de.dytanic.cloudnet.ext.report.command.CommandReport;
import de.dytanic.cloudnet.ext.report.listener.CloudNetReportListener;
import de.dytanic.cloudnet.ext.report.util.PasteServerType;
import de.dytanic.cloudnet.module.NodeCloudNetModule;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class CloudNetReportModule extends NodeCloudNetModule {

    private static CloudNetReportModule instance;

    private volatile Class<? extends Event> eventClass;

    private File savingRecordsDirectory;

    public static CloudNetReportModule getInstance() {
        return CloudNetReportModule.instance;
    }

    @ModuleTask(order = 127, event = ModuleLifeCycle.LOADED)
    public void init() {
        instance = this;
    }

    @ModuleTask(order = 127, event = ModuleLifeCycle.STARTED)
    public void initConfig() {
        getConfig().getBoolean("savingRecords", true);
        getConfig().getString("recordDestinationDirectory", "records");
        getConfig().get("pasteServerType", PasteServerType.class, PasteServerType.HASTE);
        getConfig().getString("pasteServerUrl", "https://hasteb.in");

        saveConfig();
    }

    @ModuleTask(order = 126, event = ModuleLifeCycle.STARTED)
    public void initSavingRecordsDirectory() {
        this.savingRecordsDirectory = new File(getModuleWrapper().getDataFolder(), getConfig().getString("recordDestinationDirectory"));
        this.savingRecordsDirectory.mkdirs();
    }

    @ModuleTask(order = 64, event = ModuleLifeCycle.STARTED)
    public void registerListeners() {
        registerListener(new CloudNetReportListener());
    }

    @ModuleTask(order = 16, event = ModuleLifeCycle.STARTED)
    public void registerCommands() {
        registerCommand(new CommandReport());
        registerCommand(new CommandPaste());
    }

    public String getPasteURL() {
        return this.getConfig().getString("pasteServerUrl");
    }


    public String executePaste(String context) {
        Validate.checkNotNull(context);

        try {
            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(this.getPasteURL() + "/documents").openConnection();

            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
            httpURLConnection.setRequestProperty("Accept-Language", "en-En,en;q=0.5");
            httpURLConnection.setDoOutput(true);
            httpURLConnection.connect();

            if (getConfig().get("pasteServerType", PasteServerType.class) == PasteServerType.HASTE) {
                try (DataOutputStream writer = new DataOutputStream(httpURLConnection.getOutputStream())) {
                    writer.writeBytes(context);
                    writer.flush();
                }
            }

            String input;
            try (InputStream inputStream = httpURLConnection.getInputStream()) {
                input = new String(FileUtils.toByteArray(inputStream), StandardCharsets.UTF_8);
            }

            JsonDocument jsonDocument = JsonDocument.newDocument(input);

            return this.getPasteURL() + "/" + jsonDocument.getString("key") +
                    (jsonDocument.contains("deleteSecret") ? " DeleteSecret: " + jsonDocument.getString("deleteSecret") : "");

        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return null;
    }

    public Class<? extends Event> getEventClass() {
        return this.eventClass;
    }

    public void setEventClass(Class<? extends Event> eventClass) {
        this.eventClass = eventClass;
    }

    public File getSavingRecordsDirectory() {
        return this.savingRecordsDirectory;
    }
}