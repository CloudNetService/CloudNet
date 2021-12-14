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

package de.dytanic.cloudnet.command.sub;

import de.dytanic.cloudnet.driver.service.ServiceConfigurationBase;
import java.util.Arrays;
import java.util.Collection;

public final class CommandServiceConfiguration {

  public static void applyServiceConfigurationDisplay(Collection<String> messages,
    ServiceConfigurationBase configurationBase) {
    messages.add(" ");

    messages.add("Includes:");

    for (var inclusion : configurationBase.getIncludes()) {
      messages.add("- " + inclusion.getUrl() + " => " + inclusion.getDestination());
    }

    messages.add(" ");
    messages.add("Templates:");

    for (var template : configurationBase.getTemplates()) {
      messages.add("- " + template);
    }

    messages.add(" ");
    messages.add("Deployments:");

    for (var deployment : configurationBase.getDeployments()) {
      messages.add("- ");
      messages.add(
        "Template:  " + deployment.getTemplate());
      messages.add("Excludes: " + deployment.getExcludes());
    }

    messages.add(" ");
    messages.add("JVM Options:");

    for (var jvmOption : configurationBase.getJvmOptions()) {
      messages.add("- " + jvmOption);
    }

    messages.add(" ");
    messages.add("Process Parameters:");

    for (var processParameters : configurationBase.getProcessParameters()) {
      messages.add("- " + processParameters);
    }

    messages.add(" ");

    messages.add("Properties: ");

    messages.addAll(Arrays.asList(configurationBase.getProperties().toPrettyJson().split("\n")));
    messages.add(" ");
  }
}
