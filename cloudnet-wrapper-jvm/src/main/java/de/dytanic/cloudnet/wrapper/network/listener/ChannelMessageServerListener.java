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

package de.dytanic.cloudnet.wrapper.network.listener;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionAddGroupEvent;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionAddUserEvent;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionDeleteGroupEvent;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionDeleteUserEvent;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionSetGroupsEvent;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionSetUsersEvent;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionUpdateGroupEvent;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionUpdateUserEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceConnectNetworkEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceDisconnectNetworkEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceRegisterEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStartEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStopEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceUnregisterEvent;
import de.dytanic.cloudnet.driver.event.events.service.task.ServiceTaskAddEvent;
import de.dytanic.cloudnet.driver.event.events.service.task.ServiceTaskRemoveEvent;
import de.dytanic.cloudnet.driver.network.NetworkUpdateType;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.def.NetworkConstants;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerServiceInfoPublisher.PublisherType;
import de.dytanic.cloudnet.driver.network.rpc.defaults.object.DefaultObjectMapper;
import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.logging.Level;

/**
 * @author Aldin S. (0utplay@cloudnetservice.eu)
 */
public class ChannelMessageServerListener {

  public static final Type SERVICE_TASK_COLLECTION = TypeToken.getParameterized(Collection.class, ServiceTask.class)
    .getType();
  public static final Type PERMISSION_USER_COLLECTION = TypeToken.getParameterized(Collection.class,
    PermissionUser.class).getType();
  public static final Type PERMISSION_GROUP_COLLECTION = TypeToken.getParameterized(Collection.class,
    PermissionGroup.class).getType();

  public ChannelMessageServerListener() {
    CloudNetDriver.getInstance().getEventManager().registerListener(this);
  }

  @EventListener
  public void handleChannelMessage(ChannelMessageReceiveEvent event) {
    if (!event.getChannel().equals(NetworkConstants.INTERNAL_MSG_CHANNEL)) {
      return;
    }

    ChannelMessage channelMessage = event.getChannelMessage();
    if (channelMessage.getMessage() == null) {
      return;
    }

    DataBuf content = event.getContent();
    switch (channelMessage.getMessage()) {
      case "set_service_task": {
        Collection<ServiceTask> serviceTasks = DefaultObjectMapper.DEFAULT_MAPPER.readObject(content,
          SERVICE_TASK_COLLECTION);

        if (serviceTasks != null) {
          NetworkUpdateType updateType = content.readObject(NetworkUpdateType.class);
          if (updateType == null) {
            return;
          }

          switch (updateType) {
            case ADD:
              for (ServiceTask serviceTask : serviceTasks) {
                CloudNetDriver.getInstance().getEventManager().callEvent(new ServiceTaskAddEvent(serviceTask));
              }
              break;
            case REMOVE:
              for (ServiceTask serviceTask : serviceTasks) {
                CloudNetDriver.getInstance().getEventManager().callEvent(new ServiceTaskRemoveEvent(serviceTask));
              }
              break;
            default:
              break;
          }
        }
      }
      break;
      case "set_global_log_level": {
        //TODO: remove in 3.6
        LogManager.getRootLogger().setLevel(Level.parse(content.readString()));
      }
      break;
      case "update_permissions": {
        /*
        UpdateType updateType = content.readObject(UpdateType.class);
        if (updateType == null) {
          return;
        }

        IPermissionManagement permissionManagement = CloudNetDriver.getInstance().getPermissionManagement();

        switch (updateType) {
          case ADD_USER:
            this.callEvent(new PermissionAddUserEvent(permissionManagement, content.readObject(PermissionUser.class)));
            break;
          case ADD_GROUP:
            this.callEvent(
              new PermissionAddGroupEvent(permissionManagement, content.readObject(PermissionGroup.class)));
            break;
          case SET_USERS:
            this.callEvent(
              new PermissionSetUsersEvent(permissionManagement,
                DefaultObjectMapper.DEFAULT_MAPPER.readObject(content, PERMISSION_USER_COLLECTION)));
            break;
          case SET_GROUPS:
            this.callEvent(new PermissionSetGroupsEvent(permissionManagement,
              DefaultObjectMapper.DEFAULT_MAPPER.readObject(content, PERMISSION_GROUP_COLLECTION)));
            break;
          case DELETE_USER:
            this.callEvent(
              new PermissionDeleteUserEvent(permissionManagement, content.readObject(PermissionUser.class)));
            break;
          case UPDATE_USER:
            this.callEvent(
              new PermissionUpdateUserEvent(permissionManagement, content.readObject(PermissionUser.class)));
            break;
          case DELETE_GROUP:
            this.callEvent(
              new PermissionDeleteGroupEvent(permissionManagement, content.readObject(PermissionGroup.class)));
            break;
          case UPDATE_GROUP:
            this.callEvent(
              new PermissionUpdateGroupEvent(permissionManagement, content.readObject(PermissionGroup.class)));
            break;
          default:
            break;
        }*/
      }
      break;
      case "wrapper_force_update": {
        Wrapper.getInstance().publishServiceInfoUpdate();
      }
      break;
      case "service_info_publish": {
        ServiceInfoSnapshot serviceInfoSnapshot = content.readObject(ServiceInfoSnapshot.class);
        PublisherType publisherType = DefaultObjectMapper.DEFAULT_MAPPER.readObject(content, PublisherType.class);

        if (publisherType == null || serviceInfoSnapshot == null) {
          return;
        }

        switch (publisherType) {
          case UPDATE:
            this.callEvent(new CloudServiceInfoUpdateEvent(serviceInfoSnapshot));
            break;
          case REGISTER:
            this.callEvent(new CloudServiceRegisterEvent(serviceInfoSnapshot));
            break;
          case CONNECTED:
            this.callEvent(new CloudServiceConnectNetworkEvent(serviceInfoSnapshot));
            break;
          case UNREGISTER:
            this.callEvent(new CloudServiceUnregisterEvent(serviceInfoSnapshot));
            break;
          case DISCONNECTED:
            this.callEvent(new CloudServiceDisconnectNetworkEvent(serviceInfoSnapshot));
            break;
          case STARTED:
            this.callEvent(new CloudServiceStartEvent(serviceInfoSnapshot));
            break;
          case STOPPED:
            this.callEvent(new CloudServiceStopEvent(serviceInfoSnapshot));
            break;
          default:
            break;
        }
        break;
      }
      default:
        break;
    }
  }

  private void callEvent(Event event) {
    Wrapper.getInstance().getEventManager().callEvent(event);
  }

}
