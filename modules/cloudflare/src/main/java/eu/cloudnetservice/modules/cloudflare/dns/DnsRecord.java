/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.cloudflare.dns;

import eu.cloudnetservice.common.document.gson.JsonDocument;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public class DnsRecord {

  // the id of the record, only used in responses from the CF api
  private final String id;

  private final String type;
  private final String name;
  private final String content;

  private final int ttl;
  private final boolean proxied;

  private final JsonDocument data;

  public DnsRecord(
    @NonNull DnsType type,
    @NonNull String name,
    @NonNull String content,
    int ttl,
    boolean proxied,
    @NonNull JsonDocument data
  ) {
    this.id = null;
    this.type = type.name();
    this.name = name;
    this.content = content;
    this.ttl = ttl;
    this.proxied = proxied;
    this.data = data;
  }

  public @UnknownNullability String id() {
    return this.id;
  }

  public @NonNull String type() {
    return this.type;
  }

  public @NonNull String name() {
    return this.name;
  }

  public @NonNull String content() {
    return this.content;
  }

  public int ttl() {
    return this.ttl;
  }

  public boolean proxied() {
    return this.proxied;
  }

  public @Nullable JsonDocument data() {
    return this.data;
  }
}
