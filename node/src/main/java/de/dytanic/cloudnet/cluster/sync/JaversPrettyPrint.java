/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.cluster.sync;

import lombok.NonNull;
import org.javers.core.Changes;
import org.javers.core.diff.changetype.ValueChange;
import org.javers.core.diff.changetype.container.ContainerChange;
import org.javers.core.diff.changetype.container.ElementValueChange;
import org.javers.core.diff.changetype.container.ValueAddOrRemove;
import org.javers.core.diff.changetype.container.ValueRemoved;
import org.javers.core.diff.changetype.map.EntryAddOrRemove;
import org.javers.core.diff.changetype.map.EntryRemoved;
import org.javers.core.diff.changetype.map.EntryValueChange;
import org.javers.core.diff.changetype.map.MapChange;

// LEFT OLD
// RIGHT NEW

final class JaversPrettyPrint {

  private JaversPrettyPrint() {
    throw new UnsupportedOperationException();
  }

  public static String @NonNull [] prettyPrint(@NonNull String entryName, @NonNull Changes changes) {
    // initial information
    var builder = new StringBuilder("&rThere were &6")
      .append(changes.size())
      .append("&r changes to synced element &c")
      .append(entryName)
      .append("&r:")
      .append(System.lineSeparator());
    // append all changes
    for (var change : changes) {
      // supported changes: values, container (collection & array), maps
      if (change instanceof ValueChange) {
        printValueChange(builder, (ValueChange) change);
      } else if (change instanceof ContainerChange<?>) {
        printContainerChange(builder, (ContainerChange<?>) change);
      } else if (change instanceof MapChange<?>) {
        printMapChange(builder, (MapChange<?>) change);
      }
    }
    // stringify
    return builder.append(System.lineSeparator()).toString().split(System.lineSeparator());
  }

  private static void printValueChange(@NonNull StringBuilder builder, @NonNull ValueChange change) {
    // go over the possible change types
    switch (change.getChangeType()) {
      case PROPERTY_VALUE_CHANGED -> builder
          .append(" &r- &6").append(change.getPropertyName()).append("&r: ") // name of changed property
          .append("&c").append(change.getLeft()) // red: old value
          .append(" &r=> &a").append(change.getRight()) // green: new value
          .append(System.lineSeparator());
      case PROPERTY_ADDED ->
          // right present, left absent
          builder
              .append(" &r- &6").append(change.getPropertyName()).append("&r: ") // name of changed property
              .append("&c<empty> &r=> &a") // absent before
              .append(change.getRight()) // value of new property
              .append(System.lineSeparator());
      case PROPERTY_REMOVED ->
          // left present, right absent
          builder
              .append(" &r- &6").append(change.getPropertyName()).append("&r: ") // name of changed property
              .append("&c").append(change.getLeft()) // value of the old property
              .append(" &r=> &a<empty>") // absent now
              .append(System.lineSeparator());
      default -> {
      }
    }
  }

  // Container change print helpers (collections & arrays)

  private static void printContainerChange(@NonNull StringBuilder builder, @NonNull ContainerChange<?> change) {
    // initial information
    builder
      .append(" &r- &6").append(change.getPropertyName()) // name of the changed property
      .append(" &r(").append(change.getChanges().size()).append(" elements)") // amount of changed elements
      .append(System.lineSeparator());
    // print all changes of the container
    for (var containerElementChange : change.getChanges()) {
      // 3 possibilities - element at the index changes, was removed or replaced
      if (containerElementChange instanceof ElementValueChange) {
        printElementValueChange(builder, (ElementValueChange) containerElementChange);
      } else if (containerElementChange instanceof ValueAddOrRemove) {
        // print the remove or add, use red for removed elements and green for added elements
        printElementAddOrRemoveChange(
          builder,
          (ValueAddOrRemove) containerElementChange,
          containerElementChange instanceof ValueRemoved ? "&c" : "&a");
      }
    }
  }

  private static void printElementValueChange(
    @NonNull StringBuilder builder,
    @NonNull ElementValueChange change
  ) {
    builder
      .append("    at index &6").append(change.getIndex()).append("&r: ")
      .append("&c").append(change.getLeftValue())
      .append(" &r=> &a").append(change.getRightValue())
      .append(System.lineSeparator());
  }

  private static void printElementAddOrRemoveChange(
    @NonNull StringBuilder builder,
    @NonNull ValueAddOrRemove change,
    @NonNull String colorPrefix
  ) {
    builder
      .append("    at index &6").append(change.getIndex()).append("&r: ")
      .append(colorPrefix).append(change.getValue())
      .append(System.lineSeparator());
  }

  // Map change print helpers

  private static void printMapChange(@NonNull StringBuilder builder, @NonNull MapChange<?> change) {
    // initial information
    builder
      .append(" &r- &6").append(change.getPropertyName()) // name of the changed property
      .append(" &r(").append(change.getEntryChanges().size()).append(" elements)") // amount of changed elements
      .append(System.lineSeparator());
    // print all changes of the map
    for (var entryChange : change.getEntryChanges()) {
      // 3 possibilities - element changed, was removed or replaced
      if (entryChange instanceof EntryValueChange) {
        printEntryValueChange(builder, (EntryValueChange) entryChange);
      } else if (entryChange instanceof EntryAddOrRemove) {
        // print the removed or added value, use red for removed keys and green for added keys
        printEntryAddOrRemoveChange(
          builder,
          (EntryAddOrRemove) entryChange,
          entryChange instanceof EntryRemoved ? "&c" : "&a");
      }
    }
  }

  private static void printEntryValueChange(
    @NonNull StringBuilder builder,
    @NonNull EntryValueChange change
  ) {
    builder
      .append("    at &6").append(change.getKey()).append("&r: ")
      .append("&c").append(change.getLeftValue())
      .append(" &r=> &a").append(change.getRightValue())
      .append(System.lineSeparator());
  }

  private static void printEntryAddOrRemoveChange(
    @NonNull StringBuilder builder,
    @NonNull EntryAddOrRemove change,
    @NonNull String colorPrefix
  ) {
    builder
      .append("    at &6").append(change.getKey()).append("&r: ")
      .append(colorPrefix).append(change.getValue())
      .append(System.lineSeparator());
  }
}
