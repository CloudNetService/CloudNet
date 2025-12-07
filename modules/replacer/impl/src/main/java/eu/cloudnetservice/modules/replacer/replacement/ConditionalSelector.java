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

package eu.cloudnetservice.modules.replacer.replacement;

import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.replacer.model.condition.ConditionRule;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

public final class ConditionalSelector {

  public @Nullable ValueSelector selector(@Nullable List<ConditionRule> conditions, ServiceInfoSnapshot serviceInfo) {
    if (conditions == null || conditions.isEmpty()) {
      return null;
    }

    var fieldValues = this.fieldValues(serviceInfo);
    for (var condition : conditions) {
      var when = condition.when();
      if (when == null || when.field() == null) {
        continue;
      }

      var actualValue = fieldValues.get(when.field().toLowerCase());
      if (actualValue == null) {
        continue;
      }

      if (when.equals() != null && actualValue.equals(when.equals())) {
        var value = condition.value();
        return value == null ? null : _ -> value;
      }
      if (when.regex() != null && Pattern.compile(when.regex()).matcher(actualValue).matches()) {
        var value = condition.value();
        return value == null ? null : _ -> value;
      }
    }
    return null;
  }

  private Map<String, String> fieldValues(ServiceInfoSnapshot serviceInfo) {
    var serviceId = serviceInfo.serviceId();

    var values = new HashMap<String, String>();
    values.put("task", serviceId.taskName());
    values.put("service", serviceId.name());
    values.put("environment", serviceId.environmentName());
    values.put("nodeid", Objects.toString(serviceId.nodeUniqueId(), ""));
    values.put("host", this.host(serviceInfo.address()));
    values.put("port", Integer.toString(serviceInfo.configuration().port()));

    return values;
  }

  private String host(HostAndPort address) {
    return address != null ? address.host() : "";
  }
}
