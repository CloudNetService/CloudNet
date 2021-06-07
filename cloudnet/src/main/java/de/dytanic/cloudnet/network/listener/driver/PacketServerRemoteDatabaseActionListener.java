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

package de.dytanic.cloudnet.network.listener.driver;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.driver.api.RemoteDatabaseRequestType;
import de.dytanic.cloudnet.driver.database.Database;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import java.util.Collection;
import java.util.Map;

public class PacketServerRemoteDatabaseActionListener implements IPacketListener {

  @Override
  public void handle(INetworkChannel channel, IPacket packet) throws Exception {
    ProtocolBuffer buffer = packet.getBuffer();
    RemoteDatabaseRequestType requestType = buffer.readEnumConstant(RemoteDatabaseRequestType.class);

    AbstractDatabaseProvider databaseProvider = CloudNet.getInstance().getDatabaseProvider();

    if (!requestType.isDatabaseSpecific()) {
      switch (requestType) {
        case DELETE_DATABASE: {
          boolean success = databaseProvider.deleteDatabase(buffer.readString());
          channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeBoolean(success)));
        }
        break;

        case CONTAINS_DATABASE: {
          boolean contains = databaseProvider.containsDatabase(buffer.readString());
          channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeBoolean(contains)));
        }
        break;

        case GET_DATABASES: {
          Collection<String> databases = databaseProvider.getDatabaseNames();
          channel
            .sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeStringCollection(databases)));
        }
        break;

        default:
          break;
      }

      return;
    }

    Database database = databaseProvider.getDatabase(buffer.readString());
    switch (requestType) {
      case DATABASE_KEYS: {
        Collection<String> keys = database.keys();
        channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeStringCollection(keys)));
      }
      break;

      case DATABASE_CLEAR: {
        database.clear();
        channel.sendPacket(Packet.createResponseFor(packet));
      }
      break;

      case DATABASE_DELETE: {
        boolean success = database.delete(buffer.readString());
        channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeBoolean(success)));
      }
      break;

      case DATABASE_INSERT: {
        boolean success = database.insert(buffer.readString(), buffer.readJsonDocument());
        channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeBoolean(success)));
      }
      break;

      case DATABASE_UPDATE: {
        boolean success = database.update(buffer.readString(), buffer.readJsonDocument());
        channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeBoolean(success)));
      }
      break;

      case DATABASE_ENTRIES: {
        Map<String, JsonDocument> entries = database.entries();
        ProtocolBuffer response = ProtocolBuffer.create();
        response.writeVarInt(entries.size());
        entries.forEach((key, value) -> {
          response.writeString(key);
          response.writeJsonDocument(value);
        });
        channel.sendPacket(Packet.createResponseFor(packet, response));
      }
      break;

      case DATABASE_CONTAINS: {
        boolean contains = database.contains(buffer.readString());
        channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeBoolean(contains)));
      }
      break;

      case DATABASE_DOCUMENTS: {
        Collection<JsonDocument> documents = database.documents();
        ProtocolBuffer response = ProtocolBuffer.create();
        response.writeVarInt(documents.size());
        for (JsonDocument document : documents) {
          response.writeJsonDocument(document);
        }
        channel.sendPacket(Packet.createResponseFor(packet, response));
      }
      break;

      case DATABASE_GET_BY_KEY: {
        JsonDocument document = database.get(buffer.readString());
        channel
          .sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeOptionalJsonDocument(document)));
      }
      break;

      case DATABASE_GET_BY_FIELD: {
        Collection<JsonDocument> documents = database.get(buffer.readString(), buffer.readString());
        ProtocolBuffer response = ProtocolBuffer.create();
        response.writeVarInt(documents.size());
        for (JsonDocument document : documents) {
          response.writeJsonDocument(document);
        }
        channel.sendPacket(Packet.createResponseFor(packet, response));
      }
      break;

      case DATABASE_GET_BY_FILTERS: {
        Collection<JsonDocument> documents = database.get(buffer.readJsonDocument());
        ProtocolBuffer response = ProtocolBuffer.create();
        response.writeVarInt(documents.size());
        for (JsonDocument document : documents) {
          response.writeJsonDocument(document);
        }
        channel.sendPacket(Packet.createResponseFor(packet, response));
      }
      break;

      case DATABASE_COUNT_DOCUMENTS: {
        long count = database.getDocumentsCount();
        channel.sendPacket(Packet.createResponseFor(packet, ProtocolBuffer.create().writeLong(count)));
      }
      break;

      case DATABASE_CLOSE: {
        database.close();
        channel.sendPacket(Packet.createResponseFor(packet));
      }
      break;

      default:
        break;
    }
  }
}
