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
import eu.cloudnetservice.utils.base.io.FileUtil;
import eu.cloudnetservice.utils.base.resource.ResourceResolver;
import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;
import eu.cloudnetservice.wrapper.event.ApplicationPostStartEvent;
import eu.cloudnetservice.wrapper.event.ApplicationPreStartEvent;
import eu.cloudnetservice.wrapper.holder.ServiceInfoHolder;
import eu.cloudnetservice.wrapper.impl.network.chunk.TemplateStorageCallbackListener;
import eu.cloudnetservice.wrapper.impl.network.listener.AuthorizationPacketListener;
import eu.cloudnetservice.wrapper.impl.network.listener.ChannelMessagePacketListener;
import eu.cloudnetservice.wrapper.impl.network.listener.message.GroupChannelMessageListener;
import eu.cloudnetservice.wrapper.impl.network.listener.message.ServiceChannelMessageListener;
import eu.cloudnetservice.wrapper.impl.network.listener.message.TaskChannelMessageListener;
import eu.cloudnetservice.wrapper.transform.ClassTransformer;
import eu.cloudnetservice.wrapper.transform.ClassTransformerRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.jar.JarFile;
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

  public static void loadTranslations(@NonNull @Service I18n i18n) {
    var resourcePath = Path.of(ResourceResolver.resolveCodeSourceOfClass(Wrapper.class));
    FileUtil.openZipFile(resourcePath, fs -> {
      // get the language directory
      var langDir = fs.getPath("lang/");
      if (Files.notExists(langDir) || !Files.isDirectory(langDir)) {
        throw new IllegalStateException("lang/ must be an existing directory inside the jar to load");
      }
      // visit each file and register it as a language source
      FileUtil.walkFileTree(langDir, ($, sub) -> {
        // try to load and register the language file
        try (var stream = Files.newInputStream(sub)) {
          var lang = sub.getFileName().toString().replace(".properties", "");
          i18n.registerProvider(Locale.forLanguageTag(lang), PropertiesTranslationProvider.fromProperties(stream));
        } catch (IOException exception) {
          LOGGER.error("Unable to open language file for reading @ {}", sub, exception);
        }
      }, false, "*.properties");
    });
  }

  @Inject
  @Order(100)
  private void initI18n(@NonNull @Service I18n i18n) {
    loadTranslations(i18n);
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
      // get the shutdown handler and execute the shutdown process
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
  @Order(Integer.MAX_VALUE)
  private void startApplication(
    @NonNull EventManager eventManager,
    @NonNull @Named("consoleArgs") List<String> consoleArgs
  ) throws Exception {
    // get all the information provided through the command line
    var mainClass = consoleArgs.remove(0);
    var premainClass = consoleArgs.remove(0);
    var appFile = Path.of(consoleArgs.remove(0));
    var preLoadAppJar = Boolean.parseBoolean(consoleArgs.remove(0));

    // preload all jars in the application if requested
    var loader = ClassLoader.getSystemClassLoader();
    if (preLoadAppJar) {
      // create a custom class loader for loading the application resources
      loader = new URLClassLoader(
        new URL[]{appFile.toUri().toURL()},
        ClassLoader.getSystemClassLoader());
      // force our loader to load all classes in the jar
      Premain.preloadClasses(appFile, loader);
    }

    // append the application file to the system class path
    Premain.instrumentation.appendToSystemClassLoaderSearch(new JarFile(appFile.toFile()));

    // invoke the premain method if given
    Premain.invokePremain(premainClass, loader);

    // get the main method
    var main = Class.forName(mainClass, true, loader);
    var method = main.getMethod("main", String[].class);

    // inform the user about the pre-start
    Collection<String> arguments = new LinkedList<>(consoleArgs);
    eventManager.callEvent(new ApplicationPreStartEvent(main, arguments, loader));

    // initially the class path is not allowed to contain the path to the app file
    // as the wrapper need to load it in a custom class loader after the system
    // class loader is set up.
    // however, some people for some reason rely on the app file being on the class
    // path (for example to search resources). therefore we re-append the app file
    // after jvm init so that the app file does not show up in the system class path
    // but will show up if someone access "java.class.path" (or some other source
    // in java, everything uses this property, e.g. RuntimeMXBean)
    System.setProperty("java.class.path", this.appendAppFileToClassPath(appFile));

    // start the application
    var applicationThread = new Thread(() -> {
      try {
        LOGGER.info("Starting application using class {} (pre-main: {})", mainClass, premainClass);
        // start the application
        method.invoke(null, new Object[]{arguments.toArray(new String[0])});
      } catch (Exception exception) {
        LOGGER.error("Exception while starting application", exception);
      }
    }, "Application-Thread");
    applicationThread.setContextClassLoader(loader);
    applicationThread.start();

    // inform the user about the post-start
    eventManager.callEvent(new ApplicationPostStartEvent(main, applicationThread, loader));
  }

  private @NonNull String appendAppFileToClassPath(@NonNull Path appFile) {
    var currentClassPath = System.getProperty("java.class.path");
    if (currentClassPath == null || currentClassPath.isBlank()) {
      return appFile.getFileName().toString();
    } else {
      return currentClassPath + File.pathSeparator + appFile.getFileName();
    }
  }
}
