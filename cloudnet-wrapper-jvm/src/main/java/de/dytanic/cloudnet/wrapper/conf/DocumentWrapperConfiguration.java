package de.dytanic.cloudnet.wrapper.conf;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.Getter;

/**
 * The default json based wrapper configuration for the service. It loads only
 * the configuration with the constructor all properties once.
 *
 * @see IWrapperConfiguration
 */
@Getter
public final class DocumentWrapperConfiguration implements
  IWrapperConfiguration {

  private static final Path WRAPPER_CONFIG = Paths.get(System
    .getProperty("cloudnet.wrapper.config.path", ".wrapper/wrapper.json"));

  private static final Type
    SERVICE_CFG_TYPE = new TypeToken<ServiceConfiguration>() {
  }.getType(),
    SERVICE_INFO_TYPE = new TypeToken<ServiceInfoSnapshot>() {
    }.getType();

  private String connectionKey;

  private HostAndPort targetListener;

  private ServiceInfoSnapshot serviceInfoSnapshot;

  private ServiceConfiguration serviceConfiguration;

  private JsonDocument sslConfig;

  public DocumentWrapperConfiguration() {
    this.load();
  }

  private void load() {
    JsonDocument document = JsonDocument.newDocument(WRAPPER_CONFIG);

    this.connectionKey = document.getString("connectionKey");
    this.targetListener = document.get("listener", HostAndPort.class);
    this.serviceConfiguration = document
      .get("serviceConfiguration", SERVICE_CFG_TYPE);
    this.serviceInfoSnapshot = document
      .get("serviceInfoSnapshot", SERVICE_INFO_TYPE);
    this.sslConfig = document.getDocument("sslConfig");
  }
}