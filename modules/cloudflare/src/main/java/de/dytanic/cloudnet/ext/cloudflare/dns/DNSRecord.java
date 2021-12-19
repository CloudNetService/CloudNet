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

package de.dytanic.cloudnet.ext.cloudflare.dns;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;

public class DNSRecord {

  private final String type;
  private final String name;
  private final String content;

  private final int ttl;
  private final boolean proxied;

  private final JsonDocument data;

  public DNSRecord(String type, String name, String content, int ttl, boolean proxied, JsonDocument data) {
    this.type = type;
    this.name = name;
    this.content = content;
    this.ttl = ttl;
    this.proxied = proxied;
    this.data = data;
  }

  public String type() {
    return this.type;
  }

  public String name() {
    return this.name;
  }

  public String content() {
    return this.content;
  }

  public int ttl() {
    return this.ttl;
  }

  public boolean proxied() {
    return this.proxied;
  }

  public JsonDocument data() {
    return this.data;
  }
}
