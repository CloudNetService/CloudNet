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

package eu.cloudnetservice.node.cluster.sync.prettyprint;

import dev.derklaro.gulf.diff.Change;
import dev.derklaro.gulf.diff.array.ArrayChange;
import dev.derklaro.gulf.diff.array.CollectionChange;
import dev.derklaro.gulf.diff.map.MapChange;
import dev.derklaro.gulf.path.ObjectPath;
import java.util.Collection;
import java.util.Map;
import lombok.NonNull;

public final class GulfPrettyPrint {

  private GulfPrettyPrint() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull String[] prettyPrint(@NonNull String entryName, @NonNull Collection<Change<Object>> changes) {
    // initial information
    var builder = new StringBuilder("&rThere were &6")
      .append(changes.size())
      .append("&r changes to synced element &c")
      .append(entryName)
      .append("&r:")
      .append(System.lineSeparator());
    // append all changes
    printChanges(builder, " ", changes);
    // stringify
    return builder.append(System.lineSeparator()).toString().split(System.lineSeparator());
  }

  public static void printChanges(
    @NonNull StringBuilder builder,
    @NonNull String indent,
    @NonNull Collection<Change<Object>> changes
  ) {
    for (var change : changes) {
      if (change instanceof ArrayChange<Object> arrayChange) {
        // changes in an array
        GulfArrayDiffPrinter.printArrayChange(builder, indent, arrayChange);
      } else if (change instanceof CollectionChange<Object, ? super Collection<Object>> collectionChange) {
        // changes in a collection
        GulfArrayDiffPrinter.printCollectionChange(builder, indent, collectionChange);
      } else if (change instanceof MapChange<Object, Object, ? super Map<Object, Object>> mapChange) {
        // changes in a map
        GulfMapDiffPrinter.printMapChange(builder, indent, mapChange);
      } else {
        // general change
        printValueChange(builder, indent, change);
      }
    }
  }

  public static void printValueChange(
    @NonNull StringBuilder builder,
    @NonNull String indent,
    @NonNull Change<Object> change
  ) {
    appendPathInformation(builder, indent, change.path())
      .append(": &c").append(change.leftElement()) // red: old value
      .append(" &r=> &a").append(change.rightElement()) // green: new value
      .append(System.lineSeparator());
  }

  public static @NonNull StringBuilder appendPathInformation(
    @NonNull StringBuilder builder,
    @NonNull String indent,
    @NonNull ObjectPath path
  ) {
    return builder.append(indent).append("&r- &6").append(path.toFullPath()).append("&r");
  }

  public static @NonNull String pad(@NonNull String currentIndent) {
    return currentIndent + "  ";
  }
}
