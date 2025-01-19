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
import dev.derklaro.gulf.diff.array.ArrayElementAddOrRemove;
import dev.derklaro.gulf.diff.array.ArrayElementChange;
import dev.derklaro.gulf.diff.array.CollectionChange;
import dev.derklaro.gulf.diff.array.IndexedChange;
import java.util.Collection;
import lombok.NonNull;

public final class GulfArrayDiffPrinter {

  private GulfArrayDiffPrinter() {
    throw new UnsupportedOperationException();
  }

  public static void printArrayChange(
    @NonNull StringBuilder builder,
    @NonNull String indent,
    @NonNull ArrayChange<Object> change,
    boolean path
  ) {
    // append the path information
    GulfPrettyPrint.appendPathInformation(builder, indent, change.path(), GulfPrettyPrint::appendLineSeparator, path);

    // print all element changes
    var elementIndent = GulfPrettyPrint.pad(indent);
    for (var diff : change.elementChanges()) {
      printIndexedChangeChange(builder, elementIndent, diff);
    }
  }

  public static void printCollectionChange(
    @NonNull StringBuilder builder,
    @NonNull String indent,
    @NonNull CollectionChange<Object, Collection<Object>> change,
    boolean path
  ) {
    // append the path information
    GulfPrettyPrint.appendPathInformation(builder, indent, change.path(), GulfPrettyPrint::appendLineSeparator, path);

    // print all element changes
    var elementIndent = GulfPrettyPrint.pad(indent);
    for (var diff : change.elementChanges()) {
      printIndexedChangeChange(builder, elementIndent, diff);
    }
  }

  private static void printIndexedChangeChange(
    @NonNull StringBuilder builder,
    @NonNull String indent,
    @NonNull IndexedChange<Object> change
  ) {
    // append the changed index
    var color = changeColor(change);
    builder
      .append(indent)
      .append("[")
      .append(color)
      .append(change.index())
      .append("&r")
      .append("]:")
      .append(System.lineSeparator());

    // append all element changes at the index
    var elementIndent = GulfPrettyPrint.pad(indent);
    if (change instanceof ArrayElementAddOrRemove<Object> addOrRemove) {
      printElementAddOrRemove(builder, elementIndent, addOrRemove);
    } else if (change instanceof ArrayElementChange<Object> elementChange) {
      printElementChange(builder, elementIndent, elementChange);
    }
  }

  private static void printElementAddOrRemove(
    @NonNull StringBuilder builder,
    @NonNull String indent,
    @NonNull ArrayElementAddOrRemove<Object> change
  ) {
    var changes = GulfHelper.findChanges(change.leftElement(), change.rightElement());
    GulfPrettyPrint.printChanges(builder, indent, changes, false);
  }

  private static void printElementChange(
    @NonNull StringBuilder builder,
    @NonNull String indent,
    @NonNull ArrayElementChange<Object> change
  ) {
    GulfPrettyPrint.printChanges(builder, indent, change.changes(), false);
  }

  private static @NonNull String changeColor(@NonNull Change<Object> change) {
    if (change instanceof ArrayElementAddOrRemove<Object> addOrRemove) {
      return addOrRemove.elementRemoved() ? "&c" : "&a";
    } else {
      return "&6";
    }
  }
}
