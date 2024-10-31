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

import dev.derklaro.gulf.diff.Change;
import dev.derklaro.gulf.diff.array.ArrayChange;
import dev.derklaro.gulf.diff.array.CollectionChange;
import dev.derklaro.gulf.diff.map.MapChange;
import dev.derklaro.gulf.path.ObjectPath;
import java.util.Collection;
import java.util.function.Consumer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

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
    printChanges(builder, " ", changes, true);
    // stringify
    return builder.append(System.lineSeparator()).toString().split(System.lineSeparator());
  }

  @SuppressWarnings({"rawtypes", "unchecked"}) // generics (╯°□°）╯︵ ┻━┻
  static void printChanges(
    @NonNull StringBuilder builder,
    @NonNull String indent,
    @NonNull Collection<Change<Object>> changes,
    boolean withPath
  ) {
    for (var change : changes) {
      if (change instanceof ArrayChange arrayChange) {
        // changes in an array
        GulfArrayDiffPrinter.printArrayChange(builder, indent, arrayChange, withPath);
      } else if (change instanceof CollectionChange collectionChange) {
        // changes in a collection
        GulfArrayDiffPrinter.printCollectionChange(builder, indent, collectionChange, withPath);
      } else if (change instanceof MapChange mapChange) {
        // changes in a map
        GulfMapDiffPrinter.printMapChange(builder, indent, mapChange, withPath);
      } else {
        // general change
        printValueChange(builder, indent, change, withPath);
      }
    }
  }

  static void printValueChange(
    @NonNull StringBuilder builder,
    @NonNull String indent,
    @NonNull Change<Object> change,
    boolean withPath
  ) {
    appendPathInformation(builder, indent, change.path(), b -> b.append(": "), withPath, true)
      .append("&c").append(change.leftElement()) // red: old value
      .append(" &r=> &a").append(change.rightElement()) // green: new value
      .append(System.lineSeparator());
  }

  static @NonNull StringBuilder appendPathInformation(
    @NonNull StringBuilder builder,
    @NonNull String indent,
    @NonNull ObjectPath path,
    @Nullable Consumer<StringBuilder> finisher,
    boolean doAppendPath
  ) {
    return appendPathInformation(builder, indent, path, finisher, doAppendPath, false);
  }

  static @NonNull StringBuilder appendPathInformation(
    @NonNull StringBuilder builder,
    @NonNull String indent,
    @NonNull ObjectPath path,
    @Nullable Consumer<StringBuilder> finisher,
    boolean doAppendPath,
    boolean forceIndent
  ) {
    // only append the indent if needed
    if (doAppendPath || forceIndent) {
      builder.append(indent);
    }

    if (doAppendPath) {
      // append the path & finisher if needed
      builder.append("&r- &6").append(path.toFullPath()).append("&r");
      if (finisher != null) {
        finisher.accept(builder);
      }
    }
    return builder;
  }

  static @NonNull String pad(@NonNull String currentIndent) {
    return currentIndent + "  ";
  }

  static void appendLineSeparator(@NonNull StringBuilder builder) {
    builder.append(System.lineSeparator());
  }
}
