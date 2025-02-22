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

package eu.cloudnetservice.modules.influx.impl.util;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.NonNull;

public final class PointUtil {

  private PointUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull Point point(@NonNull String measurement) {
    return Point.measurement(measurement).time(System.currentTimeMillis(), WritePrecision.MS);
  }
}
