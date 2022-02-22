/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.signs.configuration;

import com.google.common.collect.ImmutableMap;
import eu.cloudnetservice.modules.signs.node.configuration.SignConfigurationType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class SignsConfiguration {

  public static final Map<String, String> DEFAULT_MESSAGES = ImmutableMap.<String, String>builder()
    .put("server-connecting-message", "§7You will be moved to §c%server%§7...")
    .put("command-cloudsign-no-entry", "§7No configuration entry found for any group the wrapper belongs to.")
    .put("command-cloudsign-not-looking-at-sign", "§7You are not facing a sign...")
    .put("command-cloudsign-create-success",
      "§7The target sign with the target group §6%group% §7was successfully created.")
    .put("command-cloudsign-remove-not-existing", "§7The target sign is not registered as a cloud sign.")
    .put("command-cloudsign-remove-success", "§7Removing the target sign. Please wait...")
    .put("command-cloudsign-bulk-remove-success", "§7Removing §6%amount% §7signs. Please wait...")
    .put("command-cloudsign-sign-already-exist",
      "§7The sign is already set. If you want to remove it, use '/cs remove'.")
    .put("command-cloudsign-cleanup-success", "§6%amount% §7non-existing signs were removed successfully.")
    .build();

  protected Map<String, String> messages;
  protected Collection<SignConfigurationEntry> configurationEntries;

  public SignsConfiguration() {
  }

  public SignsConfiguration(Map<String, String> messages, Collection<SignConfigurationEntry> configurationEntries) {
    this.messages = messages;
    this.configurationEntries = configurationEntries;
  }

  public static SignsConfiguration createDefaultJava(@NonNull String group) {
    return new SignsConfiguration(
      new HashMap<>(DEFAULT_MESSAGES),
      new ArrayList<>(Collections.singleton(SignConfigurationType.JAVA.createEntry(group)))
    );
  }

  public static SignsConfiguration createDefaultBedrock(@NonNull String group) {
    return new SignsConfiguration(
      new HashMap<>(DEFAULT_MESSAGES),
      new ArrayList<>(Collections.singleton(SignConfigurationType.BEDROCK.createEntry(group)))
    );
  }

  public Map<String, String> messages() {
    return this.messages;
  }

  public void messages(Map<String, String> messages) {
    this.messages = messages;
  }

  public void sendMessage(@NonNull String key, @NonNull Consumer<String> messageSender) {
    this.sendMessage(key, messageSender, null);
  }

  public void sendMessage(@NonNull String key, @NonNull Consumer<String> messageSender,
    @Nullable Function<String, String> modifier) {
    var message = this.messages().getOrDefault(key, DEFAULT_MESSAGES.get(key));
    if (message != null) {
      if (modifier != null) {
        message = modifier.apply(message);
      }
      messageSender.accept(message);
    }
  }

  public Collection<SignConfigurationEntry> configurationEntries() {
    return this.configurationEntries;
  }

  public void configurationEntries(Collection<SignConfigurationEntry> configurationEntries) {
    this.configurationEntries = configurationEntries;
  }

  public boolean hasEntry(@NonNull String group) {
    return this.configurationEntries.stream().anyMatch(entry -> entry.targetGroup().equals(group));
  }
}
