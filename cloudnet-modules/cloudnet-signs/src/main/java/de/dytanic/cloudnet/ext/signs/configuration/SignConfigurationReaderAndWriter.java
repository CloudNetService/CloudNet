package de.dytanic.cloudnet.ext.signs.configuration;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.ext.signs.configuration.entry.SignConfigurationEntryType;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public final class SignConfigurationReaderAndWriter {

  private SignConfigurationReaderAndWriter() {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public static void write(SignConfiguration signConfiguration, File file) {
    write(signConfiguration, file.toPath());
  }

  public static void write(SignConfiguration signConfiguration, Path path) {
    Preconditions.checkNotNull(signConfiguration);
    Preconditions.checkNotNull(path);

    JsonDocument.newDocument("config", signConfiguration).write(path);
  }

  @Deprecated
  public static SignConfiguration read(File file) {
    return read(file.toPath());
  }

  public static SignConfiguration read(Path path) {
    Preconditions.checkNotNull(path);

    JsonDocument document = JsonDocument.newDocument(path);
    if (!document.contains("config")) {
      SignConfiguration signConfiguration = new SignConfiguration(
        new ArrayList<>(Collections.singletonList(SignConfigurationEntryType.BUKKIT.createEntry("Lobby"))),
        new HashMap<>(ImmutableMap.of(
          "server-connecting-message", "&7You will be moved to &c%server%&7...",
          "command-cloudsign-create-success",
          "&7The target sign with the target group &6%group% &7is successfully created.",
          "command-cloudsign-remove-success", "&7The target sign will removed! Please wait...",
          "command-cloudsign-sign-already-exist",
          "&7The sign is already set. If you want to remove that, use the /cloudsign remove command",
          "command-cloudsign-cleanup-success", "&7Non-existing signs were removed successfully"
        ))
      );

      write(signConfiguration, path);
      return signConfiguration;
    }

    SignConfiguration signConfiguration = document.get("config", SignConfiguration.TYPE);
    if (!signConfiguration.getMessages().containsKey("command-cloudsign-cleanup-success")) {
      signConfiguration.getMessages()
        .put("command-cloudsign-cleanup-success", "&7Non-existing signs were removed successfully");
    }

    // new properties in the configuration will be saved
    document.append("config", signConfiguration);
    document.write(path);

    return signConfiguration;
  }
}
