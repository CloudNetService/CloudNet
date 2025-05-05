/*
 * Copyright 2019-2024 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cloudnetservice.modules.signs.impl._deprecated.configuration;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.modules.signs.impl._deprecated.configuration.entry.SignConfigurationEntryType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public final class SignConfigurationReaderAndWriter {

  private SignConfigurationReaderAndWriter() {
    throw new UnsupportedOperationException();
  }

  public static void write(SignConfiguration signConfiguration, Path path) {
    Preconditions.checkNotNull(signConfiguration);
    Preconditions.checkNotNull(path);

    Document.newJsonDocument().append("config", signConfiguration).writeTo(path);
  }

  public static SignConfiguration read(Path path) {
    Preconditions.checkNotNull(path);

    var document = DocumentFactory.json().parse(path);
    return read(document, path);
  }

  public static SignConfiguration read(Document.Mutable document, Path path) {
    if (!document.contains("config")) {
      var signConfiguration = new SignConfiguration(
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

    var signConfiguration = document.readObject("config", SignConfiguration.class);
    if (!signConfiguration.getMessages().containsKey("command-cloudsign-cleanup-success")) {
      signConfiguration.getMessages()
        .put("command-cloudsign-cleanup-success", "&7Non-existing signs were removed successfully");
    }

    // new properties in the configuration will be saved
    document.append("config", signConfiguration);
    document.writeTo(path);

    return signConfiguration;
  }
}
