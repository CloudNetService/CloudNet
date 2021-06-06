package de.dytanic.cloudnet.wrapper.conf;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

/**
 * The configuration mirror of the .wrapper/wrapper.json file in the working folder from the service process. It
 * includes all important data for the Wrapper to connect with the local node.
 * <p>
 * The configuration style is defined in the specific implementation of this interface
 */
public interface IWrapperConfiguration {

  /**
   * The key, for the connection, to authenticate with the provided node and the network
   *
   * @return the string which includes the key
   */
  String getConnectionKey();

  /**
   * The address of a listener by the node
   *
   * @return the target local listener address of the node
   */
  HostAndPort getTargetListener();

  /**
   * The first own serviceConfiguration with all important information about the service
   *
   * @return the instance of the ServiceConfiguration class
   */
  ServiceConfiguration getServiceConfiguration();

  /**
   * The first serviceInfoSnapshot which has the important data for all next serviceInfoSnapshots, which the wrapper has
   * to create
   *
   * @return the serviceInfoSnapshot sample
   */
  ServiceInfoSnapshot getServiceInfoSnapshot();

  /**
   * The ssl configuration, which needs for an optional ssl client connection.
   *
   * @return a document instance, which includes all important ssl settings, for a client ssl connection
   */
  JsonDocument getSslConfig();
}
