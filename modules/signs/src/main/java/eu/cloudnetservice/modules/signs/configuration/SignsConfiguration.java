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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record SignsConfiguration(@NonNull List<SignConfigurationEntry> entries) {

  public static final Map<String, String> MESSAGES = ImmutableMap.<String, String>builder()
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

  public static void sendMessage(@NonNull String key, @NonNull Consumer<String> sender) {
    sendMessage(key, sender, null);
  }

  public static void sendMessage(
    @NonNull String key,
    @NonNull Consumer<String> sender,
    @Nullable Function<String, String> modifier
  ) {
    var message = MESSAGES.get(key);
    if (message != null) {
      if (modifier != null) {
        message = modifier.apply(message);
      }
      sender.accept(message);
    }
  }

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull SignsConfiguration configuration) {
    return builder().entries(configuration.entries());
  }

  public boolean hasEntry(@NonNull String group) {
    return this.entries.stream().anyMatch(entry -> entry.targetGroup().equals(group));
  }

  public static class Builder {

    private List<SignConfigurationEntry> entries = new ArrayList<>();

    public @NonNull Builder entries(@NonNull List<SignConfigurationEntry> entries) {
      this.entries = new ArrayList<>(entries);
      return this;
    }

    public @NonNull Builder addEntry(@NonNull SignConfigurationEntry entry) {
      this.entries.add(entry);
      return this;
    }

    public @NonNull Builder removeEntry(@NonNull SignConfigurationEntry entry) {
      this.entries.remove(entry);
      return this;
    }

    public @NonNull SignsConfiguration build() {
      return new SignsConfiguration(this.entries);
    }
  }
}
