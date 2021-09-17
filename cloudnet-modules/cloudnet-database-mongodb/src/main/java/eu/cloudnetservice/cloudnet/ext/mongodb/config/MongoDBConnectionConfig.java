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

package eu.cloudnetservice.cloudnet.ext.mongodb.config;

import com.google.common.base.Strings;
import java.io.UnsupportedEncodingException;
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

  public MongoDBConnectionConfig(String databaseServiceName, String host, int port, String authSource,
    String username, String password, String database, String overridingConnectionUri) {
    this.databaseServiceName = databaseServiceName;
    this.host = host;
    this.port = port;
    this.authSource = authSource;
    this.username = username;
    this.password = password;
    this.database = database;
    this.overridingConnectionUri = overridingConnectionUri;
  }

  public String getDatabaseServiceName() {
    return this.databaseServiceName;
  }

  public String getHost() {
    return this.host;
  }

  public int getPort() {
    return this.port;
  }

  public String getAuthSource() {
    return this.authSource;
  }

  public String getUsername() {
    return this.username;
  }

  public String getPassword() {
    return this.password;
  }

  public String getDatabase() {
    return this.database;
  }

  public String getOverridingConnectionUri() {
    return this.overridingConnectionUri;
  }

  public String buildConnectionUri() throws UnsupportedEncodingException {
    if (!Strings.isNullOrEmpty(this.overridingConnectionUri)) {
      return this.overridingConnectionUri;
    }

    String authParams = Strings.isNullOrEmpty(this.username) && Strings.isNullOrEmpty(this.password)
      ? ""
      : String.format("%s:%s@", this.encodeUrl(this.username), this.encodeUrl(this.password));
    String authSource = Strings.isNullOrEmpty(this.authSource) ? "" : String.format("/?authSource=%s", this.authSource);
    // auth @ host:port / auth source
    return String.format("mongodb://%s%s:%d%s", authParams, this.host, this.port, authSource);
  }

  private String encodeUrl(String input) throws UnsupportedEncodingException {
    return URLEncoder.encode(input, StandardCharsets.UTF_8.name());
  }
}
