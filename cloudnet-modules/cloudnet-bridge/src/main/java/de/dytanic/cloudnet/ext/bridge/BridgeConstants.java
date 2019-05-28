package de.dytanic.cloudnet.ext.bridge;

public interface BridgeConstants {

  String
    BRIDGE_NETWORK_CHANNEL_MESSAGE_GET_BRIDGE_CONFIGURATION = "cloudnet_bridge_get_bridge_configuration",
    BRIDGE_NETWORK_CHANNEL_MESSAGE_GET_BRIDGE_CONFIGURATION_CHANNEL_NAME = "cloudnet_bridge_config",
    BRIDGE_NETWORK_CHANNEL_CLUSTER_MESSAGE_UPDATE_BRIDGE_CONFIGURATION_LISTENER = "update_bridge_configuration";

  String BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL = "cloudnet-bridge-channel";

  String BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_LOGIN_REQUEST = "proxy_player_login_request_event",
    BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_LOGIN_SUCCESS = "proxy_player_login_success_event",
    BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_SERVER_CONNECT_REQUEST = "proxy_player_server_connect_request",
    BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_SERVER_SWITCH = "proxy_player_server_switch_event",
    BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_PROXY_DISCONNECT = "proxy_player_disconnect_event",
    BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_SERVER_LOGIN_REQUEST = "server_player_login_request_event",
    BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_SERVER_LOGIN_SUCCESS = "server_player_login_success_event",
    BRIDGE_EVENT_CHANNEL_MESSAGE_NAME_SERVER_DISCONNECT = "server_player_disconnect_event";

  /*= ------------------------------------------------------------------------------- =*/

  String BRIDGE_CUSTOM_MESSAGING_CHANNEL_PLAYER_API_CHANNEL_NAME = "cloudnet-bridge-channel-player-api";

  String BRIDGE_CUSTOM_CALLABLE_CHANNEL_PLAYER_API_CHANNEL_NAME = "cloudnet-bridge-channel-player-api";
}