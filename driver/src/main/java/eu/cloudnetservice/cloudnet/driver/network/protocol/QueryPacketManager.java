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

package eu.cloudnetservice.cloudnet.driver.network.protocol;

import eu.cloudnetservice.cloudnet.common.concurrent.CompletableTask;
import eu.cloudnetservice.cloudnet.driver.network.NetworkChannel;
import java.util.Map;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

public interface QueryPacketManager {

  long queryTimeoutMillis();

  @NonNull NetworkChannel networkChannel();

  @NonNull
  @UnmodifiableView Map<UUID, CompletableTask<Packet>> waitingHandlers();

  boolean hasWaitingHandler(@NonNull UUID queryUniqueId);

  boolean unregisterWaitingHandler(@NonNull UUID queryUniqueId);

  @Nullable CompletableTask<Packet> waitingHandler(@NonNull UUID queryUniqueId);

  @NonNull CompletableTask<Packet> sendQueryPacket(@NonNull Packet packet);

  @NonNull CompletableTask<Packet> sendQueryPacket(@NonNull Packet packet, @NonNull UUID queryUniqueId);
}
