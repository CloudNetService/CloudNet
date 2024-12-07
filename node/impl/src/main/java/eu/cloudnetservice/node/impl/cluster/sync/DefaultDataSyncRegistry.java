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

package eu.cloudnetservice.node.impl.cluster.sync;

import com.google.common.primitives.Ints;
import dev.derklaro.aerogel.auto.Provides;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.node.cluster.sync.DataSyncHandler;
import eu.cloudnetservice.node.cluster.sync.DataSyncRegistry;
import eu.cloudnetservice.node.impl.cluster.sync.prettyprint.GulfHelper;
import eu.cloudnetservice.node.impl.cluster.sync.prettyprint.GulfPrettyPrint;
import eu.cloudnetservice.node.impl.console.Console;
import eu.cloudnetservice.utils.base.concurrent.TaskUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Provides(DataSyncRegistry.class)
public class DefaultDataSyncRegistry implements DataSyncRegistry {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDataSyncRegistry.class);

  private final I18n i18n;
  private final Console console;
  private final Map<String, DataSyncHandler<?>> handlers = new ConcurrentHashMap<>();

  @Inject
  public DefaultDataSyncRegistry(@NonNull @Service I18n i18n, @NonNull Console console) {
    this.i18n = i18n;
    this.console = console;
  }

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
  public @NonNull DataBuf.Mutable prepareClusterData(boolean force, String @NonNull ... selectedHandlers) {
    // sort the handlers for later binary searches
    Arrays.sort(selectedHandlers);
    return this.prepareClusterData(
      force,
      handler -> selectedHandlers.length == 0 || Arrays.binarySearch(selectedHandlers, handler.key()) >= 0);
  }

  @Override
  @SuppressWarnings("unchecked")
  public @NonNull DataBuf.Mutable prepareClusterData(boolean force, @NonNull Predicate<DataSyncHandler<?>> filter) {
    // the result data
    var result = DataBuf.empty().writeBoolean(force);
    // append all handler content to the buf
    for (var handler : this.handlers.values()) {
      if (filter.test(handler)) {
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
            var changes = GulfHelper.findChanges(current, data);
            if (changes.isEmpty()) {
              // no diff detected... just write
              handler.write(data);
              continue;
            }

            // pretty format the changes
            for (var line : GulfPrettyPrint.prettyPrint(handler.name(current), changes)) {
              LOGGER.warn(line);
            }

            // print out the possibilities the user has now
            LOGGER.warn(this.i18n.translate("cluster-sync-change-decision-question"));
            // wait for the decision and apply
            switch (this.waitForCorrectMergeInput(this.console)) {
              case 1 -> {
                // accept theirs - write the change
                handler.write(data);
                LOGGER.info(this.i18n.translate("cluster-sync-accepted-theirs"));
              }
              case 2 -> {
                // accept yours - check if we already have a result buf
                if (result == null) {
                  result = DataBuf.empty();
                }
                // write the current data to the result buf
                this.serializeData(current, handler, result);
                LOGGER.info(this.i18n.translate("cluster-sync-accept-yours"));
              }
              case 3 ->
                // skip the current change
                LOGGER.info(this.i18n.translate("cluster-sync-skip"));
              default -> {
                // cannot happen
              }
            }
          } catch (Exception exception) {
            LOGGER.error("Exception processing diff on key {} with {} and {}", key, data, current);
          }
          // continue reading
          continue;
        }
      } catch (Exception exception) {
        LOGGER.error("Exception reading data for key {} while syncing", key, exception);
      }
      // no handler for the result
      LOGGER.debug("No handler for key {} to sync data", key);
    }

    // return the created result
    return result;
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
      var input = TaskUtil.getOrDefault(console.readLine(), null);
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
