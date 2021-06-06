package eu.cloudnetservice.cloudnet.ext.labymod;

import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import java.util.Arrays;
import java.util.Collection;

public interface LabyModConstants {

  String LMC_CHANNEL_NAME = "labymod3:main";
  String GET_CONFIGURATION = "get_cloudnet_labymod_config";
  String GET_PLAYER_JOIN_SECRET = "get_player_by_join_secret";
  String GET_PLAYER_SPECTATE_SECRET = "get_player_by_spectate_secret";
  String CLOUDNET_CHANNEL_NAME = "cloudnet_labymod_module";

  Collection<ServiceEnvironmentType> SUPPORTED_ENVIRONMENTS = Arrays
    .asList(ServiceEnvironmentType.BUNGEECORD, ServiceEnvironmentType.VELOCITY);
}
