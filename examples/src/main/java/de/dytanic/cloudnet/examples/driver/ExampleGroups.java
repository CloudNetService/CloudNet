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

package de.dytanic.cloudnet.examples.driver;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;

public final class ExampleGroups {

  public void test() {
    this.addGroupConfiguration();
    this.updateGroupConfiguration();
    this.removeGroupConfiguration();
  }

  public void addGroupConfiguration() {
    CloudNetDriver.getInstance().getGroupConfigurationProvider().addGroupConfiguration(
      new GroupConfiguration("TestGroup")); //Creates a group without default includes, templates and deployments
  }

  public void updateGroupConfiguration() {
    if (CloudNetDriver.getInstance().getGroupConfigurationProvider().isGroupConfigurationPresent("TestGroup")) {
      CloudNetDriver.getInstance().getGroupConfigurationProvider().getGroupConfigurationAsync("TestGroup")
        .onComplete(result -> {
          // add a new ServiceTemplate to the group
          result.getTemplates().add(new ServiceTemplate(
            "Lobby",
            "default",
            "local"
          ));

          CloudNetDriver.getInstance().getGroupConfigurationProvider()
            .addGroupConfiguration(result); //add or update the group configuration
        }).fireExceptionOnFailure();
    }
  }

  public void removeGroupConfiguration() {
    CloudNetDriver.getInstance().getGroupConfigurationProvider()
      .removeGroupConfiguration("TestGroup"); //remove the group configuration in network
  }

}
