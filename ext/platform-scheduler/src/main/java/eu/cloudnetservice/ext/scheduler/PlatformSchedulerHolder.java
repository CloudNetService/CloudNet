/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.ext.scheduler;

import java.util.function.Supplier;

/**
 * Holder for the platform scheduler instance.
 *
 * @since 4.0
 */
final class PlatformSchedulerHolder {

  static final Supplier<PlatformScheduler> INSTANCE = StableValue.supplier(() -> {
    try {
      Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
      return new FoliaPlatformScheduler();
    } catch (ClassNotFoundException e) {
      return new BukkitPlatformScheduler();
    }
  });
}
