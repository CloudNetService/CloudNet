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

import com.google.common.primitives.Ints;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.console.IConsole;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.javers.core.Changes;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.ListCompareAlgorithm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultDataSyncRegistry implements DataSyncRegistry {

  private static final Logger LOGGER = LogManager.getLogger(DefaultDataSyncRegistry.class);
  private static final Javers JAVERS = JaversBuilder.javers()
    .withInitialChanges(false)
    .withTerminalChanges(false)
    .withListCompareAlgorithm(ListCompareAlgorithm.LEVENSHTEIN_DISTANCE)
    .build();

  private final Map<String, DataSyncHandler<?>> handlers = new ConcurrentHashMap<>();

  @Override
  public void registerHandler(@NotNull DataSyncHandler<?> handler) {
    this.handlers.putIfAbsent(handler.getKey(), handler);
  }

  @Override
  public void unregisterHandler(@NotNull DataSyncHandler<?> handler) {
    this.handlers.remove(handler.getKey());
  }

  @Override
  public void unregisterHandler(@NotNull String handlerKey) {
    this.handlers.remove(handlerKey);
  }

  @Override
  public void unregisterHandler(@NotNull ClassLoader loader) {
    for (Entry<String, DataSyncHandler<?>> entry : this.handlers.entrySet()) {
      if (entry.getValue().getClass().getClassLoader().equals(loader)) {
        this.handlers.remove(entry.getKey());
        break;
      }
    }
  }

  @Override
  public boolean hasHandler(@NotNull String handlerKey) {
    return this.handlers.containsKey(handlerKey);
  }

  @Override
  @SuppressWarnings("unchecked")
  public @NotNull DataBuf.Mutable prepareClusterData(boolean force) {
    // the result data
    DataBuf.Mutable result = DataBuf.empty().writeBoolean(force);
    // append all handler content to the buf
    for (DataSyncHandler<?> handler : this.handlers.values()) {
      // extract the whole content from the handler
      Collection<Object> data = (Collection<Object>) handler.getData();
      // check if there is data
      if (!data.isEmpty()) {
        // check if there is only one argument to write
        if (data.size() == 1) {
          this.serializeData(data.iterator().next(), handler, result);
        } else {
          // serialize the whole result
          for (Object obj : data) {
            this.serializeData(obj, handler, result);
          }
        }
      }
    }
    // the whole data for the cluster sync
    return result;
  }

  @Override
  public @Nullable DataBuf handle(@NotNull DataBuf input, boolean force) {
    // holds the result of the handle - null by default indicates no result
    DataBuf.Mutable result = null;
    // handle the incoming data as long as there is data
    while (input.getReadableBytes() > 0) {
      // The data information
      String key = input.readString();
      DataBuf syncData = input.readDataBuf();
      // get the associated handler with the buf
      DataSyncHandler<?> handler = this.handlers.get(key);
      if (handler != null) {
        try {
          // read the synced data
          Object data = handler.getConverter().parse(syncData);
          Object current = handler.getCurrent(data);
          // check if we need to ask for user input to continue the sync
          if (force || current == null || current.equals(data)) {
            // write the data and continue
            handler.write(data);
            continue;
          }
          // get the diff between the current object and the data
          try {
            Changes diff = JAVERS.compare(current, data).getChanges();
            if (diff.isEmpty()) {
              // no diff detected... just write
              handler.write(data);
              continue;
            }
            // pretty format the changes
            for (String line : JaversPrettyPrint.prettyPrint(handler.getName(current), diff)) {
              LOGGER.warning(line);
            }
            // print out the possibilities the user has now
            LOGGER.info(LanguageManager.getMessage("cluster-sync-change-decision-question"));
            // wait for the decision and apply
            switch (this.waitForCorrectMergeInput(CloudNet.getInstance().getConsole())) {
              case 1:
                // accept theirs - write the change
                handler.write(data);
                LOGGER.info(LanguageManager.getMessage("cluster-sync-accepted-theirs"));
                break;
              case 2:
                // accept yours - check if we already have a result buf
                if (result == null) {
                  result = DataBuf.empty().writeBoolean(true);
                }
                // write the current data to the result buf
                this.serializeData(current, handler, result);
                LOGGER.info(LanguageManager.getMessage("cluster-sync-accept-yours"));
                break;
              case 3:
                // skip the current change
                LOGGER.info(LanguageManager.getMessage("cluster-sync-skip"));
                break;
              default:
                // cannot happen
                break;
            }
          } catch (Exception exception) {
            LOGGER.severe("Exception processing diff on key %s with %s and %s", null, key, data, current);
          }
          // continue reading
          continue;
        } catch (Exception exception) {
          LOGGER.severe("Exception reading data for key %s while syncing", null, key);
        }
      }
      // no handler for the result
      LOGGER.fine("No handler for key %s to sync data", null, key);
    }
    // return the created result
    return result == null ? force ? null : DataBuf.empty().writeBoolean(false) : result;
  }

  protected void serializeData(
    @NotNull Object data,
    @NotNull DataSyncHandler<?> handler,
    @NotNull DataBuf.Mutable target
  ) {
    // append the information
    target.writeString(handler.getKey());
    // write the data
    DataBuf.Mutable buf = DataBuf.empty();
    handler.serialize(buf, data);
    // append the data
    target.writeDataBuf(buf);
  }

  protected int waitForCorrectMergeInput(@NotNull IConsole console) {
    while (true) {
      // wait for an input
      String input = console.readLine().getDef(null);
      // check if an input was supplied
      if (input == null) {
        continue;
      }
      // get the int from the input & validate
      Integer mergeDecision = Ints.tryParse(input);
      if (mergeDecision != null && mergeDecision >= 1 && mergeDecision <= 3) {
        return mergeDecision;
      }
    }
  }
}
