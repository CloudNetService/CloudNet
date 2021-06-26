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
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.concurrent.function.ThrowableBiFunction;
import de.dytanic.cloudnet.common.concurrent.function.ThrowableFunction;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.api.DriverAPICategory;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.FileInfo;
import de.dytanic.cloudnet.driver.template.SpecificTemplateStorage;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import de.dytanic.cloudnet.driver.template.TemplateStorageResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.stream.Collectors;

public class DriverTemplateStorageListener extends CategorizedDriverAPIListener {

  public DriverTemplateStorageListener() {
    super(DriverAPICategory.TEMPLATE_STORAGE);

    super.registerHandler(DriverAPIRequestType.GET_TEMPLATES, (channel, packet, input) -> {
      Collection<ServiceTemplate> templates = this.read(input).getTemplates();
      return ProtocolBuffer.create().writeObjectCollection(templates);
    });

    super.registerHandler(DriverAPIRequestType.CLOSE_STORAGE, this.throwableResponseHandler(input -> {
      this.read(input).close();
      return TemplateStorageResponse.SUCCESS;
    }));

    super.registerHandler(DriverAPIRequestType.LIST_FILES, this.throwableHandler((channel, input) -> {
      FileInfo[] files = this.readSpecific(input).listFiles(input.readString(), input.readBoolean());
      ProtocolBuffer buffer = ProtocolBuffer.create().writeEnumConstant(TemplateStorageResponse.SUCCESS);
      buffer.writeBoolean(files != null);
      if (files != null) {
        buffer.writeObjectArray(files);
      }
      return buffer;
    }));
    super.registerHandler(DriverAPIRequestType.GET_FILE_INFO, this.throwableHandler((channel, input) -> {
      FileInfo file = this.readSpecific(input).getFileInfo(input.readString());
      return ProtocolBuffer.create().writeEnumConstant(TemplateStorageResponse.SUCCESS).writeOptionalObject(file);
    }));

    super.registerHandler(DriverAPIRequestType.CREATE_DIRECTORY, this.throwableResponseHandler(
      input -> TemplateStorageResponse.of(this.readSpecific(input).createDirectory(input.readString()))));
    super.registerHandler(DriverAPIRequestType.CREATE_FILE, this.throwableResponseHandler(
      input -> TemplateStorageResponse.of(this.readSpecific(input).createFile(input.readString()))));
    super.registerHandler(DriverAPIRequestType.CONTAINS_FILE, this.throwableResponseHandler(
      input -> TemplateStorageResponse.of(this.readSpecific(input).hasFile(input.readString()))));
    super.registerHandler(DriverAPIRequestType.DELETE_FILE, this.throwableResponseHandler(
      input -> TemplateStorageResponse.of(this.readSpecific(input).deleteFile(input.readString()))));

    super.registerHandler(DriverAPIRequestType.CONTAINS_TEMPLATE,
      this.throwableResponseHandler(input -> TemplateStorageResponse.of(this.readSpecific(input).exists())));
    super.registerHandler(DriverAPIRequestType.CREATE_TEMPLATE,
      this.throwableResponseHandler(input -> TemplateStorageResponse.of(this.readSpecific(input).create())));
    super.registerHandler(DriverAPIRequestType.DELETE_TEMPLATE,
      this.throwableResponseHandler(input -> TemplateStorageResponse.of(this.readSpecific(input).delete())));

    super.registerHandler(DriverAPIRequestType.LOAD_TEMPLATE_STREAM,
      this.chunkedHandler(input -> this.readSpecific(input).zipTemplate()));
    super.registerHandler(DriverAPIRequestType.GET_FILE_CONTENT,
      this.chunkedHandler(input -> this.readSpecific(input).newInputStream(input.readString())));

    super.registerHandler(
      DriverAPIRequestType.GET_TEMPLATE_STORAGES,
      (channel, packet, input) -> ProtocolBuffer.create().writeStringCollection(
        CloudNet.getInstance().getAvailableTemplateStorages().stream()
          .map(INameable::getName)
          .collect(Collectors.toList())
      )
    );

  }

  private DriverAPIHandler chunkedHandler(ThrowableFunction<ProtocolBuffer, InputStream, IOException> function) {
    return (channel, packet, input) -> {
      TemplateStorageResponse response = TemplateStorageResponse.SUCCESS;
      InputStream inputStream = FileUtils.EMPTY_STREAM;

      try {
        inputStream = function.apply(input);
        if (inputStream == null) {
          response = TemplateStorageResponse.FAILED;
          inputStream = FileUtils.EMPTY_STREAM;
        }

      } catch (Response thrownResponse) {
        response = thrownResponse.getResponse();
      } catch (IOException exception) {
        response = TemplateStorageResponse.EXCEPTION;
        inputStream = new ByteArrayInputStream(ProtocolBuffer.create().writeThrowable(exception).toArray());
      }

      try {
        channel.sendChunkedPacketsResponse(packet.getUniqueId(), JsonDocument.newDocument("response", response),
          inputStream);
      } catch (IOException exception) {
        exception.printStackTrace();
      }

      return null;
    };
  }

  private DriverAPIHandler throwableHandler(
    ThrowableBiFunction<INetworkChannel, ProtocolBuffer, ProtocolBuffer, IOException> function) {
    return (channel, packet, input) -> {
      try {
        return function.apply(channel, input);
      } catch (Response response) {
        return ProtocolBuffer.create().writeEnumConstant(response.getResponse());
      } catch (IOException exception) {
        return ProtocolBuffer.create().writeEnumConstant(TemplateStorageResponse.EXCEPTION).writeThrowable(exception);
      }
    };
  }

  private DriverAPIHandler throwableResponseHandler(
    ThrowableFunction<ProtocolBuffer, TemplateStorageResponse, IOException> function) {
    return (channel, packet, input) -> {
      try {
        return ProtocolBuffer.create().writeEnumConstant(function.apply(input));
      } catch (Response response) {
        return ProtocolBuffer.create().writeEnumConstant(response.getResponse());
      } catch (IOException exception) {
        return ProtocolBuffer.create().writeEnumConstant(TemplateStorageResponse.EXCEPTION).writeThrowable(exception);
      }
    };
  }

  private TemplateStorage read(ProtocolBuffer buffer) {
    TemplateStorage storage = CloudNetDriver.getInstance().getTemplateStorage(buffer.readString());
    if (storage == null) {
      throw new Response(TemplateStorageResponse.TEMPLATE_STORAGE_NOT_FOUND);
    }
    return storage;
  }

  private SpecificTemplateStorage readSpecific(ProtocolBuffer buffer) {
    SpecificTemplateStorage storage = buffer.readObject(ServiceTemplate.class).nullableStorage();
    if (storage == null) {
      throw new Response(TemplateStorageResponse.TEMPLATE_STORAGE_NOT_FOUND);
    }
    return storage;
  }

  private static class Response extends RuntimeException {

    private final TemplateStorageResponse response;

    public Response(TemplateStorageResponse response) {
      this.response = response;
    }

    public TemplateStorageResponse getResponse() {
      return this.response;
    }
  }


}
