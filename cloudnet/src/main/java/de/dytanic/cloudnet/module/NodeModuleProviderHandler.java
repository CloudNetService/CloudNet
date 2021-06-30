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

package de.dytanic.cloudnet.module;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.module.DefaultModuleProviderHandler;
import de.dytanic.cloudnet.driver.module.IModuleProviderHandler;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;

public final class NodeModuleProviderHandler extends DefaultModuleProviderHandler implements IModuleProviderHandler {

  @Override
  public void handlePostModuleStop(IModuleWrapper moduleWrapper) {
    super.handlePostModuleStop(moduleWrapper);

    CloudNet.getInstance().unregisterPacketListenersByClassLoader(moduleWrapper.getClassLoader());
    CloudNet.getInstance().getHttpServer().removeHandler(moduleWrapper.getClassLoader());
    CloudNet.getInstance().getCommandMap().unregisterCommands(moduleWrapper.getClassLoader());
  }

}
