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

package eu.cloudnetservice.node.cluster.sync;

import eu.cloudnetservice.driver.network.buffer.DataBuf;
import java.util.function.Predicate;
import lombok.NonNull;
import org.jetbrains.annotations.UnknownNullability;

public interface DataSyncRegistry {

  void registerHandler(@NonNull DataSyncHandler<?> handler);

  void unregisterHandler(@NonNull DataSyncHandler<?> handler);

  void unregisterHandler(@NonNull String handlerKey);

  void unregisterHandler(@NonNull ClassLoader loader);

  boolean hasHandler(@NonNull String handlerKey);

  @NonNull DataBuf.Mutable prepareClusterData(boolean force, String @NonNull ... selectedHandlers);

  @NonNull DataBuf.Mutable prepareClusterData(boolean force, @NonNull Predicate<DataSyncHandler<?>> handlerFilter);

  @UnknownNullability DataBuf handle(@NonNull DataBuf input, boolean force);
}
