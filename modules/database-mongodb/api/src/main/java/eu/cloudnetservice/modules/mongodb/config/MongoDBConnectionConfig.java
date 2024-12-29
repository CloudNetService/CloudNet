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

package eu.cloudnetservice.modules.mongodb.config;

import com.google.common.base.Strings;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class MongoDBConnectionConfig {

  private final String databaseServiceName;

  private final String host;
  private final int port;

  private final String authSource;
  private final String username;
  private final String password;

  private final String database;

  private final String overridingConnectionUri;

  public MongoDBConnectionConfig() {
    this(
      "mongodb",
      "127.0.0.1",
      27017,
      "admin",
      "cloudnet",
      "",
      "cn_db",
      null
    );
  }

  public MongoDBConnectionConfig(
    String databaseServiceName,
    String host,
    int port,
    String authSource,
    String username,
    String password,
    String database,
    String overridingConnectionUri
  ) {
    this.databaseServiceName = databaseServiceName;
    this.host = host;
    this.port = port;
    this.authSource = authSource;
    this.username = username;
    this.password = password;
    this.database = database;
    this.overridingConnectionUri = overridingConnectionUri;
  }

  public String databaseServiceName() {
    return this.databaseServiceName;
  }

  public String host() {
    return this.host;
  }

  public int port() {
    return this.port;
  }

  public String authSource() {
    return this.authSource;
  }

  public String username() {
    return this.username;
  }

  public String password() {
    return this.password;
  }

  public String database() {
    return this.database;
  }

  public String overridingConnectionUri() {
    return this.overridingConnectionUri;
  }

  public String buildConnectionUri() {
    if (!Strings.isNullOrEmpty(this.overridingConnectionUri)) {
      return this.overridingConnectionUri;
    }

    var authParams = Strings.isNullOrEmpty(this.username) && Strings.isNullOrEmpty(this.password)
      ? ""
      : String.format("%s:%s@", this.encodeUrl(this.username), this.encodeUrl(this.password));
    var authSource = Strings.isNullOrEmpty(this.authSource) ? "" : String.format("/?authSource=%s", this.authSource);
    // auth @ host:port / auth source
    return String.format("mongodb://%s%s:%d%s", authParams, this.host, this.port, authSource);
  }

  private String encodeUrl(String input) {
    return URLEncoder.encode(input, StandardCharsets.UTF_8);
  }
}
