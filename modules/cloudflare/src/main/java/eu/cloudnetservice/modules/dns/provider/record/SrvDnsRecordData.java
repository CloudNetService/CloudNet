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

package eu.cloudnetservice.modules.dns.provider.record;

import lombok.NonNull;
import org.jetbrains.annotations.Range;

public record SrvDnsRecordData(
  @NonNull String name,
  int ttl,
  @NonNull String target,
  @Range(from = 0, to = 0xFFFF) int port,
  @Range(from = 0, to = 0xFFFF) int priority,
  @Range(from = 0, to = 0xFFFF) int weight
) implements DnsRecordData {

  @Override
  public @NonNull String type() {
    return "SRV";
  }
}
