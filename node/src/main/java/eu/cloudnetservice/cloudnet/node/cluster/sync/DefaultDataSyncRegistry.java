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

package eu.cloudnetservice.cloudnet.node.cluster.sync;

import com.google.common.primitives.Ints;
import eu.cloudnetservice.cloudnet.common.language.I18n;
import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.console.Console;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.ListCompareAlgorithm;
import org.jetbrains.annotations.Nullable;

public class DefaultDataSyncRegistry implements DataSyncRegistry {

  private static final Logger LOGGER = LogManager.logger(DefaultDataSyncRegistry.class);
  private static final Javers JAVERS = JaversBuilder.javers()
    .withInitialChanges(false)
    .withTerminalChanges(false)
    .withListCompareAlgorithm(ListCompareAlgorithm.LEVENSHTEIN_DISTANCE)
    .build();

  private final Map<String, DataSyncHandler<?>> handlers = new ConcurrentHashMap<>();

  @Override
  public void registerHandler(@NonNull DataSyncHandler<?> handler) {
    this.handlers.putIfAbsent(handler.key(), handler);
  }

  @Override
  public void unregisterHandler(@NonNull DataSyncHandler<?> handler) {
    this.handlers.remove(handler.key());
  }

  @Override
  public void unregisterHandler(@NonNull String handlerKey) {
    this.handlers.remove(handlerKey);
  }

  @Override
  public void unregisterHandler(@NonNull ClassLoader loader) {
    for (var entry : this.handlers.entrySet()) {
      if (entry.getValue().getClass().getClassLoader().equals(loader)) {
        this.handlers.remove(entry.getKey());
        break;
      }
    }
  }

  @Override
  public boolean hasHandler(@NonNull String handlerKey) {
    return this.handlers.containsKey(handlerKey);
  }

  @Override
  @SuppressWarnings("unchecked")
  public @NonNull DataBuf.Mutable prepareClusterData(boolean force, String @NonNull ... selectedHandlers) {
    // sort the handlers for later binary searches
    Arrays.sort(selectedHandlers);
    // the result data
    var result = DataBuf.empty().writeBoolean(force);
    // append all handler content to the buf
    for (var handler : this.handlers.values()) {
      // check if we should include the handler
      if (selectedHandlers.length > 0 && Arrays.binarySearch(selectedHandlers, handler.key()) < 0) {
        continue;
      }
      // extract the whole content from the handler
      var data = (Collection<Object>) handler.data();
      // check if there is data
      if (!data.isEmpty()) {
        // check if there is only one argument to write
        if (data.size() == 1) {
          this.serializeData(data.iterator().next(), handler, result);
        } else {
          // serialize the whole result
          for (var obj : data) {
            this.serializeData(obj, handler, result);
          }
        }
      }
    }
    // the whole data for the cluster sync
    return result;
  }

  @Override
  public @Nullable DataBuf handle(@NonNull DataBuf input, boolean force) {
    // holds the result of the handle - null by default indicates no result
    DataBuf.Mutable result = null;
    // handle the incoming data as long as there is data
    while (input.readableBytes() > 0) {
      // The data information
      var key = input.readString();
      try (var syncData = input.readDataBuf()) {
        // get the associated handler with the buf
        var handler = this.handlers.get(key);
        if (handler != null) {
          // read the synced data
          var data = handler.converter().parse(syncData);
          var current = handler.current(data);
          // check if we need to ask for user input to continue the sync
          if (force || handler.alwaysForceApply() || current == null || current.equals(data)) {
            // write the data and continue
            handler.write(data);
            continue;
          }
          // get the diff between the current object and the data
          try {
            var diff = JAVERS.compare(current, data).getChanges();
            if (diff.isEmpty()) {
              // no diff detected... just write
              handler.write(data);
              continue;
            }
            // pretty format the changes
            for (var line : JaversPrettyPrint.prettyPrint(handler.name(current), diff)) {
              LOGGER.warning(line);
            }
            // print out the possibilities the user has now
            LOGGER.info(I18n.trans("cluster-sync-change-decision-question"));
            // wait for the decision and apply
            switch (this.waitForCorrectMergeInput(CloudNet.instance().console())) {
              case 1 -> {
                // accept theirs - write the change
                handler.write(data);
                LOGGER.info(I18n.trans("cluster-sync-accepted-theirs"));
              }
              case 2 -> {
                // accept yours - check if we already have a result buf
                if (result == null) {
                  result = DataBuf.empty().writeBoolean(true);
                }
                // write the current data to the result buf
                this.serializeData(current, handler, result);
                LOGGER.info(I18n.trans("cluster-sync-accept-yours"));
              }
              case 3 ->
                  // skip the current change
                  LOGGER.info(I18n.trans("cluster-sync-skip"));
              default -> {
              }
              // cannot happen
            }
          } catch (Exception exception) {
            LOGGER.severe("Exception processing diff on key %s with %s and %s", null, key, data, current);
          }
          // continue reading
          continue;
        }
      } catch (Exception exception) {
        LOGGER.severe("Exception reading data for key %s while syncing", null, key);
      }
      // no handler for the result
      LOGGER.fine("No handler for key %s to sync data", null, key);
    }
    // try to release the input buf
    input.release();
    // return the created result
    return result == null ? force ? null : DataBuf.empty().writeBoolean(false) : result;
  }

  protected void serializeData(
    @NonNull Object data,
    @NonNull DataSyncHandler<?> handler,
    @NonNull DataBuf.Mutable target
  ) {
    // append the information
    target.writeString(handler.key());
    // write the data
    var buf = DataBuf.empty();
    handler.serialize(buf, data);
    // append the data
    target.writeDataBuf(buf);
  }

  protected int waitForCorrectMergeInput(@NonNull Console console) {
    try {
      // disable all handlers of the console to prevent skips
      console.disableAllHandlers();
      // read & wait for a correct input
      return this.readMergeInput(console);
    } finally {
      // re-enable all handlers
      console.enableAllHandlers();
    }
  }

  protected int readMergeInput(@NonNull Console console) {
    while (true) {
      // wait for an input
      var input = console.readLine().getDef(null);
      // check if an input was supplied
      if (input == null) {
        continue;
      }
      // get the int from the input & validate
      var mergeDecision = Ints.tryParse(input);
      if (mergeDecision != null && mergeDecision >= 1 && mergeDecision <= 3) {
        return mergeDecision;
      }
    }
  }
}
