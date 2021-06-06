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

package de.dytanic.cloudnet.event;

import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

public class ServiceListCommandEvent extends Event {

  private final Collection<ServiceInfoSnapshot> targetServiceInfoSnapshots;
  private final Collection<Function<ServiceInfoSnapshot, String>> additionalParameters;
  private final Collection<String> additionalSummary;

  public ServiceListCommandEvent(Collection<ServiceInfoSnapshot> targetServiceInfoSnapshots,
    Collection<Function<ServiceInfoSnapshot, String>> additionalParameters, Collection<String> additionalSummary) {
    this.targetServiceInfoSnapshots = targetServiceInfoSnapshots;
    this.additionalParameters = additionalParameters;
    this.additionalSummary = additionalSummary;
  }

  public ServiceListCommandEvent(Collection<ServiceInfoSnapshot> targetServiceInfoSnapshots) {
    this(targetServiceInfoSnapshots, new ArrayList<>(), new ArrayList<>());
  }

  public Collection<ServiceInfoSnapshot> getTargetServiceInfoSnapshots() {
    return this.targetServiceInfoSnapshots;
  }

  public Collection<Function<ServiceInfoSnapshot, String>> getAdditionalParameters() {
    return this.additionalParameters;
  }

  public Collection<String> getAdditionalSummary() {
    return this.additionalSummary;
  }

  public void addParameter(Function<ServiceInfoSnapshot, String> function) {
    this.additionalParameters.add(function);
  }

  public void addSummaryParameter(String parameter) {
    this.additionalSummary.add(parameter);
  }

}
