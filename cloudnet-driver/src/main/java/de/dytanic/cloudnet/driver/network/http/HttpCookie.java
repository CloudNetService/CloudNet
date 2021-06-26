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

package de.dytanic.cloudnet.driver.network.http;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class HttpCookie {

  protected String name;
  protected String value;
  protected String domain;
  protected String path;

  protected long maxAge;

  public HttpCookie(String name, String value, String domain, String path, long maxAge) {
    this.name = name;
    this.value = value;
    this.domain = domain;
    this.path = path;
    this.maxAge = maxAge;
  }

  public HttpCookie() {
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getValue() {
    return this.value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getDomain() {
    return this.domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public String getPath() {
    return this.path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public long getMaxAge() {
    return this.maxAge;
  }

  public void setMaxAge(long maxAge) {
    this.maxAge = maxAge;
  }

}
