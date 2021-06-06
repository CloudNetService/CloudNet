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

package de.dytanic.cloudnet.service.defaults;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.encrypt.EncryptTo;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import de.dytanic.cloudnet.event.service.CloudServiceDeploymentEvent;
import de.dytanic.cloudnet.event.service.CloudServicePreLoadInclusionEvent;
import de.dytanic.cloudnet.event.service.CloudServiceTemplateLoadEvent;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import de.dytanic.cloudnet.service.handler.CloudServiceHandler;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public abstract class DefaultTemplateCloudService extends DefaultCloudService {

  private final List<ServiceRemoteInclusion> includes = new ArrayList<>();
  private final List<ServiceTemplate> templates = new ArrayList<>();
  private final List<ServiceDeployment> deployments = new CopyOnWriteArrayList<>();
  private final Queue<ServiceRemoteInclusion> waitingIncludes = new ConcurrentLinkedQueue<>();
  private final Queue<ServiceTemplate> waitingTemplates = new ConcurrentLinkedQueue<>();

  public DefaultTemplateCloudService(String runtime, ICloudServiceManager cloudServiceManager,
    ServiceConfiguration serviceConfiguration, @NotNull CloudServiceHandler handler) {
    super(runtime, cloudServiceManager, serviceConfiguration, handler);
  }

  @Override
  @ApiStatus.Internal
  public void init() {
    this.waitingIncludes.addAll(Arrays.asList(this.getServiceConfiguration().getIncludes()));
    this.waitingTemplates.addAll(Arrays.asList(this.getServiceConfiguration().getTemplates()));
    this.deployments.addAll(Arrays.asList(this.getServiceConfiguration().getDeployments()));
    super.init();
  }

  @Override
  public void includeInclusions() {
    byte[] buffer = new byte[16384];

    while (!this.waitingIncludes.isEmpty()) {
      ServiceRemoteInclusion inclusion = this.waitingIncludes.poll();

      if (inclusion != null && inclusion.getDestination() != null && inclusion.getUrl() != null) {
        try {
          CloudNet.getInstance().getLogger()
            .extended(LanguageManager.getMessage("cloud-service-include-inclusion-message")
              .replace("%task%", this.getServiceId().getTaskName())
              .replace("%id%", this.getServiceId().getUniqueId().toString())
              .replace("%serviceId%", String.valueOf(this.getServiceId().getTaskServiceId()))
              .replace("%url%", inclusion.getUrl())
              .replace("%destination%", inclusion.getDestination())
            );

          Path cacheDestination = Paths.get(
            System.getProperty("cloudnet.tempDir.includes", "temp/includes"),
            Base64.getEncoder().encodeToString(EncryptTo.encryptToSHA256(inclusion.getUrl()))
          );
          FileUtils.createDirectoryReported(cacheDestination.getParent());

          if (Files.notExists(cacheDestination)) {
            if (!this.includeInclusions0(inclusion, cacheDestination, buffer)) {
              continue;
            }
          }

          try (InputStream inputStream = Files.newInputStream(cacheDestination)) {
            Path target = this.getDirectoryPath().resolve(inclusion.getDestination());
            FileUtils.createDirectoryReported(target.getParent());

            try (OutputStream outputStream = Files.newOutputStream(target)) {
              FileUtils.copy(inputStream, outputStream, buffer);
            }
          }

          this.includes.add(inclusion);
        } catch (Exception exception) {
          exception.printStackTrace();
        }
      }
    }
  }

  private boolean includeInclusions0(ServiceRemoteInclusion inclusion, Path destination, byte[] buffer)
    throws Exception {
    URLConnection connection = new URL(inclusion.getUrl()).openConnection();

    if (CloudNetDriver.getInstance().getEventManager()
      .callEvent(new CloudServicePreLoadInclusionEvent(this, inclusion, connection)).isCancelled()) {
      return false;
    }

    if (inclusion.getProperties() != null) {
      if (inclusion.getProperties().contains("httpHeaders")) {
        JsonDocument document = inclusion.getProperties().getDocument("httpHeaders");

        for (String key : document) {
          connection.setRequestProperty(key, document.get(key).toString());
        }
      }
    }

    connection.setDoOutput(false);
    connection.setUseCaches(false);
    connection.setRequestProperty("User-Agent",
      "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");

    connection.connect();

    try (InputStream inputStream = connection.getInputStream(); OutputStream outputStream = Files
      .newOutputStream(destination)) {
      FileUtils.copy(inputStream, outputStream, buffer);
    }

    return true;
  }

  @Override
  public void includeTemplates() {
    while (!this.waitingTemplates.isEmpty()) {
      ServiceTemplate template = this.waitingTemplates.poll();

      if (template != null && template.getName() != null && template.getPrefix() != null
        && template.getStorage() != null) {
        TemplateStorage storage = this.getStorage(template.getStorage());

        if (!storage.has(template)) {
          continue;
        }

        CloudServiceTemplateLoadEvent cloudServiceTemplateLoadEvent = new CloudServiceTemplateLoadEvent(this, storage,
          template);
        CloudNetDriver.getInstance().getEventManager().callEvent(cloudServiceTemplateLoadEvent);

        if (cloudServiceTemplateLoadEvent.isCancelled()) {
          continue;
        }

        try {
          if (!this.getServiceConfiguration().isStaticService() || template.shouldAlwaysCopyToStaticServices()
            || this.firstStartupOnStaticService) {
            CloudNet.getInstance().getLogger()
              .extended(LanguageManager.getMessage("cloud-service-include-template-message")
                .replace("%task%", this.getServiceId().getTaskName())
                .replace("%id%", this.getServiceId().getUniqueId().toString())
                .replace("%serviceId%", String.valueOf(this.getServiceId().getTaskServiceId()))
                .replace("%template%", template.getTemplatePath())
                .replace("%storage%", template.getStorage())
              );

            storage.copy(template, this.getDirectoryPath());
          }

          this.templates.add(template);

        } catch (Exception exception) {
          exception.printStackTrace();
        }
      }
    }

  }

  @Override
  public void deployResources(boolean removeDeployments) {
    for (ServiceDeployment deployment : this.deployments) {
      if (deployment != null) {
        if (deployment.getTemplate() != null && deployment.getTemplate().getStorage() != null
          && deployment.getTemplate().getPrefix() != null &&
          deployment.getTemplate().getName() != null) {
          TemplateStorage storage = this.getStorage(deployment.getTemplate().getStorage());

          CloudServiceDeploymentEvent cloudServiceDeploymentEvent = new CloudServiceDeploymentEvent(this, storage,
            deployment);
          CloudNetDriver.getInstance().getEventManager().callEvent(cloudServiceDeploymentEvent);

          if (cloudServiceDeploymentEvent.isCancelled()) {
            continue;
          }

          System.out.println(LanguageManager.getMessage("cloud-service-deploy-message")
            .replace("%task%", this.getServiceId().getTaskName())
            .replace("%id%", this.getServiceId().getUniqueId().toString())
            .replace("%serviceId%", String.valueOf(this.getServiceId().getTaskServiceId()))
            .replace("%template%", deployment.getTemplate().getTemplatePath())
            .replace("%storage%", deployment.getTemplate().getStorage())
          );

          storage.deploy(this.getDirectoryPath(), deployment.getTemplate(), path -> {
              Path pathFileName = path.getFileName();
              if (pathFileName != null && deployment.getExcludes() != null) {
                String fileName = Files.isDirectory(path) ? pathFileName.toString() + '/' : pathFileName.toString();
                return !deployment.getExcludes().contains(fileName)
                  && !fileName.equals("wrapper.jar") && !fileName.equals(".wrapper");
              } else {
                return true;
              }
            }
          );

          if (removeDeployments) {
            this.deployments.remove(deployment);
          }
        }
      }
    }
  }

  @Override
  public void offerTemplate(@NotNull ServiceTemplate template) {
    this.waitingTemplates.offer(template);
    this.updateServiceInfoSnapshot(this.createServiceInfoSnapshot(this.lifeCycle));
  }

  @Override
  public void offerInclusion(@NotNull ServiceRemoteInclusion inclusion) {
    this.waitingIncludes.offer(inclusion);
    this.updateServiceInfoSnapshot(this.createServiceInfoSnapshot(this.lifeCycle));
  }

  @Override
  public void addDeployment(@NotNull ServiceDeployment deployment) {
    this.deployments.add(deployment);
    this.updateServiceInfoSnapshot(this.createServiceInfoSnapshot(this.lifeCycle));
  }

  @Override
  protected ServiceInfoSnapshot createServiceInfoSnapshot(ServiceLifeCycle lifeCycle) {
    this.getServiceConfiguration().setDeployments(this.deployments.toArray(new ServiceDeployment[0]));
    this.getServiceConfiguration().setTemplates(this.templates.toArray(new ServiceTemplate[0]));
    this.getServiceConfiguration().setIncludes(this.includes.toArray(new ServiceRemoteInclusion[0]));
    return super.createServiceInfoSnapshot(lifeCycle);
  }

  private TemplateStorage getStorage(String storageName) {
    TemplateStorage storage = CloudNetDriver.getInstance().getTemplateStorage(storageName);
    return storage != null ? storage : CloudNetDriver.getInstance().getLocalTemplateStorage();
  }

  @Override
  public List<ServiceRemoteInclusion> getIncludes() {
    return Collections.unmodifiableList(this.includes);
  }

  @Override
  public List<ServiceTemplate> getTemplates() {
    return Collections.unmodifiableList(this.templates);
  }

  @Override
  public List<ServiceDeployment> getDeployments() {
    return this.deployments;
  }

  @Override
  public Queue<ServiceRemoteInclusion> getWaitingIncludes() {
    return this.waitingIncludes;
  }

  @Override
  public Queue<ServiceTemplate> getWaitingTemplates() {
    return this.waitingTemplates;
  }
}
