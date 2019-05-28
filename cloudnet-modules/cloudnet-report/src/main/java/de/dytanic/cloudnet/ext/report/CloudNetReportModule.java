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
import lombok.Getter;
import lombok.Setter;

@Getter
public final class CloudNetReportModule extends NodeCloudNetModule {

  @Getter
  private static CloudNetReportModule instance;

  @Setter
  private volatile Class<? extends Event> eventClass;

  @Getter
  private File savingRecordsDirectory;

  @ModuleTask(order = 127, event = ModuleLifeCycle.LOADED)
  public void init() {
    instance = this;
  }

  @ModuleTask(order = 127, event = ModuleLifeCycle.STARTED)
  public void initConfig() {
    getConfig().getBoolean("savingRecords", true);
    getConfig().getString("recordDestinationDirectory", "records");
    getConfig()
        .get("pasteServerType", PasteServerType.class, PasteServerType.HASTE);
    getConfig().getString("pasteServerUrl", "https://hasteb.in");

    saveConfig();
  }

  @ModuleTask(order = 126, event = ModuleLifeCycle.STARTED)
  public void initSavingRecordsDirectory() {
    this.savingRecordsDirectory = new File(getModuleWrapper().getDataFolder(),
        getConfig().getString("recordDestinationDirectory"));
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

  /*= --------------------------------------------------------------------------------------------------------- =*/

  public String executePaste(String context) {
    Validate.checkNotNull(context);

    try {
      HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(
          getConfig().getString("pasteServerUrl") + "/documents")
          .openConnection();

      httpURLConnection.setRequestMethod("POST");
      httpURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
      httpURLConnection.setRequestProperty("Accept-Language", "en-En,en;q=0.5");
      httpURLConnection.setDoOutput(true);
      httpURLConnection.connect();

      switch (getConfig().get("pasteServerType", PasteServerType.class)) {
        case HASTE: {
          try (DataOutputStream writer = new DataOutputStream(
              httpURLConnection.getOutputStream())) {
            writer.writeBytes(context);
            writer.flush();
          }
        }
        break;
      }

      String input;
      try (InputStream inputStream = httpURLConnection.getInputStream()) {
        input = new String(FileUtils.toByteArray(inputStream),
            StandardCharsets.UTF_8);
      }

      if (input == null) {
        throw new IOException("Response text is null");
      }

      JsonDocument jsonDocument = JsonDocument.newDocument(input);

      return getConfig().getString("pasteServerUrl") + "/" + jsonDocument
          .getString("key") +
          (jsonDocument.contains("deleteSecret") ? " DeleteSecret: "
              + jsonDocument.getString("deleteSecret") : "");

    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }
}