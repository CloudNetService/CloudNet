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

package eu.cloudnetservice.wrapper.impl;

import dev.derklaro.aerogel.Order;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.impl.module.DefaultModuleProviderHandler;
import eu.cloudnetservice.driver.impl.network.NetworkConstants;
import eu.cloudnetservice.driver.impl.network.chunk.ChunkedSessionRegistry;
import eu.cloudnetservice.driver.impl.network.chunk.network.ChunkedPacketListener;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.language.PropertiesTranslationProvider;
import eu.cloudnetservice.driver.module.ModuleProvider;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.chunk.event.EventChunkHandlerFactory;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.utils.base.io.FileUtil;
import eu.cloudnetservice.utils.base.resource.ResourceResolver;
import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;
import eu.cloudnetservice.wrapper.event.WrapperBootstrapCompleteEvent;
import eu.cloudnetservice.wrapper.holder.ServiceInfoHolder;
import eu.cloudnetservice.wrapper.impl.network.chunk.TemplateStorageCallbackListener;
import eu.cloudnetservice.wrapper.impl.network.listener.AuthorizationPacketListener;
import eu.cloudnetservice.wrapper.impl.network.listener.ChannelMessagePacketListener;
import eu.cloudnetservice.wrapper.impl.network.listener.message.GroupChannelMessageListener;
import eu.cloudnetservice.wrapper.impl.network.listener.message.ServiceChannelMessageListener;
import eu.cloudnetservice.wrapper.impl.network.listener.message.TaskChannelMessageListener;
import eu.cloudnetservice.wrapper.impl.transform.ClassTransformer;
import eu.cloudnetservice.wrapper.impl.transform.ClassTransformerRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is the main api point when trying to interact with CloudNet from a running service within the CloudNet
 * network.
 *
 * @since 4.0
 */
public final class Wrapper {

  private static final Logger LOGGER = LoggerFactory.getLogger(Wrapper.class);

  @Inject
  @Order(50)
  private void initServiceRegistry() {
    ServiceRegistry.registry().discoverServices(Wrapper.class);
  }

  @Inject
  @Order(100)
  private void initI18n(@NonNull @Service I18n i18n) {
    var resourcePath = Path.of(ResourceResolver.resolveCodeSourceOfClass(Wrapper.class));
    FileUtil.openZipFile(resourcePath, fs -> {
      var langDir = fs.getPath("lang/");
      if (Files.notExists(langDir) || !Files.isDirectory(langDir)) {
        throw new IllegalStateException("lang/ must be an existing directory inside the jar to load");
      }

      FileUtil.walkFileTree(langDir, ($, sub) -> {
        try (var stream = Files.newInputStream(sub)) {
          var lang = sub.getFileName().toString().replace(".properties", "");
          i18n.registerProvider(Locale.forLanguageTag(lang), PropertiesTranslationProvider.fromProperties(stream));
        } catch (IOException exception) {
          LOGGER.error("Unable to open language file for reading @ {}", sub, exception);
        }
      }, false, "*.properties");
    });
    i18n.selectLanguage(Locale.forLanguageTag(System.getProperty("cloudnet.wrapper.messages.language", "en_US")));
  }

  @Inject
  @Order(150)
  private void initProviderAndLoadModules(
    @NonNull ModuleProvider moduleProvider,
    @NonNull DefaultModuleProviderHandler moduleProviderHandler
  ) {
    // init the provider
    moduleProvider.moduleProviderHandler(moduleProviderHandler);
    moduleProvider.moduleDirectoryPath(Path.of(".wrapper", "modules"));

    // load the modules
    moduleProvider.loadAll().startAll();
  }

  @Inject
  @Order(200)
  private void connectToNode(
    @NonNull EventManager eventManager,
    @NonNull NetworkClient networkClient,
    @NonNull WrapperConfiguration configuration,
    @NonNull ServiceInfoHolder serviceInfoHolder,
    @NonNull ChunkedSessionRegistry chunkedSessionRegistry
  ) {
    // create a new condition and the auth listener
    var currentThread = Thread.currentThread();
    var listener = new AuthorizationPacketListener(currentThread);

    // register the listener to the packet registry and connect to the target listener
    networkClient.packetRegistry().addListener(NetworkConstants.INTERNAL_AUTHORIZATION_CHANNEL, listener);
    networkClient
      .connect(configuration.targetListener())
      .exceptionally(ex -> {
        // log and exit, we're not connected
        LOGGER.error("Unable to establish a connection to the target node listener", ex);
        System.exit(-1);
        // returns void
        return null;
      }).join();

    // wait for the authentication response
    LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(30));
    if (!listener.wasAuthSuccessful()) {
      throw new IllegalStateException("Unable to authorize wrapper with node");
    }

    // set a new current snapshot with the connected time
    serviceInfoHolder.setup();

    // remove the auth listener
    networkClient.packetRegistry().removeListeners(NetworkConstants.INTERNAL_AUTHORIZATION_CHANNEL);

    // add the runtime packet listeners
    networkClient.packetRegistry().addListener(
      NetworkConstants.CHUNKED_PACKET_COM_CHANNEL,
      new ChunkedPacketListener(chunkedSessionRegistry, new EventChunkHandlerFactory(eventManager)));
    networkClient.packetRegistry().addListener(
      NetworkConstants.CHANNEL_MESSAGING_CHANNEL,
      ChannelMessagePacketListener.class);
  }

  @Inject
  @Order(200)
  private void installShutdownHook(@NonNull Provider<ShutdownHandler> shutdownHandlerProvider) {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      var shutdownHandler = shutdownHandlerProvider.get();
      shutdownHandler.shutdown();
    }));
  }

  @Inject
  @Order(250)
  private void registerTransformer(@NonNull ClassTransformerRegistry transformerRegistry) {
    var serviceLoader = ServiceLoader.load(ClassTransformer.class);
    serviceLoader.stream()
      .map(provider -> {
        try {
          return provider.get();
        } catch (ServiceConfigurationError error) {
          var typeName = provider.type().getName();
          LOGGER.debug("Skipping registration of class transformer {} due to spi configuration error", typeName, error);
          return null;
        }
      })
      .filter(Objects::nonNull)
      .forEach(transformerRegistry::registerTransformer);
  }

  @Inject
  @Order(300)
  private void registerDefaultListeners(@NonNull EventManager eventManager) {
    eventManager.registerListener(TaskChannelMessageListener.class);
    eventManager.registerListener(GroupChannelMessageListener.class);
    eventManager.registerListener(ServiceChannelMessageListener.class);
    eventManager.registerListener(TemplateStorageCallbackListener.class);
  }

  @Inject
  @Order(350)
  private void provideClassPathToFabric() {
    var classPath = System.getProperty("java.class.path");
    System.setProperty("fabric.systemLibraries", classPath);
  }

  @Inject
  @Order(Integer.MAX_VALUE)
  private void wrapperBootstrapComplete(@NonNull EventManager eventManager) {
    eventManager.callEvent(new WrapperBootstrapCompleteEvent());
  }
}
