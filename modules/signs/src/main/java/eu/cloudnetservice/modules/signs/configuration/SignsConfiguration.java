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

package eu.cloudnetservice.modules.signs.configuration;

import com.google.common.collect.ImmutableMap;
import eu.cloudnetservice.ext.component.ComponentFormat;
import eu.cloudnetservice.ext.component.InternalPlaceholder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

public record SignsConfiguration(@Unmodifiable @NonNull List<SignConfigurationEntry> entries) {

  public static final Map<String, Component> MESSAGES = ImmutableMap.<String, Component>builder()
    .put("command-cloudsign-no-entry",
      Component.text("No configuration entry found for any group the wrapper belongs to.", NamedTextColor.GRAY))
    .put("command-cloudsign-not-looking-at-sign",
      Component.text("You are not facing a sign...", NamedTextColor.GRAY))
    .put("command-cloudsign-create-success",
      Component.text("The target sign with the target group ", NamedTextColor.GRAY)
        .append(InternalPlaceholder.create("group").color(NamedTextColor.GOLD))
        .append(Component.text(" was successfully created.")))
    .put("command-cloudsign-remove-not-existing",
      Component.text("The target sign is not registered as a cloud sign.", NamedTextColor.GRAY))
    .put("command-cloudsign-remove-success",
      Component.text("Removing the target sign. Please wait...", NamedTextColor.GRAY))
    .put("command-cloudsign-bulk-remove-success",
      Component.text("Removing ", NamedTextColor.GRAY)
        .append(InternalPlaceholder.create("amount").color(NamedTextColor.GOLD))
        .append(Component.text(" signs. Please wait..."))
    )
    .put("command-cloudsign-sign-already-exist",
      Component.text("The sign is already set. If you want to remove it, use '/cs remove'.", NamedTextColor.GRAY)
    )
    .put("command-cloudsign-cleanup-success",
      InternalPlaceholder.create("amount").color(NamedTextColor.GOLD)
        .append(Component.text(" non-existing signs were removed successfully.", NamedTextColor.GRAY))
    )
    .build();

  public static <C> void sendMessage(@NonNull String key, @NonNull ComponentFormat<C> componentFormat, @NonNull Consumer<C> sender) {
    sendMessage(key, componentFormat, sender, null);
  }

  public static <C> void sendMessage(
    @NonNull String key,
    @NonNull ComponentFormat<C> componentFormat,
    @NonNull Consumer<C> sender,
    @Nullable Map<String, Component> placeholders
  ) {
    var message = MESSAGES.get(key);
    if (message != null) {
      if (placeholders != null) {
        message = InternalPlaceholder.replacePlaceholders(message, placeholders);
      }
      sender.accept(componentFormat.fromAdventure(message));
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

    public @NonNull Builder modifyEntries(@NonNull Consumer<Collection<SignConfigurationEntry>> modifier) {
      modifier.accept(this.entries);
      return this;
    }

    public @NonNull SignsConfiguration build() {
      return new SignsConfiguration(List.copyOf(this.entries));
    }
  }
}
