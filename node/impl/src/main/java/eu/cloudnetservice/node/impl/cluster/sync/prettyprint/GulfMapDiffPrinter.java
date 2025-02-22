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

package eu.cloudnetservice.node.impl.cluster.sync.prettyprint;

import dev.derklaro.gulf.diff.map.MapChange;
import dev.derklaro.gulf.diff.map.MapEntryAddOrRemove;
import dev.derklaro.gulf.diff.map.MapEntryChange;
import java.util.Map;
import lombok.NonNull;

public final class GulfMapDiffPrinter {

  private GulfMapDiffPrinter() {
    throw new UnsupportedOperationException();
  }

  public static void printMapChange(
    @NonNull StringBuilder builder,
    @NonNull String indent,
    @NonNull MapChange<Object, Object, Map<Object, Object>> change,
    boolean path
  ) {
    // append the path information
    GulfPrettyPrint.appendPathInformation(builder, indent, change.path(), GulfPrettyPrint::appendLineSeparator, path);

    // print all element changes
    var elementIndent = GulfPrettyPrint.pad(indent);
    for (var diff : change.entryChanges()) {
      if (diff instanceof MapEntryAddOrRemove<Object, Object> addOrRemove) {
        // element added or removed
        printMapEntryAddOrRemove(builder, elementIndent, addOrRemove);
      } else if (diff instanceof MapEntryChange<Object, Object> entryChange) {
        // element changed
        printElementChange(builder, elementIndent, entryChange);
      }
    }
  }

  private static void printMapEntryAddOrRemove(
    @NonNull StringBuilder builder,
    @NonNull String indent,
    @NonNull MapEntryAddOrRemove<Object, Object> change
  ) {
    // append the key information
    var color = change.elementRemoved() ? "&c" : "&a";
    builder.append(indent).append(color).append(change.key()).append("&r").append(System.lineSeparator());

    // get the diff between the removed values
    var changes = GulfHelper.findChanges(change.leftElement(), change.rightElement());
    GulfPrettyPrint.printChanges(builder, indent, changes, false);
  }

  private static void printElementChange(
    @NonNull StringBuilder builder,
    @NonNull String indent,
    @NonNull MapEntryChange<Object, Object> change
  ) {
    // append the key information
    builder.append(indent).append("&6").append(change.key()).append("&r").append(System.lineSeparator());
    GulfPrettyPrint.printChanges(builder, indent, change.changes(), false);
  }
}
